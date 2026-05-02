package cn.winddol.ai.framework.agent;

import cn.winddol.ai.framework.tool.ToolProvider;

public abstract class Agent {

    protected final String name;
    protected final ToolProvider toolProvider;

    protected Agent(String name, ToolProvider toolProvider) {
        this.name = name;
        this.toolProvider = toolProvider;
    }

    public String getName() {
        return name;
    }

    public abstract String getSystemPrompt();
}
