package cn.winddol.ai.paper.domain.retrieval;

public record PaperRetrievalQuery(
        String query,
        Long paperId,
        Double minSemanticScore,
        Integer limit
) {
}
