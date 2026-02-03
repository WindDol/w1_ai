package cn.winddol.ai.domain.paper.adapter.tools;

import cn.winddol.ai.domain.paper.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paper.model.entity.OutlineNode;
import cn.winddol.ai.domain.paper.service.AgentReaderService;
import cn.winddol.ai.domain.paper.service.HybridRetrieverService;
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
        // 调用升级后的 Service
        SearchResultDTO result = retrieverService.searchLibrary(query, paperId);

        // 调用升级后的格式化方法
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

    // --- 辅助方法：格式化输出给 LLM 看 ---
    private String formatSearchResult(SearchResultDTO result) {
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;

        // 1. 格式化符号 (Symbols) - 增加定义公式和来源
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

        // 2. 格式化正文 (Sections) - 增加来源、ID 和预览
        if (result.getSections() != null && !result.getSections().isEmpty()) {
            sb.append("### 🔍 Found Sections (Candidate Locations):\n");
            for (SearchResultDTO.SectionDTO s : result.getSections()) {
                sb.append(String.format("- **Header**: %s\n", s.getHeader()));
                sb.append(String.format("  **Source**: \"%s\"\n", s.getSourcePaperTitle())); // 关键：告诉 Agent 来源
                // 🔥 关键：打印 ID，Agent 下一步调用 readSection 需要这个！
                sb.append(String.format("  **ID**: %s\n", s.getId()));
                // 只展示前 150 字预览，诱导 Agent 去读全文
                sb.append(String.format("  **Preview**: %s\n\n", truncate(s.getContent(), 300)));
            }
            hasContent = true;
        }

        // 3. 【新增】格式化引用文献 (References) - W2D2 的成果
        if (result.getReferences() != null && !result.getReferences().isEmpty()) {
            sb.append("### 📚 Found References (External Context):\n");
            sb.append("(These papers are CITED by the source papers)\n");
            for (SearchResultDTO.ReferenceDTO r : result.getReferences()) {
                sb.append(String.format("- **Title**: %s\n", r.getTitle()));
                sb.append(String.format("  **Cited By**: \"%s\"\n", r.getSourcePaperTitle())); // 关键：谁引用了它
                // 展示摘要预览
                String abstractPreview = r.getAbstractText() != null ? truncate(r.getAbstractText(), 200) : "No abstract available.";
                sb.append(String.format("  **Abstract**: %s\n\n", abstractPreview));
            }
            hasContent = true;
        }

        if (!hasContent) {
            return "No relevant information found in the library for query: " + result.toString(); // 简单防空
        }

        return sb.toString();
    }

    // 辅助工具：字符串截断 (防止 Token 爆炸)
    private String truncate(String input, int limit) {
        if (input == null) return "";
        String clean = input.replaceAll("\\s+", " ").trim(); // 去除多余换行
        if (clean.length() <= limit) return clean;
        return clean.substring(0, limit) + "...";
    }
}
