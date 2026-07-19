package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IScientificResearchTools;
import cn.winddol.ai.paper.domain.OutlineNode;
import cn.winddol.ai.paper.domain.retrieval.PaperEvidence;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalQuery;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalResult;
import cn.winddol.ai.paper.internal.retrieval.AgentCommonToolsImpl;
import cn.winddol.ai.paper.internal.retrieval.AgentReaderServiceImpl;
import cn.winddol.ai.paper.internal.retrieval.HybridRetrieverServiceImpl;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScientificResearchToolsImpl implements IScientificResearchTools {

    private final HybridRetrieverServiceImpl retrieverService;
    private final AgentReaderServiceImpl readerService;
    private final AgentCommonToolsImpl agentCommonTools;

    public ScientificResearchToolsImpl(HybridRetrieverServiceImpl retrieverService,
                                       AgentReaderServiceImpl readerService,
                                       AgentCommonToolsImpl agentCommonTools) {
        this.retrieverService = retrieverService;
        this.readerService = readerService;
        this.agentCommonTools = agentCommonTools;
    }

    /**
     * 执行论文库混合检索，并将结构化证据格式化为 ResearchAgent 可直接阅读的文本。
     */
    @Override
    @Tool("Search the paper library with vector and full-text retrieval. 'paperId' is optional. 'threshold' is the minimum vector similarity (0.2-0.95). Returns ranked evidence with paper, outline path, page, quote, score, and retrieval channels.")
    public String searchLibrary(String query, Long paperId, Double threshold) {
        PaperRetrievalResult result = retrieverService.retrieve(
                new PaperRetrievalQuery(query, paperId, threshold, null));
        return formatSearchResult(result);
    }

    @Override
    @Tool("Get the hierarchical outline (table of contents) of a specific paper. Input is the paperId (e.g., 7).")
    public String getPaperOutline(Long paperId) {
        List<OutlineNode> outline = readerService.getPaperOutline(paperId);
        return JSON.toJSONString(outline);
    }

    @Override
    @Tool("Read the full content of a specific section. Input is the section UUID. This tool also provides context from parent sections and local symbol definitions.")
    public String readSection(String sectionUuid) {
        return readerService.readSectionWithContext(sectionUuid);
    }

    @Override
    @Tool("""
    Look up the specific details (Title and Abstract) of a reference cited in the paper.
    Use this tool when you encounter citation marks like '[12]', 'Ref. 24', or 'in [5]' in the text
    and you need to understand what that external source is about.
    Input: paperId (Long), refIndex (String, e.g., '24').
    """)
    public String lookupReference(Long paperId, String refIndex) {
        return readerService.lookupReference(paperId, refIndex);
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
