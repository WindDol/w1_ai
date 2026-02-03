package cn.winddol.ai.domain.paperTools.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class SectionEntity {
    private String id;
    private Long paperId;
    private String header;
    private String parentId;
    private String content;
    private Integer idx;
    private Object embedding;
}
