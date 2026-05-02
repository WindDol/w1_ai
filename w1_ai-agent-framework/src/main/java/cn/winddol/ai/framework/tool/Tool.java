package cn.winddol.ai.framework.tool;

import cn.winddol.ai.shared.model.tool.ToolResult;

public interface Tool {

    String name();

    String description();

    String parametersSchema();

    ToolResult execute(String inputJson);
}
