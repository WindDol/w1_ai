package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalReferenceEntity {
    private Long id;
    private String s2Id;
    private String fingerprint;
    private String title;
    private String abstractText;
    private String sourceType;
    private Integer citationCount;
    private Long linkedPaperId;
}
