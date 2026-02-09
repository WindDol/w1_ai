package cn.winddol.ai.domain.agent.adapter.embedding;

public interface IEmbeddingProcessor {
    void embedReferences(Long paperId);
    void embedSections(Long paperId);
}
