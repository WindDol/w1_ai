package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.parser.MarkdownParser;
import cn.winddol.ai.paper.internal.PaperStructureNormalizer;
import cn.winddol.ai.paper.model.entity.SectionPO;
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
        for (String sample : List.of("Mobius1", "Mobius2", "Mobius33", "1-s2.0-S2405896322006656-main")) {
            Path markdown = resolveSample(sample);
            String normalized = normalizer.normalize(Files.readString(markdown));
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
}
