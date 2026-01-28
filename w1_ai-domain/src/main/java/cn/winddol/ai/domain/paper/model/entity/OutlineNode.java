package cn.winddol.ai.domain.paper.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class OutlineNode {
    private String id;        // UUID
    private String title;     // 标题
    private Integer level;    // 层级 (1, 2, 3)
    @Builder.Default
    private List<OutlineNode> children = new ArrayList<>();
}