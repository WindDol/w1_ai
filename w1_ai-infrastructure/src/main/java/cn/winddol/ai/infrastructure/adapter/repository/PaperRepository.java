package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.SearchResultDTO;
import cn.winddol.ai.paper.domain.*;
import cn.winddol.ai.paper.domain.PaperVO;
import cn.winddol.ai.paper.domain.ReferenceEnum;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.domain.SymbolDefinition;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.infrastructure.dao.*;
import cn.winddol.ai.infrastructure.dao.impl.ReferenceSeriveceImpl;
import cn.winddol.ai.infrastructure.dao.impl.SectionReferenceLinkService;
import cn.winddol.ai.infrastructure.dao.impl.SymbolServiceImpl;
import cn.winddol.ai.infrastructure.dao.po.*;
import cn.winddol.ai.infrastructure.parser.ReferenceParser;
import cn.winddol.ai.infrastructure.utils.TreeBuilderUtil;
import cn.winddol.ai.shared.exception.AppException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.model.embedding.EmbeddingModel;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Repository
public class PaperRepository implements IPaperRepository {
    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private SectionMapper sectionMapper;
    @Resource
    private SymbolMapper symbolMapper;
    @Resource
    private ReferenceParser referenceParser;

    @Resource
    private SymbolServiceImpl symbolService;

    @Resource
    private ReferenceSeriveceImpl referenceSerivece;
    @Resource
    private ReferenceMapper referenceMapper;
    @Resource
    private SectionReferenceLinkService sectionReferenceLinkService;
    @Resource
    private SectionReferenceLinkMapper sectionReferenceLinkMapper;
    @Resource
    private EmbeddingModel embeddingModel;
    @Resource
    private GlobalReferenceMapper globalReferenceMapper;
    @Resource
    private PaperKnowledgeRelationMapper relationMapper;
    @Resource
    private SectionChunkMapper sectionChunkMapper;


    @Override
    @Transactional
    public Long saveFullPaper(String title, List<SectionPO> sectionPOs, String fingerprint, String abstractText, Integer year) {
        Paper existingPaper = paperMapper.selectOne(
                new LambdaQueryWrapper<Paper>().eq(Paper::getFingerprint, fingerprint)
        );
        if (existingPaper != null) {
            log.warn("⚠️ Paper already exists in library: {}", title);
            throw new AppException("1001","当前已存在该篇论文");
        }
        // 1. 构建并保存 Paper 主表
        Paper paper = getPaper(title, sectionPOs);
        paper.setAbstractText(abstractText);
        paper.setYears(year);
        paper.setFingerprint(fingerprint);
        paper.setStatus("PARSING");
        // 插入数据库，插入后 paper.id 会自动被回填
        paperMapper.insert(paper);
        Long newPaperId = paper.getId();

        // 2. 批量构建并保存 Section 内容表
        Long paperId = paper.getId();
        insertSections(paperId, sectionPOs);
        List<GlobalReference> danglingNodes = globalReferenceMapper.selectList(
                new LambdaQueryWrapper<GlobalReference>()
                        .eq(GlobalReference::getFingerprint, fingerprint)
                        .isNull(GlobalReference::getLinkedPaperId) // 只有还没连上的才需要连
        );
        for (GlobalReference node : danglingNodes) {
            node.setLinkedPaperId(newPaperId);
            // 如果这个节点之前是 Contextual (合成) 摘要，现在既然有了正文
            // 甚至可以用论文 A 的真实 Abstract 覆盖掉那个合成摘要（知识进化）
            globalReferenceMapper.updateById(node);
            log.info("🔗 Retroactive Link: Reference node [{}] now linked to Paper ID: {}",
                    node.getId(), newPaperId);
        }
        return paperId;
    }

