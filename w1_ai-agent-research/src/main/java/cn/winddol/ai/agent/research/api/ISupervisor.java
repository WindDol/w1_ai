package cn.winddol.ai.agent.research.api;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface ISupervisor {

    String rewriteAndTranslate(String question, List<ChatMessage> history);
}
