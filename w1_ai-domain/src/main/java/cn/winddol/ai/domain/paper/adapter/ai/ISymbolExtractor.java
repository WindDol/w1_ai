package cn.winddol.ai.domain.paper.adapter.ai;

import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;

import java.util.List;

public interface ISymbolExtractor {
    public List<SymbolDefinition> extractFromSection(String paperTitle,String content);
}
