package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.retrieval.RetrievalCandidate;
import cn.winddol.ai.paper.domain.retrieval.SectionChunk;

import java.util.List;

public interface IRetrievalRepository {

    // 摄取流程 EMBEDDING 阶段使用的索引生命周期操作。
    List<SectionEntity> findSectionsForIndex(Long paperId);

    void replaceSectionChunks(Long paperId, List<SectionChunk> chunks);

    void deleteSectionChunks(Long paperId);

    // 按证据类型和召回通道分别查询候选，最终由领域服务统一融合排名。
    List<RetrievalCandidate> searchSectionChunksByVector(Long paperId, String vector,
                                                         double minScore, int limit);

    List<RetrievalCandidate> searchSectionChunksByFullText(Long paperId, String query, int limit);

    List<RetrievalCandidate> searchSymbolsByVector(Long paperId, String vector,
                                                   double minScore, int limit);

    List<RetrievalCandidate> searchSymbolsByFullText(Long paperId, String query, int limit);

    List<RetrievalCandidate> searchSymbolsByExact(Long paperId, String query, int limit);

    List<RetrievalCandidate> searchReferencesByVector(Long paperId, String vector,
                                                      double minScore, int limit);

    List<RetrievalCandidate> searchReferencesByFullText(Long paperId, String query, int limit);
}
