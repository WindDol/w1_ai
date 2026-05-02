package cn.winddol.ai.framework.memory;

public interface AgentMemoryFactory {

    AgentMemory create(String sessionId, int maxMessages);
}
