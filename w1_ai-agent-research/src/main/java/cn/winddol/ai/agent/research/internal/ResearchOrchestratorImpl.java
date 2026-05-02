package cn.winddol.ai.agent.research.internal;

import cn.winddol.ai.agent.research.api.*;
import cn.winddol.ai.framework.event.AgentEvent;
import cn.winddol.ai.framework.memory.AgentMemoryFactory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Consumer;

@Service
@Slf4j
public class ResearchOrchestratorImpl implements IResearchOrchestrator {

    private final ISupervisor supervisor;
    private final IResearchAgent researchAgent;
    private final AgentMemoryFactory memoryFactory;
    private final Consumer<AgentEvent> eventSink;
    private final ISessionLockService sessionLockService;

    public ResearchOrchestratorImpl(ISupervisor supervisor,
                                     IResearchAgent researchAgent,
                                     AgentMemoryFactory memoryFactory,
                                     Consumer<AgentEvent> eventSink,
                                     ISessionLockService sessionLockService) {
        this.supervisor = supervisor;
        this.researchAgent = researchAgent;
        this.memoryFactory = memoryFactory;
        this.eventSink = eventSink;
        this.sessionLockService = sessionLockService;
    }

    @Override
    public String startResearch(String sessionId, String userQuestion) {
        if (sessionLockService.tryLock(sessionId, Duration.ofMinutes(10))) {
            try {
                log.info("🚀 Starting new research agent for session: {}", sessionId);
                ChatMemory memory = memoryFactory.create(sessionId, 20).asChatMemory();
                memory.add(UserMessage.from(userQuestion));

                String taskDescription = supervisor.rewriteAndTranslate(userQuestion, memory.messages());
                log.info("📝 Original: '{}' -> Rewritten: '{}'", userQuestion, taskDescription);

                ResearchEventListener listener = event -> eventSink.accept(event);
                String result = researchAgent.doResearch(sessionId, taskDescription, memory, listener);
                return result;
            } finally {
                sessionLockService.unlock(sessionId);
                log.info("🏁 Agent finished and lock released for session: {}", sessionId);
            }
        } else {
            log.warn("⚠️ Session {} is already running.", sessionId);
        }
        return null;
    }
}
