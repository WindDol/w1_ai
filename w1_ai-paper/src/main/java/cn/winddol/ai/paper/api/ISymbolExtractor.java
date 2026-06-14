package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.RefMetadata;
import cn.winddol.ai.paper.domain.SymbolDefinition;

import java.util.List;

public interface ISymbolExtractor {
    List<SymbolDefinition> extractFromSection(String paperTitle,String content);

    RefMetadata extractRefMetadata(String rawReference);
    // 通过前后文生成摘要
    String summarizeReferenceContext(String refIndex, List<String> snippets);

    String fuseSyntheticAbstracts(String oldAbs, String newAbs);
}
