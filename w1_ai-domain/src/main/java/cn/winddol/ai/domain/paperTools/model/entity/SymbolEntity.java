package cn.winddol.ai.domain.paperTools.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class SymbolEntity {
    private Long id;
    private Long paperId;
    private String symbol;
    private String latex;
    private String description;
    private String definitionFormula;
    private Boolean isGlobal;
    private String[] sourceIds;
    private float[] embedding;
}
