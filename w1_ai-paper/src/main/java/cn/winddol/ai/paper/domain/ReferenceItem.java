package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReferenceItem {
    private Long id;
    private Long paperId;
    private String refId;
    private String rawText;
    private String title;
    private String paperAbstract;
    private String sourceType;
    private Long globalRefId;
    private Long linkedPaperId;
    private Integer citationCount;
}
