package cn.winddol.ai.domain.agent.service;

import cn.winddol.ai.domain.agent.event.ResearchEvent;

public interface ResearchProcessListener {
    void onStep(ResearchEvent event);
}
