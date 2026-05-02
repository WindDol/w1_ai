package cn.winddol.ai.agent.writer.internal;

import cn.winddol.ai.agent.writer.api.IPaperWriterOrchestrator;
import cn.winddol.ai.framework.event.AgentEvent;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

@Component
public class PaperWriterOrchestratorImpl implements IPaperWriterOrchestrator {
    @Override
    public String writePaper(String topic, Consumer<AgentEvent> eventSink) {
        throw new UnsupportedOperationException("Paper writer orchestrator not yet implemented");
    }
}
