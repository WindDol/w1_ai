package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
