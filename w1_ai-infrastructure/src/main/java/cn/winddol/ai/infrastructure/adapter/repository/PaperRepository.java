package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.domain.paper.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paper.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paper.model.entity.*;
import cn.winddol.ai.domain.paper.model.valobj.ReferenceItem;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;
import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.SectionMapper;
import cn.winddol.ai.infrastructure.dao.SymbolMapper;
import cn.winddol.ai.infrastructure.dao.impl.ReferenceSeriveceImpl;
import cn.winddol.ai.infrastructure.dao.impl.SymbolServiceImpl;
import cn.winddol.ai.infrastructure.dao.po.Paper;
import cn.winddol.ai.infrastructure.dao.po.Reference;
import cn.winddol.ai.infrastructure.dao.po.Section;
import cn.winddol.ai.infrastructure.dao.po.Symbol;
import cn.winddol.ai.infrastructure.parser.ReferenceParser;
import cn.winddol.ai.infrastructure.utils.TreeBuilderUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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


    @Override
    @Transactional
    public void saveFullPaper(String title, List<SectionPO> sectionPOs) {
        // 1. 构建并保存 Paper 主表
        Paper paper = getPaper(title, sectionPOs);

        // 插入数据库，插入后 paper.id 会自动被回填
        paperMapper.insert(paper);

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
    public void saveEnrichmentData(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection) {
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
            List<ReferenceItem> references = referenceParser.parse(refSection.getContent());
            if (references != null && !references.isEmpty()) {
                List<Reference> referenceList = references.stream().map(r -> Reference.builder()
                        .paperId(paperId).refIndex(r.getRefId()).rawText(r.getRawText()).title(r.getTitle()).build()).toList();
                referenceSerivece.saveBatch(referenceList);
            }
            log.info("Extracted {} references.", references.size());
        }
    }

    @Override
    public List<SearchResultDTO.SectionDTO> searchSectionsByVector(String vector, int topK) {
        List<Section> sections = sectionMapper.searchByVector(vector, topK);
        if(sections != null && !sections.isEmpty()){
            return sections.stream().map(s ->
                    SearchResultDTO.SectionDTO.builder()
                            .id(s.getId()).paperId(s.getPaperId()).header(s.getHeader())
                            .parentId(s.getParentId()).idx(s.getIdx()).score(s.getScore()).build()
            ).toList();
        }
        return List.of();
    }

    @Override
    public List<SearchResultDTO.SymbolDTO> searchSymbolsByVector(String vector, int topK) {
        List<Symbol> symbols = symbolMapper.searchByVector(vector, topK);
        if(symbols != null && !symbols.isEmpty()){
            return symbols.stream().map(s ->
                    SearchResultDTO.SymbolDTO.builder()
                            .id(s.getId()).paperId(s.getPaperId()).symbol(s.getSymbol())
                            .description(s.getDescription()).definitionFormula(s.getDefinitionFormula())
                            .score(s.getScore()).build()
            ).toList();
        }
        return List.of();
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
    public List<SearchResultDTO.SymbolDTO> searchSymbolsByKeyword(String query) {
        List<Symbol> entities = symbolMapper.selectList(
                new LambdaQueryWrapper<Symbol>()
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


    private static @NonNull Paper getPaper(String title, List<SectionPO> sectionPOs) {
        Paper paper = new Paper();
        paper.setTitle(title);

        // 生成 Outline Map (Header -> UUID)
        List<OutlineNode> treeOutline = TreeBuilderUtil.buildTree(sectionPOs);
        paper.setOutline(treeOutline);
        return paper;
    }
}
