package cn.winddol.ai.framework.event;

@FunctionalInterface
public interface AgentEventListener {
    void onEvent(AgentEvent event);
}
