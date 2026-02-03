package cn.winddol.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class AgentStep {
    // 思考过程 (Chain of Thought)
    private String thought;

    // 要调用的工具名 (searchLibrary, getPaperOutline, readSection)
    private String action;

    // 工具参数 (json string 或 simple string)
    private String actionInput;

    // 最终答案 (如果有值，说明循环结束)
    private String finalAnswer;
}
