package cn.winddol.ai.domain.paper.model.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PaperEntity {
    private Long id;

    private String title;

    private List<OutlineNode>  outline;

    private Map<String, Object> metadata;
}
