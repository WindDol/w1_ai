package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperRelationDTO {
    private Long relatedId;
    private String relatedTitle;
    private String type;
    private String direction;
    private String description;
}
