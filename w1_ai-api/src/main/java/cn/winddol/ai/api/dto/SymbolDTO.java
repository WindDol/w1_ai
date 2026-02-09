package cn.winddol.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymbolDTO {
    private Long id;
    private Long paperId;
    private String symbol;
    private String latex;
    private String description;
    private String definitionFormula;
}
