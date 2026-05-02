package cn.winddol.ai.framework.tool;

import cn.winddol.ai.shared.model.tool.ToolResult;
import java.util.List;

public interface ToolProvider {

    List<Tool> getTools();

    ToolResult executeTool(String toolName, String inputJson);
}
