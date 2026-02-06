package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.domain.agent.adapter.repository.IAgentRepository;
import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.domain.agent.model.entity.KnowledgeRelationEntity;
import cn.winddol.ai.domain.agent.model.entity.PaperEntity;
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
        List<Map<String, Object>> rawData = relationMapper.selectRelationsWithTitle(paperId);
        return rawData.stream().map(map -> KnowledgeRelationEntity.builder()
                .targetId((Long) map.get("target_paper_id"))
                .targetTitle((String) map.get("target_paper_title"))
                .type((String) map.get("relation_type"))
                .description((String) map.get("description"))
                .build()).toList();
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
    public List<PaperEntity> searchSimilarPapers(float[] vector, int i, Long newPaperId) {
        String vectorStr = Arrays.toString(vector);

        // 2. 执行数据库查询
        List<Map<String, Object>> results = paperMapper.searchSimilar(vectorStr, i, newPaperId);

        // 3. 将 Map 转换为 Entity
        return results.stream().map(map -> {
            PaperEntity entity = new PaperEntity();
            entity.setId((Long) map.get("id"));
            entity.setTitle((String) map.get("title"));
            return entity;
        }).collect(Collectors.toList());
    }
}
