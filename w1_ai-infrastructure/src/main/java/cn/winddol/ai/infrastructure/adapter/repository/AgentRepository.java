package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.domain.agent.adapter.repository.IAgentRepository;
import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.domain.agent.model.entity.KnowledgeRelationEntity;
import cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity;
import cn.winddol.ai.infrastructure.dao.AgentThoughtTraceMapper;
import cn.winddol.ai.infrastructure.dao.PaperKnowledgeRelationMapper;
import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.po.AgentThoughtTrace;
import cn.winddol.ai.infrastructure.dao.po.Paper;
import cn.winddol.ai.infrastructure.dao.po.PaperKnowledgeRelation;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class AgentRepository implements IAgentRepository {
    @Resource
    private AgentThoughtTraceMapper traceMapper;
    @Resource
    private PaperKnowledgeRelationMapper relationMapper;
    @Resource
    private PaperMapper paperMapper;
    @Override
    public void logStep(String sessionId, int step, AgentStep thoughtAction, String observation) {
        AgentThoughtTrace po = new AgentThoughtTrace();
        po.setSessionId(sessionId);
        po.setStepNumber(step);
        po.setThought(thoughtAction.getThought());
        po.setAction(thoughtAction.getAction());
        po.setActionInput(thoughtAction.getActionInput());
        po.setObservation(observation);
        traceMapper.insert(po);
    }

    @Override
    public void saveRelation(Long sourceId, Long targetId, String type, String reason) {
        PaperKnowledgeRelation po = PaperKnowledgeRelation.builder()
                .sourcePaperId(sourceId)
                .targetPaperId(targetId)
                .relationType(type)
                .description(reason)
                .build();
        relationMapper.insert(po);
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
                .build()).toList();
    }

    @Override
    public AgentPaperEntity getPaperById(Long id) {
        if(id == null){
            return null;
        }
        Paper paper = paperMapper.selectById(id);

        return AgentPaperEntity.builder()
                .id(id)
                .outline(toLegacyOutline(paper.getOutline()))
                .abstractText(paper.getAbstractText())
                .embedding(paper.getEmbedding())
                .title(paper.getTitle())
                .years(paper.getYears())
                .build();
    }

    @Override
    public List<AgentPaperEntity> searchSimilarPapers(float[] vector, int i, Long newPaperId) {
        String vectorStr = Arrays.toString(vector);

        // 2. 执行数据库查询
        List<Map<String, Object>> results = paperMapper.searchSimilar(vectorStr, i, newPaperId);

        // 3. 将 Map 转换为 Entity
        return results.stream().map(map -> {
            AgentPaperEntity entity = new AgentPaperEntity();
            entity.setId((Long) map.get("id"));
            entity.setTitle((String) map.get("title"));
            entity.setAbstractText((String)map.get("abstracttext"));
            entity.setYears((Integer) map.get("years"));
            return entity;
        }).collect(Collectors.toList());
    }

    private List<cn.winddol.ai.domain.paperTools.model.entity.OutlineNode> toLegacyOutline(
            List<cn.winddol.ai.paper.domain.OutlineNode> outline) {
        if (outline == null) {
            return null;
        }
        return outline.stream().map(node -> cn.winddol.ai.domain.paperTools.model.entity.OutlineNode.builder()
                .id(node.getId())
                .title(node.getTitle())
                .level(node.getLevel())
                .children(toLegacyOutline(node.getChildren()))
                .build()).collect(Collectors.toList());
    }
}
