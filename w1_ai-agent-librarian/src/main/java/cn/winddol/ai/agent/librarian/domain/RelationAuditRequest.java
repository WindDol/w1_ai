package cn.winddol.ai.agent.librarian.domain;

import cn.winddol.ai.shared.model.tool.ToolEvidence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提供给关系判定模型的输入：候选论文元数据与两侧真实检索证据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationAuditRequest {

    private AgentPaperEntity sourcePaper;
    private AgentPaperEntity targetPaper;
    private String auditVersion;
    private String promptVersion;
    private String sourceRetrievalVersion;
    private String targetRetrievalVersion;

    @Builder.Default
    private List<ToolEvidence> sourceEvidence = List.of();

    @Builder.Default
    private List<ToolEvidence> targetEvidence = List.of();
}
