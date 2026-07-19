package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.api.IEmbeddingService;
import cn.winddol.ai.paper.api.IParserArtifactReader;
import cn.winddol.ai.paper.api.IRetrievalIndexService;
import cn.winddol.ai.paper.api.IRetrievalRepository;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.retrieval.SectionChunk;
import cn.winddol.ai.paper.domain.retrieval.SourceTextBlock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RetrievalIndexServiceImpl implements IRetrievalIndexService {

    private final IRetrievalRepository repository;
    private final IEmbeddingService embeddingService;
    private final IParserArtifactReader artifactReader;
    private final StructuredSectionChunker chunker;
    private final HeadingPathResolver headingPathResolver;
    private final String embeddingModel;

    public RetrievalIndexServiceImpl(IRetrievalRepository repository,
                                     IEmbeddingService embeddingService,
                                     IParserArtifactReader artifactReader,
                                     StructuredSectionChunker chunker,
                                     HeadingPathResolver headingPathResolver,
                                     @Value("${ai.embedding.model:text-embedding-v4}") String embeddingModel) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.artifactReader = artifactReader;
        this.chunker = chunker;
        this.headingPathResolver = headingPathResolver;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 从已入库 Section 重建论文的 Chunk 索引，并尽可能补充页码和 embedding。
     * 全部 Chunk 构建成功后才替换数据库中的旧索引。
     */
    @Override
    public int rebuild(Long paperId, String parserArtifactPath) {
        List<SectionEntity> sections = repository.findSectionsForIndex(paperId);
        if (sections.isEmpty()) {
            repository.deleteSectionChunks(paperId);
            return 0;
        }

        Map<String, String> headingPaths = headingPathResolver.resolve(sections);
        // 页码属于可选增强信息；没有文本块时降级为无页码 Chunk，不中断索引构建。
        List<SourceTextBlock> sourceBlocks = artifactReader.readTextBlocks(parserArtifactPath);
        SourcePageLocator pageLocator = new SourcePageLocator(sourceBlocks);
        List<SectionChunk> chunks = new ArrayList<>();

        for (SectionEntity section : sections) {
            // Chunk 只允许在单个 Section 内拆分，避免一条证据跨越不同 Outline 节点。
            List<SectionChunk> sectionChunks = chunker.chunk(section, headingPaths.get(section.getId()));
            for (SectionChunk chunk : sectionChunks) {
                SourcePageLocator.PageLocation location = pageLocator.locate(chunk.getContent());
                chunk.setPageStart(location.start());
                chunk.setPageEnd(location.end());
                chunk.setEmbeddingModel(embeddingModel);
                // 将标题路径加入向量文本，用于区分不同章节中内容相似的段落。
                chunk.setEmbedding(embeddingService.embed(embeddingText(chunk)));
            }
            chunks.addAll(sectionChunks);
        }

        // 所有 embedding 成功后再整体替换；仓储层通过同一事务完成删除和插入。
        repository.replaceSectionChunks(paperId, chunks);
        return chunks.size();
    }

    /**
     * 将 Outline 路径与正文组合为向量化输入，为 Chunk 增加章节语境。
     */
    private String embeddingText(SectionChunk chunk) {
        return "Heading path: " + safe(chunk.getHeadingPath()) + "\nContent: " + safe(chunk.getContent());
    }

    /**
     * 将可空文本转换为空字符串，避免拼接 embedding 输入时出现 null。
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
