package cn.winddol.ai.domain.paperTools.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class PaperEntity {
    private Long id;

    private String title;

    private List<OutlineNode>  outline;

    private Map<String, Object> metadata;
    private String abstractText;
    private Double score;
}
