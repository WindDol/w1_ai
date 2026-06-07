package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IEmbeddingService;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.api.IScientificResearchTools;
import cn.winddol.ai.paper.model.aggregate.SearchResultDTO;
import cn.winddol.ai.paper.model.entity.OutlineNode;
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

    @Override
    @Tool("Search the paper library. 'query' is the keyword/sentence. 'paperId' is optional: set specific ID to search within one paper, or set NULL to search the entire library. 'threshold' is optional (0.35-0.7), or set NULL. Returns candidate sections, symbols, and references.")
    public String searchLibrary(String query, Long paperId, Double threshold) {
        SearchResultDTO result = retrieverService.searchLibrary(query, paperId, threshold);
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

    private String formatSearchResult(SearchResultDTO result) {
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;

        if (result.getSymbols() != null && !result.getSymbols().isEmpty()) {
            sb.append("### Related Symbols (Terminology):\n");
            for (SearchResultDTO.SymbolDTO s : result.getSymbols()) {
                sb.append(String.format("- **%s**: %s", s.getSymbol(), s.getDescription()));

                if (s.getSourcePaperTitle() != null) {
                    sb.append(String.format(" (Source: \"%s\")", s.getSourcePaperTitle()));
                }
                if (s.getDefinitionFormula() != null) {
                    sb.append(String.format(" [Def: $%s$]", s.getDefinitionFormula()));
                }
                sb.append("\n");
            }
            sb.append("\n");
            hasContent = true;
        }

        if (result.getSections() != null && !result.getSections().isEmpty()) {
            sb.append("### Found Sections (Candidate Locations):\n");
            for (SearchResultDTO.SectionDTO s : result.getSections()) {
                sb.append(String.format("- **Header**: %s\n", s.getHeader()));
                sb.append(String.format("  **Source**: \"%s\"\n", s.getSourcePaperTitle()));
                sb.append(String.format("  **ID**: %s\n", s.getId()));
                sb.append(String.format("  **Preview**: %s\n\n", truncate(s.getContent(), 300)));
            }
            hasContent = true;
        }

        if (result.getReferences() != null && !result.getReferences().isEmpty()) {
            sb.append("### Found References (External Context):\n");
            for (SearchResultDTO.ReferenceDTO r : result.getReferences()) {
                sb.append(String.format("- **[%s] %s**\n", r.getRefId(), r.getTitle()));

                if (r.getLinkedPaperId() != null) {
                    sb.append(String.format("  [FULL TEXT AVAILABLE] This paper is in your library. ID: %d\n", r.getLinkedPaperId()));
                    sb.append(String.format("  Action Hint: You can use `getPaperOutline(%d)` to explore it deeper.\n", r.getLinkedPaperId()));
                }

                sb.append(String.format("  **Cited By**: \"%s\" (Paper ID: %d)\n", r.getSourcePaperTitle(), r.getPaperId()));
                String abstractPreview = r.getAbstractText() != null ? truncate(r.getAbstractText(), 200) : "No abstract available.";
                sb.append(String.format("  **Abstract**: %s\n\n", abstractPreview));
            }
        }

        if (!hasContent) {
            return "No relevant information found in the library for query: " + result;
        }

        return sb.toString();
    }

    private String truncate(String input, int limit) {
        if (input == null) return "";
        String clean = input.replaceAll("\\s+", " ").trim();
        if (clean.length() <= limit) return clean;
        return clean.substring(0, limit) + "...";
    }
}
