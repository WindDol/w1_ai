package cn.winddol.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class ConflictPoint {
    private String targetPaperTitle;
    private String type; // SUPPORT / CONTRADICT / EXTEND
    private String description;
}
