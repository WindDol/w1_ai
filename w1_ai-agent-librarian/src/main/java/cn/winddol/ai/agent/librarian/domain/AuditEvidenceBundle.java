package cn.winddol.ai.agent.librarian.domain;

import cn.winddol.ai.shared.model.tool.ToolEvidence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一次关系审计中，从单篇论文检索到的真实证据及其检索版本。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvidenceBundle {

    private String retrievalVersion;

    @Builder.Default
    private List<ToolEvidence> evidence = List.of();
}
