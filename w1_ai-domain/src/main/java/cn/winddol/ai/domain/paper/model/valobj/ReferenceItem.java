package cn.winddol.ai.domain.paper.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReferenceItem {
    private String refId;      // "11", "23"
    private String rawText;    // 原始引文文本
    private String title;      // 提取出的标题 (可能为空，后续用 API 补全)
}
