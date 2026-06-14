package cn.winddol.ai.paper.domain;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @JSONField(name = "definition_formula")
    private String definitionFormula;
    @JSONField(name = "is_global")
    private boolean isGlobal = false;
}