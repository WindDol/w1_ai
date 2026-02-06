package cn.winddol.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelationEntity {

    private Long relatedId;      // 对方论文ID
    private String relatedTitle; // 对方论文标题
    private String type;         // EXTEND, SUPPORT, etc.
    private String description;  // 评价内容
    private String direction;    // OUTGOING (当前论文评价别人) / INCOMING (别人评价当前论文)

}
