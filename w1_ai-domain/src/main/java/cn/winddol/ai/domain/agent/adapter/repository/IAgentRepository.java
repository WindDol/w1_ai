package cn.winddol.ai.domain.agent.adapter.repository;

import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.domain.agent.model.entity.KnowledgeRelationEntity;
import cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity;

import java.util.List;

public interface IAgentRepository {
    void logStep(String sessionId, int step, AgentStep thoughtAction, String observation);
    void saveRelation(Long sourceId, Long targetId, String type, String reason);
    List<KnowledgeRelationEntity> findRelationsByPaperId(Long paperId);

    AgentPaperEntity getPaperById(Long newPaperId);

    List<AgentPaperEntity> searchSimilarPapers(float[] vector, int i, Long newPaperId);
}
