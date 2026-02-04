package cn.winddol.ai.domain.paperTools.model.entity;

import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class GlobalReferenceEntity {
    private Long id;
    private String s2Id;
    private String fingerprint;
    private String title;
    private String abstractText;
    private ReferenceEnum sourceType;
    private Integer citationCount;
    private Long linkedPaperId;
}
