package cn.winddol.ai.domain.agent.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ResearchOrchestrator {
    @Resource
    private AgentSupervisor supervisor;
    @Resource
    private ResearchAgent researchAgent;
    @Resource
    private ChatMemoryStore chatMemoryStore;

    public String ask(String sessionId, String userQuestion) {
        ChatMemory memory = getMemory(sessionId);
        memory.add(UserMessage.from(userQuestion));
        // 1. 导师进行预处理（重写、指代消解、翻译）
        String taskDescription = supervisor.rewriteAndTranslate(userQuestion, memory.messages());
        log.info("📝 Original: '{}' -> Rewritten: '{}'", userQuestion, taskDescription);
        // 2. 研究员进入实验室（ReAct 循环，带 Trace 记录）
        String result = researchAgent.doResearch(sessionId,taskDescription,memory);

        return result;
    }
    private ChatMemory getMemory(String sessionId) {
        return MessageWindowChatMemory.builder()
                .id(sessionId)              // 关键：ID 对应 Redis 中的 Key
                .maxMessages(20)            // 保留最近 20 条消息
                .chatMemoryStore(chatMemoryStore) // 关键：数据持久化到 Redis
                .build();
    }
}