package cn.winddol.ai.domain.paperTools.service;

import cn.winddol.ai.domain.paperTools.model.entity.PaperEntity;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.model.entity.GlobalReferenceEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentCommonTools {
    @Resource
    private IPaperRepository repository;

    public String findPapers(String query, Double threshold){
        List<PaperEntity> papers = repository.searchPapers(query,threshold);
        return formatPaperList(papers);
    }

    public String getTopCitedReferences(Integer limit) {
        if (limit == null) limit = 5;
        // 逻辑：SELECT raw_text, COUNT(*) as freq FROM section_reference_links ... GROUP BY ref_fingerprint
        // 或者直接查询 global_references 的关联计数
        List<GlobalReferenceEntity> topRefs = repository.getTopFrequentReferences(limit);
        return formatTopRefs(topRefs);
    }
    private String formatPaperList(List<PaperEntity> papers) {
        if (papers == null || papers.isEmpty()) {
            return "No matching papers found in the library.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 📚 Relevant Papers Found in Library:\n");
        sb.append("Use the 'paperId' to explore the outline of a specific paper.\n\n");

        for (PaperEntity p : papers) {
            sb.append(String.format("- **[ID: %d] %s**\n", p.getId(), p.getTitle()));

            // 提取并截断摘要，只给 Agent 一个简短的背景参考
            String abs = p.getAbstractText();
            if (abs != null && !abs.isEmpty()) {
                String shortAbs = abs.length() > 300 ? abs.substring(0, 300) + "..." : abs;
                sb.append("  > Abstract: ").append(shortAbs).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    private String formatTopRefs(List<GlobalReferenceEntity> refs) {
        if (refs == null || refs.isEmpty()) {
            return "No citation data available in the library.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 🔝 Foundational References (Top Cited in Local Library):\n");
        sb.append("These works are frequently cited across multiple papers in your collection.\n\n");

        for (int i = 0; i < refs.size(); i++) {
            GlobalReferenceEntity r = refs.get(i);
            sb.append(String.format("%d. **%s**\n", i + 1, r.getTitle()));

            sb.append(String.format("   - Local Library Citations: **%d times**\n", r.getCitationCount()));

            sb.append(String.format("   - Source Status: %s\n", r.getSourceType()));
            sb.append("\n");
        }
        return sb.toString();
    }
}
