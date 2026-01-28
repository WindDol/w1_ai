package cn.winddol.ai.domain.paper.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SymbolDefinition {
    private String symbol;
    private String description;
    private String latex;
    private Set<String> scopes = new HashSet<>();
    private String definitionFormula;
    private boolean isGlobal = false;
}