    @Override
    @Transactional
    public void replaceFullPaper(Long paperId, String title, List<SectionPO> sectionPOs,
                                 String fingerprint, String abstractText, Integer year) {
        if (paperMapper.selectById(paperId) == null) {
            throw new AppException("PAPER_NOT_FOUND", "Paper not found: " + paperId);
        }
        resetDerivedDataFrom(paperId, PaperIngestStage.SYMBOL_ENRICHMENT);
        sectionMapper.delete(new LambdaQueryWrapper<Section>().eq(Section::getPaperId, paperId));

        Paper paper = getPaper(title, sectionPOs);
        paper.setId(paperId);
        paper.setAbstractText(abstractText);
        paper.setYears(year);
        paper.setFingerprint(fingerprint);
        paper.setEmbedding(null);
        paper.setStatus("PARSING");
        paper.setStatusMessage(null);
        paperMapper.updateById(paper);
        paperMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Paper>()
                .eq(Paper::getId, paperId)
                .set(Paper::getEmbedding, null)
                .set(Paper::getStatusMessage, null));
        insertSections(paperId, sectionPOs);
    }

    @Override
    public void updatePaperEmbedding(Long paperId, float[] embedding) {
        Paper paper = new Paper();
        paper.setId(paperId);
        paper.setEmbedding(embedding);
        paperMapper.updateById(paper);
    }

    /**
     * 按重跑起始阶段清理派生数据；从 EMBEDDING 重跑时同时删除旧 Chunk 索引。
     */
    @Override
    @Transactional
    public void resetDerivedDataFrom(Long paperId, PaperIngestStage stage) {
        if (stage.isBeforeOrEqual(PaperIngestStage.SYMBOL_ENRICHMENT)) {
            sectionReferenceLinkMapper.delete(new LambdaQueryWrapper<SectionReferenceLink>()
                    .eq(SectionReferenceLink::getPaperId, paperId));
            referenceMapper.delete(new LambdaQueryWrapper<Reference>()
                    .eq(Reference::getPaperId, paperId));
            symbolMapper.delete(new LambdaQueryWrapper<Symbol>()
                    .eq(Symbol::getPaperId, paperId));
        } else if (stage == PaperIngestStage.CITATION_ENRICHMENT) {
            referenceMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Reference>()
                    .eq(Reference::getPaperId, paperId)
                    .set(Reference::getTitle, null)
                    .set(Reference::getPaperAbstract, null)
                    .set(Reference::getSourceType, null)
                    .set(Reference::getGlobalRefId, null));
        }

        if (stage.isBeforeOrEqual(PaperIngestStage.EMBEDDING)) {
            sectionChunkMapper.delete(new LambdaQueryWrapper<SectionChunkPO>()
                    .eq(SectionChunkPO::getPaperId, paperId));
            paperMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Paper>()
                    .eq(Paper::getId, paperId)
                    .set(Paper::getEmbedding, null));
            sectionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Section>()
                    .eq(Section::getPaperId, paperId)
                    .set(Section::getEmbedding, null));
            symbolMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Symbol>()
                    .eq(Symbol::getPaperId, paperId)
                    .set(Symbol::getEmbedding, null));
        }

        if (stage.isBeforeOrEqual(PaperIngestStage.LIBRARIAN_AUDIT)) {
            relationMapper.delete(new LambdaQueryWrapper<PaperKnowledgeRelation>()
                    .eq(PaperKnowledgeRelation::getSourcePaperId, paperId));
        }
    }

    @Override
    public List<SectionEntity> getSectionByUuid(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        List<Section> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<Section>()
                        .in(Section::getId, uuids)
                        .orderByAsc(Section::getIdx)
        );

        return sections.stream().map(po -> {
            SectionEntity entity = new SectionEntity();
            entity.setPaperId(po.getPaperId());
            entity.setHeader(po.getHeader());
            entity.setContent(po.getContent());
            entity.setEmbedding(po.getEmbedding());
            entity.setIdx(po.getIdx());
            entity.setParentId(po.getParentId());
            entity.setId(po.getId());
            return entity;
        }).toList();
    }

    @Override
    public PaperEntity getPaperById(Long id) {
        if(id == null){
            return null;
        }
        Paper paper = paperMapper.selectById(id);

        return PaperEntity.builder()
                .id(id)
                .outline(paper.getOutline())
                .title(paper.getTitle())
                .build();
    }
    @Override
    public PaperEntity getPaperDetailsById(Long paperId) {
        if(paperId == null){
            return null;
        }
        Paper paper = paperMapper.selectById(paperId);

        return PaperEntity.builder()
                .id(paperId)
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .build();
    }

    @Transactional
    @Override
    public void saveEnrichmentData(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection, Map<String, Set<String>> citationLinks) {
        List<Symbol> symbols = finalSymbols.stream().map(s -> Symbol.builder()
                .paperId(paperId)
                .symbol(s.getSymbol())
                .latex(s.getLatex())
                .description(s.getDescription())
                .definitionFormula(s.getDefinitionFormula())
                .sourceIds(s.getScopes().toArray(new String[0]))
                .isGlobal(s.isGlobal()).build()).toList();

        symbolService.saveBatch(symbols);
        Set<String> validRefIds = new HashSet<>(); //去重
        if(refSection != null){
            List<ReferenceItem> references = referenceParser.parse(paperId,refSection.getContent());
            if (references != null && !references.isEmpty()) {
                List<Reference> referenceList = references.stream()
                        .filter(distinctByKey(ReferenceItem::getRefId)) // 自定义去重
                        .map(r -> {
                            validRefIds.add(r.getRefId());
                            return Reference.builder()
                                    .paperId(paperId)
                                    .refIndex(r.getRefId()) // 这里可能是 "1" 也可能是 "Altafini 2013"
                                    .rawText(r.getRawText())
                                    .build();
                        }).toList();
                referenceSerivece.saveBatch(referenceList);
            }
            log.info("Extracted {} references.", references.size());
        }

        if (citationLinks != null && !citationLinks.isEmpty()) {
            List<SectionReferenceLink> links = new ArrayList<>();
            citationLinks.forEach((sectionId, refIndices) -> {
                for (String index : refIndices) {
                    if (validRefIds.contains(index) || isNumeric(index)) {
                        links.add(SectionReferenceLink.builder()
                                .paperId(paperId)
                                .sectionId(sectionId)
                                .refIndex(index)
                                .build());
                    }
                }
            });
            if (!links.isEmpty()) {
                sectionReferenceLinkService.saveBatch(links);
            }
        }
    }
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    // 辅助：判断是否纯数字 (兼容旧逻辑)
    private boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    @Override
    public List<SearchResultDTO.SectionDTO> searchSectionsByVector(Long paperId, String vector, int topK) {
        return  sectionMapper.searchByVector(paperId,vector, topK);
    }

    @Override
    public List<SearchResultDTO.SymbolDTO> searchSymbolsByVector(Long paperId, String vector, int topK) {
        return  symbolMapper.searchByVector(paperId,vector, topK);
    }
    @Override
    public List<SearchResultDTO.ReferenceDTO> searchReferencesByVector(Long paperId, String vector, int topK) {
        return referenceMapper.searchReferencesByVector(paperId,vector,topK);
    }

    @Override
    public GlobalReferenceEntity selectGlobalReferenceByS2Id(String s2Id) {
        if (StringUtils.isBlank(s2Id)) return null;

        GlobalReference po = globalReferenceMapper.selectOne(
                new LambdaQueryWrapper<GlobalReference>().eq(GlobalReference::getS2Id, s2Id)
        );
        return GRPoToEntity(po);
    }

    @Override
    public GlobalReferenceEntity selectGlobalReferenceByFingerprint(String fingerprint) {
        if (StringUtils.isBlank(fingerprint)) return null;

        GlobalReference po = globalReferenceMapper.selectOne(
                new LambdaQueryWrapper<GlobalReference>().eq(GlobalReference::getFingerprint, fingerprint)
        );
        return GRPoToEntity(po);
    }

    @Override
    @Transactional
    public Long insertGlobalReference(GlobalReferenceEntity entity) {
        GlobalReference po = GREntityToPo(entity);
        String textToEmbed = po.getTitle() + "\n" + po.getAbstractText();
        float[] vector = embeddingModel.embed(textToEmbed).content().vector();
        po.setEmbedding(vector);
        globalReferenceMapper.insert(po);
        // 回填自增 ID 到领域实体
        return po.getId();
    }

    @Override
    @Transactional
    public Long updateGlobalReference(GlobalReferenceEntity entity) {
        if (entity.getId() == null) return null;
        GlobalReference po = GREntityToPo(entity);
        String textToEmbed = po.getTitle() + "\n" + po.getAbstractText();
        po.setEmbedding(embeddingModel.embed(textToEmbed).content().vector());
        globalReferenceMapper.updateById(po);
        return po.getId();
    }

    @Override
    public Long findPaperIdByFingerprint(String fingerprint) {
        if (StringUtils.isBlank(fingerprint)) return null;

        // 假设 papers 表中已经增加了 fingerprint 字段
        // SELECT id FROM papers WHERE fingerprint = ? LIMIT 1
        Paper paper = paperMapper.selectOne(
                new LambdaQueryWrapper<Paper>()
                        .select(Paper::getId) // 只查 ID，性能更好
                        .eq(Paper::getFingerprint, fingerprint)
                        .last("LIMIT 1")
        );

        return paper != null ? paper.getId() : null;
    }

    @Override
    public ReferenceItem lookupReference(Long paperId, String refIndex) {
        String cleanIndex = refIndex.replaceAll("[\\[\\]]", "").trim();
        Long globalId = referenceMapper.selectGlobalRefId(paperId, cleanIndex);

        if (globalId == null) {
            return null;
        }
        GlobalReference po = globalReferenceMapper.selectGlobalById(globalId);

        if (po == null) {
            return null;
        }
        return ReferenceItem.builder()
                .refId(refIndex)
                .paperId(paperId)
                .globalRefId(po.getId())
                .title(po.getTitle())
                .paperAbstract(po.getAbstractText())
                .linkedPaperId(po.getLinkedPaperId())
                .sourceType(ReferenceEnum.getCode(po.getSourceType()))
                .build();
    }

    @Override
    public List<ReferenceItem> selectPendingReferencesByPaperId(Long paperId) {
        List<Reference> referenceList = referenceMapper.selectList(
                new LambdaQueryWrapper<Reference>()
                        .eq(Reference::getPaperId, paperId)
                        .isNull(Reference::getTitle)
                        .last("LIMIT 50") // 每次只处理 50 条，防止超时
        );
        return referenceList.stream().map(s-> ReferenceItem.builder()
                .id(s.getId())
                .paperId(s.getPaperId())
                .refId(s.getRefIndex())
                .rawText(s.getRawText())
                .build()).toList();
    }

    @Override
    public List<ReferenceItem> selectReferencesByPaperId(Long paperId) {
        List<Reference> referenceList = referenceMapper.selectList(
                new LambdaQueryWrapper<Reference>()
                        .eq(Reference::getPaperId, paperId)
        );
        return referenceList.stream().map(s-> ReferenceItem.builder()
                .id(s.getId())
                .paperId(s.getPaperId())
                .refId(s.getRefIndex())
                .rawText(s.getRawText())
                .build()).toList();
    }

    @Override
    public void updateStatus(Long paperId, String status) {
        Paper po = new Paper();
        po.setId(paperId);
        po.setStatus(status);
        paperMapper.updateById(po);
    }

    @Override
    public void updateStatusWithError(Long paperId, String status, String errorMessage) {
        Paper po = new Paper();
        po.setId(paperId);
        po.setStatus(status);
        po.setStatusMessage(errorMessage); // 记录错误详情
        paperMapper.updateById(po);
    }

    @Override
    public List<KnowledgeRelationEntity> findRelationsByPaperId(Long paperId) {
        List<Map<String, Object>> rawData = relationMapper.selectBidirectionalRelations(paperId);
        return rawData.stream().map(map -> KnowledgeRelationEntity.builder()
                .relatedId((Long) map.get("related_paper_id"))
                .relatedTitle((String) map.get("related_paper_title"))
                .type((String) map.get("relation_type"))
                .description((String) map.get("description"))
                .direction((String) map.get("direction"))
                .confidence(numberToDouble(map.get("confidence")))
                .auditStatus((String) map.get("audit_status"))
                .supportingEvidence((String) map.get("supporting_evidence"))
                .conflictingEvidence((String) map.get("conflicting_evidence"))
                .auditVersion((String) map.get("audit_version"))
                .modelName((String) map.get("model_name"))
                .promptVersion((String) map.get("prompt_version"))
                .retrievalVersion((String) map.get("retrieval_version"))
                .build()).toList();
    }

    private Double numberToDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    @Override
    public List<PaperEntity> searchPapers(String query, Double threshold) {
        double defaultThreshold = 0.5;
        if (threshold != null){
            defaultThreshold = threshold;
        }
        // 1. 将查询语句向量化
        float[] queryVector = embeddingModel.embed(query).content().vector();
        String vectorStr = Arrays.toString(queryVector);

        return paperMapper.searchPapers(query, vectorStr, defaultThreshold, 5);
    }

    @Override
    public List<GlobalReferenceEntity> getTopFrequentReferences(Integer limit) {
        if (limit == null) limit = 5;
        List<GlobalReference> globalReferenceEntities = globalReferenceMapper.getTopFrequentReferences(limit);
        return globalReferenceEntities.stream().map(this::GRPoToEntity).toList();
    }

    @Override
    public List<PaperVO> listAllPapers() {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                Paper::getId,
                Paper::getTitle,
                Paper::getFingerprint,
                Paper::getStatus,
                Paper::getCreatedAt
        );
        wrapper.orderByDesc(Paper::getCreatedAt);
        List<Paper> paperPOs = paperMapper.selectList(wrapper);
        return paperPOs.stream().map(s-> PaperVO.builder().id(s.getId())
                .title(s.getTitle())
                .status(s.getStatus())
                .fingerprint(s.getFingerprint())
                .createdAt(s.getCreatedAt()).build()).toList();
    }



    private GlobalReferenceEntity GRPoToEntity(GlobalReference po) {
        if (po == null) return null;
        return GlobalReferenceEntity.builder()
                .id(po.getId())
                .s2Id(po.getS2Id())
                .fingerprint(po.getFingerprint())
                .title(po.getTitle())
                .abstractText(po.getAbstractText())
                .sourceType(ReferenceEnum.getCode(po.getSourceType()))
                .linkedPaperId(po.getLinkedPaperId())
                .citationCount(po.getCitationCount())
                .build();
    }

    private GlobalReference GREntityToPo(GlobalReferenceEntity entity) {
        if (entity == null) return null;
        GlobalReference po = new GlobalReference();
        po.setId(entity.getId());
        po.setS2Id(entity.getS2Id());
        po.setFingerprint(entity.getFingerprint());
        po.setTitle(entity.getTitle());
        po.setCitationCount(entity.getCitationCount()+1);
        po.setAbstractText(entity.getAbstractText());
        po.setSourceType(entity.getSourceType().getCode());
        po.setLinkedPaperId(entity.getLinkedPaperId());
        return po;
    }

    @Override
    public PaperEntity selectPaperById(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);

        return PaperEntity.builder().id(paperId).title(paper.getTitle())
                .outline(paper.getOutline()).build() ;
    }

    @Override
    public SectionEntity selectSectionById(String sectionUuid) {
        Section section = sectionMapper.selectById(sectionUuid);
        return SectionEntity.builder()
                .id(sectionUuid).paperId(section.getPaperId())
                .header(section.getHeader()).parentId(section.getParentId())
                .content(section.getContent()).idx(section.getIdx()).build();
    }

    /**
     * 查询当前章节的局部符号及其所属论文的全局符号，禁止混入其他论文的全局符号。
     */
    @Override
    public List<SymbolEntity> selectSymbolsByUuids(String sectionUuid) {
        Section section = sectionMapper.selectById(sectionUuid);
        if (section == null || section.getPaperId() == null) {
            return List.of();
        }
        List<Symbol> symbols = symbolMapper.selectList(new QueryWrapper<Symbol>()
                .eq("paper_id", section.getPaperId())
                .and(wrapper -> wrapper
                        .apply("source_ids @> ARRAY[{0}]::text[]", sectionUuid)
                        .or()
                        .eq("is_global", true)
                ));
        if(symbols != null && !symbols.isEmpty()) {
            return symbols.stream().map(s -> SymbolEntity.builder()
                    .id(s.getId()).paperId(s.getPaperId()).symbol(s.getSymbol())
                    .latex(s.getLatex()).description(s.getDescription())
                    .definitionFormula(s.getDefinitionFormula()).isGlobal(s.getIsGlobal())
                    .sourceIds(s.getSourceIds()).build()).toList();
        }
        return List.of();
    }

    /**
     * 查询指定论文的完整符号表，包括该论文的全局符号和各章节局部符号。
     */
    @Override
    public List<SymbolEntity> findByPaperId(Long paperId) {
        List<Symbol> symbols = symbolMapper.selectList(new QueryWrapper<Symbol>()
                .eq("paper_id", paperId));
        if(symbols != null && !symbols.isEmpty()) {
            return symbols.stream().map(s -> SymbolEntity.builder()
                    .id(s.getId()).paperId(s.getPaperId()).symbol(s.getSymbol())
                    .latex(s.getLatex())
                    .description(s.getDescription())
                    .definitionFormula(s.getDefinitionFormula())
                    .build()).toList();
        }
        return List.of();
    }

    @Override
    public SectionEntity getSectionSibling(Long paperId, int idx, int offset) {
        Section section = sectionMapper.selectOne(new QueryWrapper<Section>()
                .eq("paper_id", paperId)
                .eq("idx", idx + offset) // 假设你有 idx 字段记录顺序
                .last("LIMIT 1"));
        if(section == null){
            return  null;
        }
        return SectionEntity.builder().header(section.getHeader()).id(section.getId()).build();
    }

    @Override
    public List<SearchResultDTO.SymbolDTO> searchSymbolsByKeyword(String query, Long paperId) {
        List<Symbol> entities = symbolMapper.selectList(
                new LambdaQueryWrapper<Symbol>()
                        .eq(Symbol::getPaperId,paperId)
                        .eq(Symbol::getSymbol, query) // 精确匹配 symbol 字段
        );
        return entities.stream().map(e -> {
            SearchResultDTO.SymbolDTO dto = new SearchResultDTO.SymbolDTO();
            dto.setSymbol(e.getSymbol());
            dto.setDescription(e.getDescription());
            dto.setDefinitionFormula(e.getDefinitionFormula());
            dto.setScore(1.0); // 精确匹配给满分
            return dto;
        }).toList();
    }

    @Override
    public List<ReferenceItem> selectReferences() {
        List<Reference> referenceList = referenceMapper.selectList(
                new LambdaQueryWrapper<Reference>()
                        .isNull(Reference::getTitle)
                        .last("LIMIT 50") // 每次只处理 50 条，防止超时
        );
        return referenceList.stream().map(s-> ReferenceItem.builder()
                .id(s.getId())
                .paperId(s.getPaperId())
                .refId(s.getRefIndex())
                .rawText(s.getRawText())
                .build()).toList();
    }

    @Override
    public void updateReferences(ReferenceItem ref) {
        if (ref.getId() == null) return;

        // 方案 A：直接更新原始对象（如果你的 ReferenceItem 也是个实体）
        // 方案 B：如果必须转，手动赋值确认
        Reference po = new Reference();
        po.setId(ref.getId());
        po.setTitle(ref.getTitle());
        po.setPaperAbstract(ref.getPaperAbstract());
        po.setGlobalRefId(ref.getGlobalRefId());
        // 防御性编程：如果没有数据，不执行更新
        if (po.getTitle() == null && po.getPaperAbstract() == null) {
            return;
        }
        if(ref.getSourceType()!= null){
            po.setSourceType(ref.getSourceType().getSourceType());
        }

        referenceMapper.updateById(po);
    }

    @Override
    public List<SectionReferenceLinkEntity> selectReferenceLinks(Long paperId, String refId) {
        List<SectionReferenceLink> links = sectionReferenceLinkMapper.selectList(
                new LambdaQueryWrapper<SectionReferenceLink>()
                        .eq(SectionReferenceLink::getPaperId, paperId)
                        .eq(SectionReferenceLink::getRefIndex, refId)
        );

        return links.stream().map(s -> SectionReferenceLinkEntity.builder().sectionId(s.getSectionId()).build()).toList();
    }

    @Override
    public List<SectionReferenceLinkEntity> selectLinksBySectionId(String sectionId) {
        List<SectionReferenceLink> links = sectionReferenceLinkMapper.selectList(
                new LambdaQueryWrapper<SectionReferenceLink>()
                        .eq(SectionReferenceLink::getSectionId, sectionId)
        );
        return links.stream().map(s -> SectionReferenceLinkEntity.builder().paperId(s.getPaperId())
                .refIndex(s.getRefIndex())
                .build()).toList();
    }

    @Override
    public ReferenceItem selectReferenceByIndex(Long paperId, String refIndex) {
        return referenceMapper.selectJoinedReference(paperId, refIndex);
    }

    private static @NonNull Paper getPaper(String title, List<SectionPO> sectionPOs) {
        Paper paper = new Paper();
        paper.setTitle(title);

        // 生成 Outline Map (Header -> UUID)
        List<OutlineNode> treeOutline = TreeBuilderUtil.buildTree(sectionPOs);
        paper.setOutline(treeOutline);
        return paper;
    }

    private void insertSections(Long paperId, List<SectionPO> sectionPOs) {
        int index = 0;
        for (SectionPO po : sectionPOs) {
            Section section = new Section();
            section.setId(po.uuid);
            section.setPaperId(paperId);
            section.setHeader(po.header);
            section.setContent(po.getContent());
            section.setIdx(index++);
            section.setTokenCount(po.getContent() == null ? 0 : po.getContent().length());
            section.setParentId(po.getParentId());
            sectionMapper.insert(section);
        }
    }
}
