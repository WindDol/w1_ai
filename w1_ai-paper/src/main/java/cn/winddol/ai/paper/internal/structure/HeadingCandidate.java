package cn.winddol.ai.paper.internal.structure;

public record HeadingCandidate(
        int lineNumber,
        String rawLine,
        String text,
        boolean markdownHeading,
        int sourceLevel,
        boolean blankBefore,
        boolean blankAfter,
        boolean afterReferences,
        CandidateKind kind,
        String marker,
        String remainder
) {
}
