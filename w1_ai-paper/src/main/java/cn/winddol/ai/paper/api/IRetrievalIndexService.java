package cn.winddol.ai.paper.api;

public interface IRetrievalIndexService {

    /**
     * 重建派生的 Chunk 检索索引，不修改原始 Outline 和 Section。
     */
    int rebuild(Long paperId, String parserArtifactPath);
}
