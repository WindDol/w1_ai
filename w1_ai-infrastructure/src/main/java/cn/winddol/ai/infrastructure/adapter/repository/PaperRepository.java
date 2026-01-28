package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.domain.paper.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paper.model.entity.OutlineNode;
import cn.winddol.ai.domain.paper.model.entity.PaperEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionPO;
import cn.winddol.ai.domain.paper.model.valobj.ReferenceItem;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;
import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.ReferenceMapper;
import cn.winddol.ai.infrastructure.dao.SectionMapper;
import cn.winddol.ai.infrastructure.dao.SymbolMapper;
import cn.winddol.ai.infrastructure.dao.impl.ReferenceSeriveceImpl;
import cn.winddol.ai.infrastructure.dao.impl.SymbolService;
import cn.winddol.ai.infrastructure.dao.impl.SymbolServiceImpl;
import cn.winddol.ai.infrastructure.dao.po.Paper;
import cn.winddol.ai.infrastructure.dao.po.Reference;
import cn.winddol.ai.infrastructure.dao.po.Section;
import cn.winddol.ai.infrastructure.dao.po.Symbol;
import cn.winddol.ai.infrastructure.parser.ReferenceParser;
import cn.winddol.ai.infrastructure.utils.TreeBuilderUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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


    private static @NonNull Paper getPaper(String title, List<SectionPO> sectionPOs) {
        Paper paper = new Paper();
        paper.setTitle(title);

        // 生成 Outline Map (Header -> UUID)
        List<OutlineNode> treeOutline = TreeBuilderUtil.buildTree(sectionPOs);
        paper.setOutline(treeOutline);
        return paper;
    }
}
