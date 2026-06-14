package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.parser.MarkdownParser;
import cn.winddol.ai.paper.internal.CandidateKind;
import cn.winddol.ai.paper.internal.DecisionAction;
import cn.winddol.ai.paper.internal.PaperStructureNormalizationResult;
import cn.winddol.ai.paper.internal.PaperStructureNormalizer;
import cn.winddol.ai.paper.domain.SectionPO;
import cn.winddol.ai.infrastructure.parser.PaperCleaner;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaperStructureNormalizerTest {

    private final PaperStructureNormalizer normalizer = new PaperStructureNormalizer();
    private final MarkdownParser markdownParser = new MarkdownParser();

    @Test
    public void normalizeMonkeyOcrSamplesForOutlineParsing() throws Exception {
        for (String sample : List.of(
                "Mobius1", "Mobius2", "Mobius33", "1-s2.0-S2405896322006656-main",
                "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"
        )) {
            PaperStructureNormalizationResult report = normalizer.normalizeWithReport(Files.readString(resolveSample(sample)));
            String normalized = report.normalizedMarkdown();
            List<SectionPO> sections = markdownParser.parse(normalized);

            assertFalse(normalized.startsWith("# PAPER"), sample);
            assertFalse(normalized.contains("# You may also like"), sample);
            assertTrue(sections.size() >= 4, sample);
            assertTrue(hasHeader(sections, "introduction"), sample);
            assertTrue(hasHeader(sections, "references"), sample);
            assertTrue(sections.stream().anyMatch(section -> section.getLevel() >= 2), sample);
        }
    }

    @Test
    public void promotesBareNumberedSectionAndKeepsTheoremLikeTextOutOfOutline() {
        String markdown = """
                # A Good Paper Title

                ABSTRACT. This paper studies a useful problem.

                # 1. INTRODUCTION

                Intro text.

                3. CHARACTERIZATION OF EQUILIBRIUM SETS

                Definition 3. This is not an outline heading.

                # REFERENCES
                [1] Someone A 2024 Title.
                """;

        String normalized = normalizer.normalize(markdown);
        List<SectionPO> sections = markdownParser.parse(normalized);

        assertTrue(normalized.contains("## Abstract"));
        assertTrue(normalized.contains("## 3. CHARACTERIZATION OF EQUILIBRIUM SETS"));
        assertFalse(normalized.contains("# Definition 3."));
        assertTrue(hasHeader(sections, "characterization of equilibrium sets"));
    }

    @Test
    public void keepsNumberedAlgorithmStepsOutOfOutline() throws Exception {
        PaperStructureNormalizationResult report = normalizer.normalizeWithReport(Files.readString(resolveSample("Mobius2")));
        String normalized = report.normalizedMarkdown();
        List<SectionPO> sections = markdownParser.parse(normalized);

        assertHeader(sections, "1. introduction");
        assertHeader(sections, "2. the wrapped cauchy family on the circle");
        assertHeader(sections, "3. conformally natural family");
        assertHeader(sections, "3.1. random variate generation in the hyperbolic disc");
        assertHeader(sections, "3.2. the special case");
        assertHeader(sections, "4. relations with mathematical physics");
        assertHeader(sections, "5. significance for ml and bioinformatics");
        assertHeader(sections, "6. conclusion");
        assertHeader(sections, "references");

        assertTrue(normalized.contains("1. Sample two random numbers"));
        assertTrue(normalized.contains("2. Set $\\psi"));
        assertFalse(hasHeader(sections, "sample two random numbers"));
        assertFalse(hasHeader(sections, "set $\\psi"));
        assertDecision(report, "Sample two random numbers", CandidateKind.LIST_ITEM, DecisionAction.EMIT_TEXT);
        assertDecision(report, "RELATIONS WITH MATHEMATICAL PHYSICS", CandidateKind.SECTION, DecisionAction.EMIT_HEADING);
        assertAuditableDowngrades(report);
    }

    @Test
    public void keepsStructuralHeadersWithJournalLikeWords() {
        assertFalse(PaperCleaner.isNoise("## 4. RELATIONS WITH MATHEMATICAL PHYSICS"));
        assertFalse(PaperCleaner.isNoise("### 3.1. Science-aware representation learning"));
        assertFalse(PaperCleaner.isNoise("## References"));

        List<SectionPO> sections = markdownParser.parse("""
                # Demo Paper
                ## 4. RELATIONS WITH MATHEMATICAL PHYSICS
                Body.
                """);
        assertHeader(sections, "4. relations with mathematical physics");
    }

    @Test
    public void nestsArabicSubsectionsUnderLetterSections() throws Exception {
        PaperStructureNormalizationResult report = normalizer.normalizeWithReport(Files.readString(resolveSample("Mobius33")));
        String normalized = report.normalizedMarkdown();
        List<SectionPO> sections = markdownParser.parse(normalized);

        assertFalse(normalized.contains("# AFFILIATIONS"));
        assertHeader(sections, "i. introduction");
        assertHeader(sections, "ii. preliminaries");
        assertHeader(sections, "c. hyperbolic geometry and möbius transformations");
        assertHeader(sections, "1. möbius transformations in higher dimensions");
        assertHeader(sections, "2. infinitesimal generators");
        assertHeader(sections, "iii. reduced equations");
        assertHeader(sections, "ix. summary and discussion");
        assertHeader(sections, "acknowledgments");
        assertHeader(sections, "data availability");
        assertHeader(sections, "references");
        assertHeaderLevel(sections, "c. hyperbolic geometry and möbius transformations", 3);
        assertHeaderLevel(sections, "1. möbius transformations in higher dimensions", 4);
        assertHeaderLevel(sections, "2. infinitesimal generators", 4);
        assertHeaderLevel(sections, "c. analysis of dynamics", 3);

        assertTrue(normalized.contains("#### 1. Möbius transformations in higher dimensions"));
        assertTrue(normalized.contains("#### 2. Infinitesimal generators"));
        assertDecision(report, "AFFILIATIONS", CandidateKind.NOISE, DecisionAction.DROP);
        assertAuditableDowngrades(report);
    }

    @Test
    public void normalizesSecondBatchOutlineFailureModes() throws Exception {

        List<SectionPO> sample14 = parseSample("14");
        assertHeaderLevel(sample14, "low dimensional behavior of large systems of globally coupled oscillators", 1);
        assertNoHeader(sample14, "articles you may be interested in");
        assertHeaderLevel(sample14, "e. the millennium bridge problem", 3);
        assertHeaderLevel(sample14, "references", 2);

        List<SectionPO> sample15 = parseSample("15");
        assertHeaderLevel(sample15, "pseudospectral approximation of eigenvalues", 1);
        assertHeaderLevel(sample15, "1. introduction", 2);
        assertHeaderLevel(sample15, "references", 2);

        List<SectionPO> sample16 = parseSample("16");
        assertHeaderLevel(sample16, "mutual entrainment of two limit cycle oscillators", 1);
        assertHeaderLevel(sample16, "1. introduction", 2);
        assertHeaderLevel(sample16, "2. the solution", 2);
        assertHeaderLevel(sample16, "3. discussion of the numerical results", 2);
        assertHeaderLevel(sample16, "4. summary", 2);
        assertHeaderLevel(sample16, "references", 2);
    }

    private Path resolveSample(String sample) {
        Path rootPath = Path.of("docs", sample, sample + ".md");
        if (Files.exists(rootPath)) {
            return rootPath;
        }
        Path appModulePath = Path.of("..", "docs", sample, sample + ".md");
        if (Files.exists(appModulePath)) {
            return appModulePath;
        }
        throw new IllegalStateException("Sample markdown not found for " + sample);
    }

    private boolean hasHeader(List<SectionPO> sections, String expected) {
        return sections.stream()
                .map(SectionPO::getHeader)
                .filter(header -> header != null)
                .map(String::toLowerCase)
                .anyMatch(header -> header.contains(expected));
    }

    private void assertHeader(List<SectionPO> sections, String expected) {
        assertTrue(hasHeader(sections, expected), expected);
    }

    private List<SectionPO> parseSample(String sample) throws Exception {
        String normalized = normalizer.normalize(Files.readString(resolveSample(sample)));
        return markdownParser.parse(normalized);
    }

    private void assertHeaderLevel(List<SectionPO> sections, String expected, int level) {
        boolean found = sections.stream()
                .filter(section -> section.getHeader() != null)
                .anyMatch(section -> section.getHeader().toLowerCase().contains(expected)
                        && section.getLevel() == level);
        assertTrue(found, expected + " level " + level);
    }

    private void assertNoHeader(List<SectionPO> sections, String unexpected) {
        assertFalse(hasHeader(sections, unexpected), unexpected);
    }

    private void assertHeaderCount(List<SectionPO> sections, String expected, int count) {
        long actual = sections.stream()
                .map(SectionPO::getHeader)
                .filter(header -> header != null)
                .map(String::toLowerCase)
                .filter(header -> header.contains(expected))
                .count();
        assertTrue(actual == count, expected + " count " + count + " actual " + actual);
    }

    private void assertParentHeader(List<SectionPO> sections, String childExpected, String parentExpected) {
        SectionPO child = findHeader(sections, childExpected);
        SectionPO parent = findHeader(sections, parentExpected);
        assertTrue(child != null, childExpected);
        assertTrue(parent != null, parentExpected);
        assertTrue(parent.getUuid().equals(child.getParentId()), childExpected + " parent " + parentExpected);
    }

    private SectionPO findHeader(List<SectionPO> sections, String expected) {
        return sections.stream()
                .filter(section -> section.getHeader() != null)
                .filter(section -> section.getHeader().toLowerCase().contains(expected))
                .findFirst()
                .orElse(null);
    }

    private void assertDecision(PaperStructureNormalizationResult report,
                                String text,
                                CandidateKind kind,
                                DecisionAction action) {
        boolean found = report.decisions().stream()
                .filter(decision -> decision.candidate().text().contains(text))
                .anyMatch(decision -> decision.kind() == kind
                        && decision.action() == action
                        && !decision.reasons().isEmpty());
        assertTrue(found, text);
    }

    private void assertAuditableDowngrades(PaperStructureNormalizationResult report) {
        boolean missingReason = report.decisions().stream()
                .filter(decision -> decision.action() == DecisionAction.DROP
                        || decision.kind() == CandidateKind.LIST_ITEM
                        || decision.kind() == CandidateKind.THEOREM_LIKE
                        || decision.kind() == CandidateKind.REFERENCE_ITEM
                        || decision.kind() == CandidateKind.UNCERTAIN)
                .anyMatch(decision -> decision.reasons().isEmpty());
        assertFalse(missingReason);
    }
}
