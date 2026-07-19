package cn.winddol.ai.infrastructure.dao;

import cn.winddol.ai.paper.domain.retrieval.RetrievalCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RetrievalMapper {

    List<RetrievalCandidate> searchSectionChunksByVector(
            @Param("paperId") Long paperId,
            @Param("vectorStr") String vectorStr,
            @Param("minScore") double minScore,
            @Param("limit") int limit);

    List<RetrievalCandidate> searchSectionChunksByFullText(
            @Param("paperId") Long paperId,
            @Param("query") String query,
            @Param("limit") int limit);

    List<RetrievalCandidate> searchSymbolsByVector(
            @Param("paperId") Long paperId,
            @Param("vectorStr") String vectorStr,
            @Param("minScore") double minScore,
            @Param("limit") int limit);

    List<RetrievalCandidate> searchSymbolsByFullText(
            @Param("paperId") Long paperId,
            @Param("query") String query,
            @Param("limit") int limit);

    List<RetrievalCandidate> searchSymbolsByExact(
            @Param("paperId") Long paperId,
            @Param("query") String query,
            @Param("limit") int limit);

    List<RetrievalCandidate> searchReferencesByVector(
            @Param("paperId") Long paperId,
            @Param("vectorStr") String vectorStr,
            @Param("minScore") double minScore,
            @Param("limit") int limit);

    List<RetrievalCandidate> searchReferencesByFullText(
            @Param("paperId") Long paperId,
            @Param("query") String query,
            @Param("limit") int limit);
}
