package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.infrastructure.dao.RetrievalMapper;
import cn.winddol.ai.infrastructure.dao.SectionChunkMapper;
import cn.winddol.ai.infrastructure.dao.SectionMapper;
import cn.winddol.ai.infrastructure.dao.po.Section;
import cn.winddol.ai.infrastructure.dao.po.SectionChunkPO;
import cn.winddol.ai.paper.api.IRetrievalRepository;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.retrieval.RetrievalCandidate;
import cn.winddol.ai.paper.domain.retrieval.SectionChunk;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class RetrievalRepository implements IRetrievalRepository {

    private final SectionMapper sectionMapper;
    private final SectionChunkMapper chunkMapper;
    private final RetrievalMapper retrievalMapper;

    public RetrievalRepository(SectionMapper sectionMapper,
                               SectionChunkMapper chunkMapper,
                               RetrievalMapper retrievalMapper) {
        this.sectionMapper = sectionMapper;
        this.chunkMapper = chunkMapper;
        this.retrievalMapper = retrievalMapper;
    }

    /**
     * 按章节顺序读取论文的完整 Section，作为 Chunk 索引的唯一正文来源。
     */
    @Override
    public List<SectionEntity> findSectionsForIndex(Long paperId) {
        return sectionMapper.selectList(new LambdaQueryWrapper<Section>()
                        .eq(Section::getPaperId, paperId)
                        .orderByAsc(Section::getIdx))
                .stream()
                .map(this::toSectionEntity)
                .toList();
    }

    /**
     * 在同一事务内用新 Chunk 全量替换指定论文的旧检索索引。
     */
    @Override
    @Transactional
    public void replaceSectionChunks(Long paperId, List<SectionChunk> chunks) {
        // 同一事务内删除旧索引并写入新索引，写入不完整时整体回滚，避免版本混杂。
        deleteSectionChunks(paperId);
        for (SectionChunk chunk : chunks) {
            chunkMapper.insert(toPo(chunk));
        }
    }

    /**
     * 删除指定论文的全部派生 Chunk，不影响原始 Section 和 Outline。
     */
    @Override
    public void deleteSectionChunks(Long paperId) {
        chunkMapper.delete(new LambdaQueryWrapper<SectionChunkPO>()
                .eq(SectionChunkPO::getPaperId, paperId));
    }

    /**
     * 使用 pgvector 从 Section Chunk 中召回满足相似度阈值的候选。
     */
    @Override
    public List<RetrievalCandidate> searchSectionChunksByVector(Long paperId, String vector,
                                                                double minScore, int limit) {
        return retrievalMapper.searchSectionChunksByVector(paperId, vector, minScore, limit);
    }

    /**
     * 使用 PostgreSQL 全文索引从 Section Chunk 中召回词法候选。
     */
    @Override
    public List<RetrievalCandidate> searchSectionChunksByFullText(Long paperId, String query, int limit) {
        return retrievalMapper.searchSectionChunksByFullText(paperId, query, limit);
    }

    /**
     * 使用已保存的符号 embedding 召回语义相关符号。
     */
    @Override
    public List<RetrievalCandidate> searchSymbolsByVector(Long paperId, String vector,
                                                          double minScore, int limit) {
        return retrievalMapper.searchSymbolsByVector(paperId, vector, minScore, limit);
    }

    /**
     * 根据符号、描述和定义公式召回全文匹配候选。
     */
    @Override
    public List<RetrievalCandidate> searchSymbolsByFullText(Long paperId, String query, int limit) {
        return retrievalMapper.searchSymbolsByFullText(paperId, query, limit);
    }

    /**
     * 对符号原文或 LaTeX 表达式执行大小写无关的精确匹配。
     */
    @Override
    public List<RetrievalCandidate> searchSymbolsByExact(Long paperId, String query, int limit) {
        return retrievalMapper.searchSymbolsByExact(paperId, query, limit);
    }

    /**
     * 使用全局标准引用的 embedding 召回当前论文中的相关引用。
     */
    @Override
    public List<RetrievalCandidate> searchReferencesByVector(Long paperId, String vector,
                                                             double minScore, int limit) {
        return retrievalMapper.searchReferencesByVector(paperId, vector, minScore, limit);
    }

    /**
     * 根据引用标题、摘要和原始引用文本召回全文匹配候选。
     */
    @Override
    public List<RetrievalCandidate> searchReferencesByFullText(Long paperId, String query, int limit) {
        return retrievalMapper.searchReferencesByFullText(paperId, query, limit);
    }

    /**
     * 将数据库 Section PO 转换成 paper 模块拥有的领域对象。
     */
    private SectionEntity toSectionEntity(Section section) {
        return SectionEntity.builder()
                .id(section.getId())
                .paperId(section.getPaperId())
                .header(section.getHeader())
                .parentId(section.getParentId())
                .content(section.getContent())
                .idx(section.getIdx())
                .build();
    }

    /**
     * 将领域 Chunk 转换成数据库持久化对象。
     */
    private SectionChunkPO toPo(SectionChunk chunk) {
        return SectionChunkPO.builder()
                .id(chunk.getId())
                .paperId(chunk.getPaperId())
                .sectionId(chunk.getSectionId())
                .chunkIndex(chunk.getChunkIndex())
                .heading(chunk.getHeading())
                .headingPath(chunk.getHeadingPath())
                .content(chunk.getContent())
                .pageStart(chunk.getPageStart())
                .pageEnd(chunk.getPageEnd())
                .tokenCount(chunk.getTokenCount())
                .contentHash(chunk.getContentHash())
                .chunkerVersion(chunk.getChunkerVersion())
                .embeddingModel(chunk.getEmbeddingModel())
                .embedding(chunk.getEmbedding())
                .build();
    }
}
