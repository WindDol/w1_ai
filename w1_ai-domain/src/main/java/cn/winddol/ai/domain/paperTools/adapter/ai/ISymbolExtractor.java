package cn.winddol.ai.domain.paperTools.adapter.ai;

import cn.winddol.ai.domain.paperTools.model.entity.RefMetadata;
import cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition;

import java.util.List;

public interface ISymbolExtractor {
    List<SymbolDefinition> extractFromSection(String paperTitle,String content);

    RefMetadata extractRefMetadata(String rawReference);
    // 通过前后文生成摘要
    String summarizeReferenceContext(String refIndex, List<String> snippets);

    String fuseSyntheticAbstracts(String oldAbs, String newAbs);
}
