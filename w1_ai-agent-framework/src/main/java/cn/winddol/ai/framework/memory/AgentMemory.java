package cn.winddol.ai.framework.memory;

import dev.langchain4j.data.message.ChatMessage;
import java.util.List;

public interface AgentMemory {

    void addMessage(ChatMessage message);

    List<ChatMessage> getMessages();

    void clear();

    String getSessionId();
}
