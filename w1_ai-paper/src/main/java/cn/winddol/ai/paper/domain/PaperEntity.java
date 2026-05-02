package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperEntity {
    private Long id;
    private String title;
    private List<OutlineNode> outline;
    private Map<String, Object> metadata;
    private String abstractText;
    private Double score;
}
