package cn.winddol.ai.agent.research.api;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.List;

public interface IResearchAgent {

    String doResearch(String sessionId, String taskDescription, ChatMemory memory,
                      ResearchEventListener listener);
}
