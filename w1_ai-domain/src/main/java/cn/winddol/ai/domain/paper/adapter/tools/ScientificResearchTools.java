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

    @Tool("Search the paper library for relevant sections and symbols. Use this to find WHERE to read. Returns a list of section headers and symbol definitions.")
    public String searchLibrary(String query) {
        SearchResultDTO result = retrieverService.searchLibrary(query);

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
        if (result.getSymbols() != null && !result.getSymbols().isEmpty()) {
            sb.append("--- Found Symbols ---\n");
            result.getSymbols().forEach(s ->
                    sb.append(String.format("- %s: %s\n", s.getSymbol(), s.getDescription()))
            );
        }
        if (result.getSections() != null && !result.getSections().isEmpty()) {
            sb.append("--- Found Sections ---\n");
            result.getSections().forEach(s ->
                    sb.append(String.format("- Title: %s (ID: %s)\n", s.getHeader(), s.getId()))
            );
        }
        return sb.toString();
    }
}
