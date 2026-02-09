package cn.winddol.ai.domain.agent.service.bussiness;

import cn.winddol.ai.domain.agent.event.ResearchEvent;

public interface ResearchProcessListener {
    void onStep(ResearchEvent event);
}
