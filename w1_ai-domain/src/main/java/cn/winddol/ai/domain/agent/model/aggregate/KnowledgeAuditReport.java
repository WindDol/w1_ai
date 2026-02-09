package cn.winddol.ai.domain.agent.model.aggregate;

import cn.winddol.ai.domain.agent.model.entity.ConflictPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class KnowledgeAuditReport {
    private int qualityScore;      // 0-100 质量分
    private String noveltySummary; // 创新性概述
    private List<ConflictPoint> conflicts; // 冲突点列表
    private String suggestions;    // 对研究员的建议

}