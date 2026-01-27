package cn.winddol.ai.domain.paper.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class SymbolDefinition {
    private String symbol;
    private String description;
    private String latex;
    private Set<String> scopes;
    private String definitionFormula;
    private boolean isGlobal = false;
}