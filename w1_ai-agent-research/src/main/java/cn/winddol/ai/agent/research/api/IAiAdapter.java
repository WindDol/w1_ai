package cn.winddol.ai.agent.research.api;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface IAiAdapter {
    String rewriteQueryIfNecessary(String currentQuestion, List<ChatMessage> history);
}
