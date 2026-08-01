package cn.winddol.ai.agent.librarian.domain;

import cn.winddol.ai.shared.model.tool.ToolEvidence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一条可落库的论文关系审计记录，包含模型结论及其引用的证据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationAuditRecord {

    private Long sourcePaperId;
    private Long targetPaperId;
    private String relationType;
    private String description;
    private Double confidence;
    private RelationAuditStatus auditStatus;
    private String auditVersion;
    private String modelName;
    private String promptVersion;
    private String retrievalVersion;

    @Builder.Default
    private List<ToolEvidence> supportingEvidence = List.of();

    @Builder.Default
    private List<ToolEvidence> conflictingEvidence = List.of();
}
