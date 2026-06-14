package cn.winddol.ai.paper.internal;

import java.util.List;

public record PaperStructureNormalizationResult(
        String normalizedMarkdown,
        List<HeadingDecision> decisions,
        List<String> warnings,
        double outlineConfidence
) {
    public PaperStructureNormalizationResult {
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
