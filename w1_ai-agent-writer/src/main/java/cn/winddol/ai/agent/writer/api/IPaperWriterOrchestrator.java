package cn.winddol.ai.agent.writer.api;

import cn.winddol.ai.framework.event.AgentEvent;
import java.util.function.Consumer;

public interface IPaperWriterOrchestrator {
    String writePaper(String topic, Consumer<AgentEvent> eventSink);
}
