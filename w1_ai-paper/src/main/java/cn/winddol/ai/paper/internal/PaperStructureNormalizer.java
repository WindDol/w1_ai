package cn.winddol.ai.paper.internal;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PaperStructureNormalizer {

    private final HeadingCandidateExtractor candidateExtractor;
    private final HeadingRuleScorer ruleScorer;
    private final NormalizedMarkdownBuilder markdownBuilder;

    public PaperStructureNormalizer() {
        this(new HeadingCandidateExtractor(), new HeadingRuleScorer(), new NormalizedMarkdownBuilder());
    }

    PaperStructureNormalizer(HeadingCandidateExtractor candidateExtractor,
                             HeadingRuleScorer ruleScorer,
                             NormalizedMarkdownBuilder markdownBuilder) {
        this.candidateExtractor = candidateExtractor;
        this.ruleScorer = ruleScorer;
        this.markdownBuilder = markdownBuilder;
    }

    public String normalize(String markdown) {
        return normalizeWithReport(markdown).normalizedMarkdown();
    }

    public PaperStructureNormalizationResult normalizeWithReport(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new PaperStructureNormalizationResult(markdown, List.of(), List.of(), 0.0);
        }

        List<HeadingCandidate> candidates = candidateExtractor.extract(markdown);
        List<HeadingDecision> decisions = ruleScorer.score(candidates);
        String normalizedMarkdown = markdownBuilder.build(decisions);
        List<String> warnings = buildWarnings(decisions);
        double confidence = calculateOutlineConfidence(decisions);
        return new PaperStructureNormalizationResult(normalizedMarkdown, decisions, warnings, confidence);
    }

    private List<String> buildWarnings(List<HeadingDecision> decisions) {
        List<String> warnings = new ArrayList<>();
        boolean hasTitle = decisions.stream()
                .anyMatch(decision -> decision.kind() == CandidateKind.TITLE
                        && decision.action() == DecisionAction.EMIT_HEADING);
        boolean hasReferences = decisions.stream()
                .anyMatch(decision -> decision.kind() == CandidateKind.REFERENCES
                        && decision.action() == DecisionAction.EMIT_HEADING);
        if (!hasTitle) {
            warnings.add("No confident title heading was detected.");
        }
        if (!hasReferences) {
            warnings.add("No references heading was detected.");
        }
        return warnings;
    }

    private double calculateOutlineConfidence(List<HeadingDecision> decisions) {
        List<HeadingDecision> headingDecisions = decisions.stream()
                .filter(decision -> decision.action() == DecisionAction.EMIT_HEADING
                        || decision.action() == DecisionAction.SPLIT_HEADING_BODY)
                .toList();
        if (headingDecisions.isEmpty()) {
            return 0.0;
        }
        double total = headingDecisions.stream().mapToDouble(HeadingDecision::confidence).sum();
        return total / headingDecisions.size();
    }
}
