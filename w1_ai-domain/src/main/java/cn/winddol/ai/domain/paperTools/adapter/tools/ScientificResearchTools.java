package cn.winddol.ai.domain.paperTools.adapter.tools;

import cn.winddol.ai.domain.paperTools.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paperTools.model.entity.OutlineNode;
import cn.winddol.ai.domain.paperTools.service.AgentReaderService;
import cn.winddol.ai.domain.paperTools.service.HybridRetrieverService;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;

import java.util.List;

@Component
public class ScientificResearchTools {

    @Resource
    private HybridRetrieverService retrieverService;
    @Resource
    private AgentReaderService readerService;

    @Tool("Search the paper library. 'query' is the keyword/sentence. 'paperId' is optional: set specific ID to search within one paper, or set NULL to search the entire library. Returns candidate sections, symbols, and references.")
    public String searchLibrary(String query, Long paperId) {
        SearchResultDTO result = retrieverService.searchLibrary(query, paperId);
        return formatSearchResult(result);
    }

    // --- 工具 2: 目录地图 ---

    @Tool("Get the hierarchical outline (table of contents) of a specific paper. Input is the paperId (e.g., 7).")
    public String getPaperOutline(Long paperId) {
        List<OutlineNode> outline = readerService.getPaperOutline(paperId);
        return JSON.toJSONString(outline);
    }

    // --- 工具 3: 深度阅读 ---

    @Tool("Read the full content of a specific section. Input is the section UUID. This tool also provides context from parent sections and local symbol definitions.")
    public String readSection(String sectionUuid) {
        return readerService.readSectionWithContext(sectionUuid);
    }

    @Tool("""
    Look up the specific details (Title and Abstract) of a reference cited in the paper. 
    Use this tool when you encounter citation marks like '[12]', 'Ref. 24', or 'in [5]' in the text 
    and you need to understand what that external source is about.
    Input: paperId (Long), refIndex (String, e.g., '24').
    """)
    public String lookupReference(Long paperId, String refIndex) {
        return readerService.lookupReference(paperId, refIndex);
    }


    // --- 辅助方法：格式化输出给 LLM 看 ---
    private String formatSearchResult(SearchResultDTO result) {
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;

        if (result.getSymbols() != null && !result.getSymbols().isEmpty()) {
            sb.append("### 🔣 Related Symbols (Terminology):\n");
            for (SearchResultDTO.SymbolDTO s : result.getSymbols()) {
                sb.append(String.format("- **%s**: %s", s.getSymbol(), s.getDescription()));

                // 展示来源论文，防止 Agent 混淆不同论文的符号
                if (s.getSourcePaperTitle() != null) {
                    sb.append(String.format(" (Source: \"%s\")", s.getSourcePaperTitle()));
                }
                // 展示数学定义，增强推理能力
                if (s.getDefinitionFormula() != null) {
                    sb.append(String.format(" [Def: $%s$]", s.getDefinitionFormula()));
                }
                sb.append("\n");
            }
            sb.append("\n");
            hasContent = true;
        }

        if (result.getSections() != null && !result.getSections().isEmpty()) {
            sb.append("### 🔍 Found Sections (Candidate Locations):\n");
            for (SearchResultDTO.SectionDTO s : result.getSections()) {
                sb.append(String.format("- **Header**: %s\n", s.getHeader()));
                sb.append(String.format("  **Source**: \"%s\"\n", s.getSourcePaperTitle())); // 关键：告诉 Agent 来源
                sb.append(String.format("  **ID**: %s\n", s.getId()));
                sb.append(String.format("  **Preview**: %s\n\n", truncate(s.getContent(), 300)));
            }
            hasContent = true;
        }

        if (result.getReferences() != null && !result.getReferences().isEmpty()) {
            sb.append("### 📚 Found References (External Context):\n");
            for (SearchResultDTO.ReferenceDTO r : result.getReferences()) {
                sb.append(String.format("- **[%s] %s**\n", r.getRefId(), r.getTitle()));

                if (r.getLinkedPaperId() != null) {
                    sb.append(String.format("  🌟 [FULL TEXT AVAILABLE] This paper is in your library. ID: %d\n", r.getLinkedPaperId()));
                    sb.append(String.format("  Action Hint: You can use `getPaperOutline(%d)` to explore it deeper.\n", r.getLinkedPaperId()));
                }

                sb.append(String.format("  **Cited By**: \"%s\" (Paper ID: %d)\n", r.getSourcePaperTitle(), r.getPaperId()));
                String abstractPreview = r.getAbstractText() != null ? truncate(r.getAbstractText(), 200) : "No abstract available.";
                sb.append(String.format("  **Abstract**: %s\n\n", abstractPreview));
            }
        }

        if (!hasContent) {
            return "No relevant information found in the library for query: " + result.toString(); // 简单防空
        }

        return sb.toString();
    }

    private String truncate(String input, int limit) {
        if (input == null) return "";
        String clean = input.replaceAll("\\s+", " ").trim(); // 去除多余换行
        if (clean.length() <= limit) return clean;
        return clean.substring(0, limit) + "...";
    }
}
