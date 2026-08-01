package cn.winddol.ai.agent.librarian.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperAuditResult {
    private String type;
    private String reason;
    private Double confidence;

    /** 模型只能引用请求中提供的 evidenceKey，服务层会过滤不存在的键。 */
    @Builder.Default
    private List<String> supportingEvidenceKeys = List.of();

    @Builder.Default
    private List<String> conflictingEvidenceKeys = List.of();
}
