package cn.winddol.ai.framework.orchestrator;

import cn.winddol.ai.framework.event.AgentEvent;
import java.util.function.Consumer;

public interface AgentOrchestrator {

    String execute(String sessionId, String userQuestion, Consumer<AgentEvent> eventSink);
}
