package cn.winddol.ai.paper.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelationEntity {

    private Long relatedId;
    private String relatedTitle;
    private String type;
    private String description;
    private String direction;
}
