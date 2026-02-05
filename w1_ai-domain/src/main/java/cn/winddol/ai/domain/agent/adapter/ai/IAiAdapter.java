package cn.winddol.ai.domain.agent.adapter.ai;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface IAiAdapter {
    String rewriteQueryIfNecessary(String userQuestion, List<ChatMessage> messages);
}
