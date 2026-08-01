package cn.winddol.ai.api.dto.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperRelationDTO {
    private Long relatedId;
    private String relatedTitle;
    private String type;
    private String description;
    private String direction;
    private Double confidence;
    private String auditStatus;
    private String supportingEvidence;
    private String conflictingEvidence;
    private String auditVersion;
    private String modelName;
    private String retrievalVersion;
}
