package cn.winddol.ai.domain.agent.adapter.repository;

import cn.winddol.ai.domain.agent.model.entity.AgentStep;

public interface IAgentRepository {
    void logStep(String sessionId, int step, AgentStep thoughtAction, String observation);
}
