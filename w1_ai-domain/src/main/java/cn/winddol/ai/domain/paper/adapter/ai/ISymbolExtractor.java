package cn.winddol.ai.domain.paper.adapter.ai;

import cn.winddol.ai.domain.paper.adapter.external.dto.RefMetadata;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;

import java.util.List;

public interface ISymbolExtractor {
    List<SymbolDefinition> extractFromSection(String paperTitle,String content);

    RefMetadata extractRefMetadata(String rawReference);
    // 通过前后文生成摘要
    String summarizeReferenceContext(String refIndex, List<String> snippets);
}
