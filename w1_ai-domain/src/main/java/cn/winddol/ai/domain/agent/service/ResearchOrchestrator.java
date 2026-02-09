package cn.winddol.ai.domain.agent.service;

import cn.winddol.ai.domain.agent.adapter.event.NotificationService;
import cn.winddol.ai.domain.agent.adapter.redis.ISessionLockService;
import cn.winddol.ai.domain.agent.service.bussiness.AgentSupervisor;
import cn.winddol.ai.domain.agent.service.bussiness.ResearchAgent;
import cn.winddol.ai.domain.agent.service.bussiness.ResearchProcessListener;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class ResearchOrchestrator implements IResearchOrchestrator{
    @Resource
    private AgentSupervisor supervisor;
    @Resource
    private ResearchAgent researchAgent;
    @Resource
    private ChatMemoryStore chatMemoryStore;
    @Resource
    private NotificationService notificationService;
    @Resource
    private ISessionLockService sessionLockService;

    public String startResearch(String sessionId, String userQuestion) {
        if (sessionLockService.tryLock(sessionId, Duration.ofMinutes(10))) {
            try {
                log.info("🚀 Starting new research agent for session: {}", sessionId);
                ChatMemory memory = getMemory(sessionId);
                memory.add(UserMessage.from(userQuestion));
                // 1. 导师进行预处理（重写、指代消解、翻译）
                String taskDescription = supervisor.rewriteAndTranslate(userQuestion, memory.messages());
                log.info("📝 Original: '{}' -> Rewritten: '{}'", userQuestion, taskDescription);
                // 2. 研究员进入实验室（ReAct 循环，带 Trace 记录）
                ResearchProcessListener listener = event -> {
                    // 可以在这里做 DTO 转换，将领域事件转为前端需要的格式
                    notificationService.send(event.getSessionId(), event);
                };
                String result = researchAgent.doResearch(sessionId, taskDescription, memory, listener);
                return result;
            }finally {
                sessionLockService.unlock(sessionId);
                log.info("🏁 Agent finished and lock released for session: {}", sessionId);
            }
        } else {
            log.warn("⚠️ Session {} is already running. Attaching to existing process.", sessionId);
        }
        return null;

    }
    private ChatMemory getMemory(String sessionId) {
        return MessageWindowChatMemory.builder()
                .id(sessionId)              // 关键：ID 对应 Redis 中的 Key
                .maxMessages(20)            // 保留最近 20 条消息
                .chatMemoryStore(chatMemoryStore) // 关键：数据持久化到 Redis
                .build();
    }
}