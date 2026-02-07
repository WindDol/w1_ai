package cn.winddol.ai.domain.agent.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResearchEvent {
    private String sessionId;
    private String type;      // THOUGHT (思考), ACTION (行动), OBSERVATION (观察), ANSWER (最终答案), ERROR
    private String content;   // 内容文本
    private Object data;      // 附加数据 (如 Tool 的输入输出 JSON)
    private Integer step;
}