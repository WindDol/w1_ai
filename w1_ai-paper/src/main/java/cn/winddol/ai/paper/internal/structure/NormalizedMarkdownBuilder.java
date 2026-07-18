package cn.winddol.ai.paper.internal.structure;

import java.util.List;

class NormalizedMarkdownBuilder {

    String build(List<HeadingDecision> decisions) {
        StringBuilder markdown = new StringBuilder();
        for (HeadingDecision decision : decisions) {
            switch (decision.action()) {
                case DROP -> {
                }
                case EMIT_TEXT -> appendLine(markdown, decision.outputText());
                case EMIT_HEADING -> appendLine(markdown,
                        "#".repeat(Math.max(1, decision.targetLevel())) + " " + decision.outputHeading());
                case SPLIT_HEADING_BODY -> {
                    appendLine(markdown, "#".repeat(Math.max(1, decision.targetLevel())) + " " + decision.outputHeading());
                    if (decision.outputText() != null && !decision.outputText().isBlank()) {
                        appendLine(markdown, decision.outputText());
                    }
                }
            }
        }
        return markdown.toString().trim() + "\n";
    }

    private void appendLine(StringBuilder sb, String line) {
        sb.append(line == null ? "" : line).append('\n');
    }
}
