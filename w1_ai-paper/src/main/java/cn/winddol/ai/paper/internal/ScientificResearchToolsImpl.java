package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IScientificResearchTools;
import cn.winddol.ai.paper.domain.OutlineNode;
import cn.winddol.ai.paper.domain.retrieval.PaperEvidence;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalQuery;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalResult;
import cn.winddol.ai.paper.internal.retrieval.AgentCommonToolsImpl;
import cn.winddol.ai.paper.internal.retrieval.AgentReaderServiceImpl;
import cn.winddol.ai.paper.internal.retrieval.HybridRetrieverServiceImpl;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import cn.winddol.ai.shared.model.tool.ToolResult;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ScientificResearchToolsImpl implements IScientificResearchTools {

    private final HybridRetrieverServiceImpl retrieverService;
    private final AgentReaderServiceImpl readerService;
    private final AgentCommonToolsImpl agentCommonTools;
    private final int smartReadLimit;
    private final double smartReadSingleSectionRatio;

    public ScientificResearchToolsImpl(HybridRetrieverServiceImpl retrieverService,
                                       AgentReaderServiceImpl readerService,
                                       AgentCommonToolsImpl agentCommonTools,
                                       @Value("${paper.retrieval.smart-read-limit:2}") int smartReadLimit,
                                       @Value("${paper.retrieval.smart-read-single-section-ratio:1.35}") double smartReadSingleSectionRatio) {
        this.retrieverService = retrieverService;
        this.readerService = readerService;
        this.agentCommonTools = agentCommonTools;
        this.smartReadLimit = Math.max(0, smartReadLimit);
        this.smartReadSingleSectionRatio = Math.max(1.0, smartReadSingleSectionRatio);
    }

    /**
     * 执行论文库混合检索，并将结构化证据格式化为 ResearchAgent 可直接阅读的文本。
     */
    @Override
    @Tool("Search the paper library with vector and full-text retrieval. 'paperId' is optional. 'threshold' is the minimum vector similarity (0.2-0.95). Returns ranked evidence with paper, outline path, page, quote, score, and retrieval channels.")
    public String searchLibrary(String query, Long paperId, Double threshold) {
        return searchLibraryWithEvidence(query, paperId, threshold).getContent();
    }

    /**
     * 执行混合检索，并把真实召回结果同时作为工具正文和结构化证据返回。
     */
    @Override
    public ToolResult searchLibraryWithEvidence(String query, Long paperId, Double threshold) {
        PaperRetrievalResult result = retrieverService.retrieve(
                new PaperRetrievalQuery(query, paperId, threshold, null));
        Map<String, ToolEvidence> evidence = new LinkedHashMap<>();
        result.evidence().stream()
                .map(this::toToolEvidence)
                .forEach(item -> evidence.putIfAbsent(item.getEvidenceKey(), item));

        StringBuilder content = new StringBuilder(formatSearchResult(result));
        Map<String, PaperEvidence> matchedSections = new LinkedHashMap<>();
        for (PaperEvidence item : result.evidence()) {
            if (item.getEvidenceType() == cn.winddol.ai.paper.domain.retrieval.EvidenceType.SECTION
                    && item.getSectionId() != null && !item.getSectionId().isBlank()) {
                matchedSections.putIfAbsent(item.getSectionId(), item);
            }
        }
        int expansionLimit = determineSmartReadLimit(List.copyOf(matchedSections.values()));
        if (expansionLimit > 0) {
            content.append("\n### Structurally Expanded Section Context\n");
            content.append("The chunks above are retrieval anchors. The complete matched sections below are the context units.\n\n");
            content.append("Smart Read expanded ").append(expansionLimit)
                    .append(" of ").append(matchedSections.size()).append(" unique candidate sections.\n\n");
            int expandedCount = 0;
            for (String sectionId : matchedSections.keySet()) {
                if (expandedCount++ >= expansionLimit) {
                    break;
                }
                ToolResult expanded = readerService.readSectionWithEvidence(sectionId);
                content.append("#### Expanded sectionId=").append(sectionId).append("\n");
                content.append(expanded.getContent()).append("\n");
                if (expanded.getEvidence() != null) {
                    for (ToolEvidence item : expanded.getEvidence()) {
                        evidence.putIfAbsent(item.getEvidenceKey(), item);
                    }
                }
            }
        }
        return ToolResult.ok(content.toString(), List.copyOf(evidence.values()));
    }

    /**
     * Expand at most the configured number of sections. When the first section's fused RRF
     * score clearly dominates the second, one complete Smart Read is enough; otherwise two
     * nearby candidates are retained for cross-checking.
     */
    private int determineSmartReadLimit(List<PaperEvidence> candidates) {
        int limit = Math.min(smartReadLimit, candidates.size());
        if (limit <= 1 || candidates.size() < 2) {
            return limit;
        }
        double first = score(candidates.get(0));
        double second = score(candidates.get(1));
        if (first > 0.0 && (second <= 0.0 || first / second >= smartReadSingleSectionRatio)) {
            return 1;
        }
        return limit;
    }

    private double score(PaperEvidence evidence) {
        return evidence.getFusionScore() == null ? 0.0 : evidence.getFusionScore();
    }

    @Override
    @Tool("Get the hierarchical outline (table of contents) of a specific paper. Input is the paperId (e.g., 7).")
    public String getPaperOutline(Long paperId) {
        List<OutlineNode> outline = readerService.getPaperOutline(paperId);
        return JSON.toJSONString(outline);
    }

    @Override
    @Tool("Read the full content of a specific section. Input is the section UUID. This tool also provides available symbol definitions and the evidence trail retains the outline path for review.")
    public String readSection(String sectionUuid) {
        return readSectionWithEvidence(sectionUuid).getContent();
    }

    /**
     * 读取章节及其符号，并保留此次读取所对应的章节级证据。
     */
    @Override
    public ToolResult readSectionWithEvidence(String sectionUuid) {
        return readerService.readSectionWithEvidence(sectionUuid);
    }

    @Override
    @Tool("""
    Look up the specific details (Title and Abstract) of a reference cited in the paper.
    Use this tool when you encounter citation marks like '[12]', 'Ref. 24', or 'in [5]' in the text
    and you need to understand what that external source is about.
    Input: paperId (Long), refIndex (String, e.g., '24').
    """)
    public String lookupReference(Long paperId, String refIndex) {
        return lookupReferenceWithEvidence(paperId, refIndex).getContent();
    }

    /**
     * 查询参考文献并返回该参考文献的可复查证据项。
     */
    @Override
    public ToolResult lookupReferenceWithEvidence(Long paperId, String refIndex) {
        return readerService.lookupReferenceWithEvidence(paperId, refIndex);
    }

    @Override
    @Tool("""
    Check for inter-paper relationships, such as conflicts, supports, or extensions
    discovered by the Librarian. Use this when the user asks about how a paper
    relates to the rest of the library or if there are contradictions.
    Input: paperId (Long).
    """)
    public String checkPaperRelations(Long paperId) {
        return readerService.checkPaperRelations(paperId);
    }

    @Override
    @Tool("""
    Search for specific papers in the library metadata (Title, Authors, Abstract, Year).
    Use this when the user asks 'Do we have any papers by [Author]?' or 'List papers about [Topic]'.
    Returns a list of Paper IDs and Titles.
    Input: query (String), threshold is optional (0.35-0.7).
    """)
    public String findPapers(String query, Double threshold) {
        return agentCommonTools.findPapers(query, threshold);
    }

    @Override
    @Tool("""
    Identify the most influential references within the private library.
    Use this to find 'foundational works' or 'common baselines' that multiple papers cite.
    Input: limit (Integer, optional, default 5).
    """)
    public String getTopCitedReferences(Integer limit) {
        return agentCommonTools.getTopCitedReferences(limit);
    }

    /**
     * 输出证据类型、论文、Outline 路径、页码、分数、召回通道和原文片段。
     */
    private String formatSearchResult(PaperRetrievalResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.evidence() == null || result.evidence().isEmpty()) {
            return "No relevant evidence found in the library for query: " + result.query();
        }
        sb.append("### Ranked Evidence\n");
        for (int i = 0; i < result.evidence().size(); i++) {
            PaperEvidence evidence = result.evidence().get(i);
            sb.append(String.format("%d. **[%s] %s**\n", i + 1,
                    evidence.getEvidenceType(), safe(evidence.getHeading())));
            sb.append(String.format("   Paper: \"%s\" (paperId=%s)\n",
                    safe(evidence.getPaperTitle()), evidence.getPaperId()));
            if (evidence.getHeadingPath() != null) {
                sb.append(String.format("   Outline: %s\n", evidence.getHeadingPath()));
            }
            if (evidence.getSectionId() != null) {
                sb.append(String.format("   Section ID: %s; Chunk ID: %s\n",
                        evidence.getSectionId(), evidence.getChunkId()));
            }
            if (evidence.getReferenceIndex() != null) {
                sb.append(String.format("   Reference index: %s\n", evidence.getReferenceIndex()));
            }
            if (evidence.getPageStart() != null) {
                String page = evidence.getPageEnd() != null
                        && !evidence.getPageStart().equals(evidence.getPageEnd())
                        ? evidence.getPageStart() + "-" + evidence.getPageEnd()
                        : evidence.getPageStart().toString();
                sb.append(String.format("   Page: %s\n", page));
            }
            sb.append(String.format("   Score: %.6f; Channels: %s\n",
                    evidence.getFusionScore(), evidence.getMatchedChannels()));
            sb.append(String.format("   Quote: %s\n\n", evidence.getQuote()));
        }
        sb.append(String.format("Retrieval version: %s; elapsed: %d ms\n",
                result.retrievalVersion(), result.elapsedMillis()));
        return sb.toString();
    }

    /**
     * 将论文检索领域对象转换为跨模块通用的工具证据，供 ResearchAgent 汇总并通过 SSE 输出。
     */
    private ToolEvidence toToolEvidence(PaperEvidence evidence) {
        return ToolEvidence.builder()
                .evidenceKey(evidence.getEvidenceKey())
                .evidenceType(evidence.getEvidenceType() == null ? null : evidence.getEvidenceType().name())
                .paperId(evidence.getPaperId())
                .paperTitle(evidence.getPaperTitle())
                .sectionId(evidence.getSectionId())
                .chunkId(evidence.getChunkId())
                .heading(evidence.getHeading())
                .headingPath(evidence.getHeadingPath())
                .pageStart(evidence.getPageStart())
                .pageEnd(evidence.getPageEnd())
                .referenceIndex(evidence.getReferenceIndex())
                .quote(evidence.getQuote())
                .accessedVia("searchLibrary")
                .build();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
