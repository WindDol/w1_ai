package cn.winddol.ai.paper.internal;

import java.util.List;

public record HeadingDecision(
        HeadingCandidate candidate,
        CandidateKind kind,
        DecisionAction action,
        int targetLevel,
        double confidence,
        String outputHeading,
        String outputText,
        List<String> reasons
) {
    public HeadingDecision {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
