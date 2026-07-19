package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.domain.retrieval.RetrievalCandidate;
import cn.winddol.ai.paper.domain.retrieval.RetrievalChannel;

import java.util.List;

public record RankedCandidates(
        RetrievalChannel channel,
        double weight,
        List<RetrievalCandidate> candidates
) {
}
