package cn.winddol.ai.domain.paper.model.entity;

import lombok.Data;

@Data
public class SectionEntity {
    private String id;
    private String header;

    private String content;

    private Object embedding;

    private Integer idx;

    private String parentId;

    private Long paperId;
}
