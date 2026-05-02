package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.GlobalReferenceEntity;
import cn.winddol.ai.paper.domain.PaperEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentCommonToolsImpl {

    private final IPaperRepository repository;

    public AgentCommonToolsImpl(IPaperRepository repository) {
        this.repository = repository;
    }

    public String findPapers(String query, Double threshold) {
        List<PaperEntity> papers = repository.searchPapers(query, threshold);
        return formatPaperList(papers);
    }

    public String getTopCitedReferences(Integer limit) {
        if (limit == null) limit = 5;
        List<GlobalReferenceEntity> topRefs = repository.getTopFrequentReferences(limit);
        return formatTopRefs(topRefs);
    }

    private String formatPaperList(List<PaperEntity> papers) {
        if (papers == null || papers.isEmpty()) {
            return "No matching papers found in the library.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Relevant Papers Found in Library:\n");
        sb.append("Use the 'paperId' to explore the outline of a specific paper.\n\n");

        for (PaperEntity p : papers) {
            sb.append(String.format("- **[ID: %d] %s**\n", p.getId(), p.getTitle()));

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
        sb.append("### Foundational References (Top Cited in Local Library):\n");
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
