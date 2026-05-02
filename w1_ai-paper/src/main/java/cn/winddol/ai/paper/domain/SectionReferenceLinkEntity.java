package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionReferenceLinkEntity {
    private Long paperId;
    private String sectionId;
    private String refIndex;
}
