package cn.winddol.ai.paper.domain.retrieval;

import java.util.List;

public record PaperRetrievalResult(
        String query,
        Long paperId,
        String retrievalVersion,
        long elapsedMillis,
        List<PaperEvidence> evidence
) {
}
