package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.domain.agent.adapter.repository.IAgentRepository;
import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.infrastructure.dao.AgentThoughtTraceMapper;
import cn.winddol.ai.infrastructure.dao.po.AgentThoughtTrace;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class AgentRepository implements IAgentRepository {
    @Resource
    private AgentThoughtTraceMapper traceMapper;
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
}
