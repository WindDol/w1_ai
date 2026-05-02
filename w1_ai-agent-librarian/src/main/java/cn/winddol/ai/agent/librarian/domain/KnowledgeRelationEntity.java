package cn.winddol.ai.agent.librarian.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeRelationEntity {
    private Long relatedId;
    private String relatedTitle;
    private String type;
    private String description;
    private String direction;
}
