package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paperTools.model.entity.*;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceEnum;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;
import cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition;
import cn.winddol.ai.infrastructure.dao.*;
import cn.winddol.ai.infrastructure.dao.impl.ReferenceSeriveceImpl;
import cn.winddol.ai.infrastructure.dao.impl.SectionReferenceLinkService;
import cn.winddol.ai.infrastructure.dao.impl.SymbolServiceImpl;
import cn.winddol.ai.infrastructure.dao.po.*;
import cn.winddol.ai.infrastructure.parser.ReferenceParser;
import cn.winddol.ai.infrastructure.utils.TreeBuilderUtil;
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


    @Override
    @Transactional
    public void saveFullPaper(String title, List<SectionPO> sectionPOs, String fingerprint) {
        Paper existingPaper = paperMapper.selectOne(
                new LambdaQueryWrapper<Paper>().eq(Paper::getFingerprint, fingerprint)
        );
        if (existingPaper != null) {
            log.warn("⚠️ Paper already exists in library: {}", title);
            return; // 或者返回已有的 ID
        }
        // 1. 构建并保存 Paper 主表
        Paper paper = getPaper(title, sectionPOs);
        paper.setFingerprint(fingerprint);
        // 插入数据库，插入后 paper.id 会自动被回填
        paperMapper.insert(paper);
        Long newPaperId = paper.getId();

        // 2. 批量构建并保存 Section 内容表
        Long paperId = paper.getId();
        int index = 0;
        for (SectionPO po : sectionPOs) {
            Section section = new Section();
            section.setId(po.uuid); // 使用解析时生成的 UUID
            section.setPaperId(paperId);
            section.setHeader(po.header);
            section.setContent(po.getContent());
            section.setIdx(index++);
            section.setTokenCount(po.getContent().length()); // 简单估算，后面用分词器精修
            section.setParentId(po.getParentId());
            sectionMapper.insert(section);
        }
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

        if(refSection != null){
            List<ReferenceItem> references = referenceParser.parse(paperId,refSection.getContent());
            if (references != null && !references.isEmpty()) {
                List<Reference> referenceList = references.stream().map(r -> Reference.builder()
                        .paperId(paperId).refIndex(r.getRefId()).rawText(r.getRawText()).build()).toList();
                referenceSerivece.saveBatch(referenceList);
            }
            log.info("Extracted {} references.", references.size());
        }

        if (citationLinks != null && !citationLinks.isEmpty()) {
            List<SectionReferenceLink> links = new ArrayList<>();
            citationLinks.forEach((sectionId, refIndices) -> {
                for (String index : refIndices) {
                    links.add(SectionReferenceLink.builder()
                            .paperId(paperId)
                            .sectionId(sectionId)
                            .refIndex(index)
                            .build());
                }
            });
            sectionReferenceLinkService.saveBatch(links);
        }
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
        String cleanIndex = refIndex.replaceAll("[\\[\\]\\s]", "");
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

    @Override
    public List<SymbolEntity> selectSymbolsByUuids(String sectionUuid) {
        List<Symbol> symbols = symbolMapper.selectList(new QueryWrapper<Symbol>()
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
}
