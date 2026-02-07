package cn.winddol.ai.domain.paperTools.model.valobj;

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
    private String refId;      // "11", "23"
    private String rawText;    // 原始引文文本
    private String title;      // 提取出的标题 (可能为空，后续用 API 补全)
    private String paperAbstract;
    private ReferenceEnum sourceType;
    private Long globalRefId;
    private Long linkedPaperId;
    private Integer citationCount;
}
