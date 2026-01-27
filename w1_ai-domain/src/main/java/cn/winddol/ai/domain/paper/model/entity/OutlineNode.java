package cn.winddol.ai.domain.paper.model.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OutlineNode {
    private String id;        // UUID
    private String title;     // 标题
    private Integer level;    // 层级 (1, 2, 3)
    private List<OutlineNode> children = new ArrayList<>();

    public OutlineNode(String id, String title, Integer level) {
        this.id = id;
        this.title = title;
        this.level = level;
    }
}