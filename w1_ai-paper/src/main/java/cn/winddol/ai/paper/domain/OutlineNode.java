package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutlineNode {
    private String id;
    private String title;
    private Integer level;
    @Builder.Default
    private List<OutlineNode> children = new ArrayList<>();
}
