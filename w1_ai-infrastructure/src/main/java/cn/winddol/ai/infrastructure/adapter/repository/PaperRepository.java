package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.domain.paper.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paper.model.entity.OutlineNode;
import cn.winddol.ai.domain.paper.model.entity.PaperEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionPO;
import cn.winddol.ai.domain.paper.model.valobj.ReferenceItem;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;
import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.SectionMapper;
import cn.winddol.ai.infrastructure.dao.po.Paper;
import cn.winddol.ai.infrastructure.dao.po.Section;
import cn.winddol.ai.infrastructure.parser.ReferenceParser;
import cn.winddol.ai.infrastructure.utils.TreeBuilderUtil;
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
        List<Section> sections = sectionMapper.selectBatchIds(uuids);

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
    public void updatePaperMetadata(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection) {
        // 1. 先查询出当前的 PO 对象
        // 注意：这里引用的是 infrastructure 层的 PO
        Paper po = paperMapper.selectById(paperId);
        Map<String, Object> metadata = po.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("symbols", finalSymbols);
        if(refSection != null){
            List<ReferenceItem> references = referenceParser.parse(refSection.getContent());
            if (references != null && !references.isEmpty()) {
                metadata.put("references", references);
            }
            log.info("Extracted {} references.", references.size());
        }
        // 4. 回填修改后的 metadata
        po.setMetadata(metadata);

        // 5. 执行数据库更新
        paperMapper.updateById(po);

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
