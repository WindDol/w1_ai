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
    /**
     * 所属论文ID
     */
    private Long paperId;

    /**
     * 引用发生的章节 UUID
     */
    private String sectionId;

    /**
     * 引用文献的索引标号 (如 "24", "1", "12")
     * 对应 paper_references 表中的 ref_index
     */
    private String refIndex;
}
