package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.domain.GlobalReferenceEntity;
import cn.winddol.ai.paper.domain.PaperEntity;
import cn.winddol.ai.paper.api.IPaperRepository;
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
        if (papers == null || papers.isEmpty()) {
            return "No papers found for query: " + query;
        }
        StringBuilder sb = new StringBuilder("### Found Papers\n");
        for (PaperEntity p : papers) {
            sb.append(String.format("- **Paper [%d]**: %s\n", p.getId(), p.getTitle()));
        }
        return sb.toString();
    }

    public String getTopCitedReferences(Integer limit) {
        List<GlobalReferenceEntity> refs = repository.getTopFrequentReferences(limit);
        if (refs == null || refs.isEmpty()) {
            return "No frequent references found.";
        }
        StringBuilder sb = new StringBuilder("### Top Cited References\n");
        for (var r : refs) {
            sb.append(String.format("- **%s** (cited %d times)\n", r.getTitle(), r.getCitationCount()));
        }
        return sb.toString();
    }
}
