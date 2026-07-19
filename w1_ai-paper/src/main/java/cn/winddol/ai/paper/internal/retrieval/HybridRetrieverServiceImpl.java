package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.api.IEmbeddingService;
import cn.winddol.ai.paper.api.IPaperRetrievalService;
import cn.winddol.ai.paper.api.IRetrievalRepository;
import cn.winddol.ai.paper.domain.SearchResultDTO;
import cn.winddol.ai.paper.domain.retrieval.EvidenceType;
import cn.winddol.ai.paper.domain.retrieval.PaperEvidence;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalQuery;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalResult;
import cn.winddol.ai.paper.domain.retrieval.RetrievalCandidate;
import cn.winddol.ai.paper.domain.retrieval.RetrievalChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class HybridRetrieverServiceImpl implements IPaperRetrievalService {

    private static final double DEFAULT_THRESHOLD = 0.40;
    private static final double MIN_THRESHOLD = 0.20;
    private static final double MAX_THRESHOLD = 0.95;
    private static final String RETRIEVAL_VERSION = "rrf-v1";

    private final IRetrievalRepository repository;
    private final IEmbeddingService embeddingService;
    private final ReciprocalRankFusion fusion;
    private final EvidenceQuoteExtractor quoteExtractor;
    private final int candidateLimit;
    private final int defaultLimit;
    private final String retrievalMode;

    public HybridRetrieverServiceImpl(IRetrievalRepository repository,
                                      IEmbeddingService embeddingService,
                                      ReciprocalRankFusion fusion,
                                      EvidenceQuoteExtractor quoteExtractor,
                                      @Value("${paper.retrieval.candidate-limit:30}") int candidateLimit,
                                      @Value("${paper.retrieval.top-k:8}") int defaultLimit,
                                      @Value("${paper.retrieval.mode:hybrid}") String retrievalMode) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.fusion = fusion;
        this.quoteExtractor = quoteExtractor;
        this.candidateLimit = Math.max(5, candidateLimit);
        this.defaultLimit = Math.max(1, defaultLimit);
        this.retrievalMode = normalizeMode(retrievalMode);
    }

    /**
     * 按配置执行向量、全文和精确召回，通过 RRF 融合后返回结构化证据。
     * Hybrid 模式允许在 embedding 服务异常时降级为词法检索。
     */
    @Override
    public PaperRetrievalResult retrieve(PaperRetrievalQuery request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("Retrieval query must not be blank");
        }
        long startedAt = System.currentTimeMillis();
        String query = request.query().trim();
        double threshold = normalizeThreshold(request.minSemanticScore());
        int limit = request.limit() == null ? defaultLimit : Math.max(1, request.limit());
        List<RankedCandidates> ranked = new ArrayList<>();

        // 词法通道用于补充向量检索容易遗漏的精确符号、专有名词和论文术语。
        if (!"vector".equals(retrievalMode)) {
            ranked.add(new RankedCandidates(RetrievalChannel.EXACT, 1.25,
                    repository.searchSymbolsByExact(request.paperId(), query, candidateLimit)));
            ranked.add(new RankedCandidates(RetrievalChannel.FULL_TEXT, 1.0,
                    repository.searchSectionChunksByFullText(request.paperId(), query, candidateLimit)));
            ranked.add(new RankedCandidates(RetrievalChannel.FULL_TEXT, 0.85,
                    repository.searchSymbolsByFullText(request.paperId(), query, candidateLimit)));
            ranked.add(new RankedCandidates(RetrievalChannel.FULL_TEXT, 0.75,
                    repository.searchReferencesByFullText(request.paperId(), query, candidateLimit)));
        }

        if (!"fulltext".equals(retrievalMode)) {
            try {
                String vector = Arrays.toString(embeddingService.embed(query));
                ranked.add(new RankedCandidates(RetrievalChannel.VECTOR, 1.0,
                        repository.searchSectionChunksByVector(request.paperId(), vector, threshold, candidateLimit)));
                ranked.add(new RankedCandidates(RetrievalChannel.VECTOR, 0.85,
                        repository.searchSymbolsByVector(request.paperId(), vector, threshold, candidateLimit)));
                ranked.add(new RankedCandidates(RetrievalChannel.VECTOR, 0.75,
                        repository.searchReferencesByVector(request.paperId(), vector, threshold, candidateLimit)));
            } catch (RuntimeException embeddingError) {
                if ("vector".equals(retrievalMode)) {
                    throw embeddingError;
                }
                // Hybrid 模式在 embedding 服务不可用时降级为全文检索，保证基础查询仍可使用。
                log.warn("Embedding retrieval unavailable; continuing with full-text candidates", embeddingError);
            }
        }

        // 向量分数和全文分数不能直接比较，因此使用 RRF 对各通道排名进行融合。
        List<PaperEvidence> evidence = fusion.fuse(ranked, limit).stream()
                .map(candidate -> toEvidence(query, candidate))
                .toList();
        return new PaperRetrievalResult(query, request.paperId(), retrievalMode + '-' + RETRIEVAL_VERSION,
                System.currentTimeMillis() - startedAt, evidence);
    }

    /**
     * 兼容原有调用方，将新的结构化证据结果转换成旧版 SearchResultDTO。
     */
    public SearchResultDTO searchLibrary(String query, Long paperId, Double threshold) {
        PaperRetrievalResult result = retrieve(new PaperRetrievalQuery(query, paperId, threshold, defaultLimit));
        SearchResultDTO legacy = new SearchResultDTO();
        legacy.setSections(result.evidence().stream()
                .filter(item -> item.getEvidenceType() == EvidenceType.SECTION)
                .map(this::toLegacySection)
                .toList());
        legacy.setSymbols(result.evidence().stream()
                .filter(item -> item.getEvidenceType() == EvidenceType.SYMBOL)
                .map(this::toLegacySymbol)
                .toList());
        legacy.setReferences(result.evidence().stream()
                .filter(item -> item.getEvidenceType() == EvidenceType.REFERENCE)
                .map(this::toLegacyReference)
                .toList());
        return legacy;
    }

    /**
     * 将融合后的内部候选转换成面向 Agent 的可追溯证据对象。
     */
    private PaperEvidence toEvidence(String query, RetrievalCandidate candidate) {
        return PaperEvidence.builder()
                .evidenceKey(candidate.getEvidenceKey())
                .evidenceType(candidate.getEvidenceType())
                .paperId(candidate.getPaperId())
                .paperTitle(candidate.getPaperTitle())
                .sectionId(candidate.getSectionId())
                .chunkId(candidate.getChunkId())
                .heading(candidate.getHeading())
                .headingPath(candidate.getHeadingPath())
                .pageStart(candidate.getPageStart())
                .pageEnd(candidate.getPageEnd())
                .quote(quoteExtractor.extract(query, candidate.getContent()))
                .referenceIndex(candidate.getReferenceIndex())
                .semanticScore(candidate.getSemanticScore())
                .lexicalScore(candidate.getLexicalScore())
                .fusionScore(candidate.getFusionScore())
                .matchedChannels(candidate.getMatchedChannels())
                .build();
    }

    /**
     * 将章节证据转换为旧版章节检索结果。
     */
    private SearchResultDTO.SectionDTO toLegacySection(PaperEvidence evidence) {
        return SearchResultDTO.SectionDTO.builder()
                .id(evidence.getSectionId())
                .paperId(evidence.getPaperId())
                .header(evidence.getHeadingPath())
                .content(evidence.getQuote())
                .score(evidence.getFusionScore())
                .sourcePaperTitle(evidence.getPaperTitle())
                .build();
    }

    /**
     * 将符号证据转换为旧版符号检索结果。
     */
    private SearchResultDTO.SymbolDTO toLegacySymbol(PaperEvidence evidence) {
        return SearchResultDTO.SymbolDTO.builder()
                .paperId(evidence.getPaperId())
                .symbol(evidence.getHeading())
                .description(evidence.getQuote())
                .score(evidence.getFusionScore())
                .sourcePaperTitle(evidence.getPaperTitle())
                .build();
    }

    /**
     * 将引用证据转换为旧版引用检索结果。
     */
    private SearchResultDTO.ReferenceDTO toLegacyReference(PaperEvidence evidence) {
        return SearchResultDTO.ReferenceDTO.builder()
                .paperId(evidence.getPaperId())
                .refId(evidence.getReferenceIndex())
                .title(evidence.getHeading())
                .abstractText(evidence.getQuote())
                .score(evidence.getFusionScore())
                .sourcePaperTitle(evidence.getPaperTitle())
                .build();
    }

    /**
     * 为向量相似度阈值补充默认值，并限制在系统支持的区间内。
     */
    private double normalizeThreshold(Double threshold) {
        double value = threshold == null ? DEFAULT_THRESHOLD : threshold;
        return Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, value));
    }

    /**
     * 规范检索模式；无法识别的配置直接拒绝启动，避免静默使用错误策略。
     */
    private String normalizeMode(String mode) {
        String normalized = mode == null ? "hybrid" : mode.trim().toLowerCase();
        return switch (normalized) {
            case "vector", "fulltext", "hybrid" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported retrieval mode: " + mode);
        };
    }
}
