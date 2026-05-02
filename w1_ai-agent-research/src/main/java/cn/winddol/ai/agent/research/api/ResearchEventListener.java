package cn.winddol.ai.agent.research.api;

import cn.winddol.ai.framework.event.AgentEvent;

public interface ResearchEventListener {
    void onStep(AgentEvent event);
}
