package cn.winddol.ai.agent.research.api;

import cn.winddol.ai.agent.research.domain.ResearchContext;

public interface IResearchOrchestrator {
    default String startResearch(String sessionId, String userQuestion) {
        return startResearch(sessionId, userQuestion, ResearchContext.empty());
    }

    String startResearch(String sessionId, String userQuestion, ResearchContext context);
}
