package cn.winddol.ai.paper.internal;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PaperStructureNormalizer {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern INLINE_ABSTRACT = Pattern.compile("^(?i)(abstract)\\s*[:.]\\s+(.+)$");
    private static final Pattern DECIMAL_SECTION = Pattern.compile("^(\\d+(?:\\.\\d+)+)\\s+(.+)$");
    private static final Pattern NUMBERED_SECTION = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
    private static final Pattern ROMAN_SECTION = Pattern.compile("^([IVXLCDM]+)\\.\\s+(.+)$");
    private static final Pattern LETTER_SECTION = Pattern.compile("^([A-Z])\\.\\s+(.+)$");
    private static final Pattern APPENDIX_SECTION = Pattern.compile("^(?i)(appendix\\s+[A-Z])\\.?\\s+(.+)$");
    private static final Pattern THEOREM_LIKE = Pattern.compile(
            "^(?i)(theorem|lemma|proposition|definition|corollary|remark|example|proof|fig\\.?|figure|table)\\b.*"
    );

    private static final Set<String> NOISE_HEADINGS = Set.of(
            "paper",
            "you may also like",
            "articles you may be interested in"
    );

    public String normalize(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }

        StringBuilder normalized = new StringBuilder();
        Set<String> emittedTitleKeys = new HashSet<>();
        String titleKey = null;
        boolean titleSeen = false;
        boolean skippingNoiseBlock = false;
        boolean inReferences = false;

        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();

            if (!titleSeen && trimmed.isBlank()) {
                continue;
            }

            Matcher headingMatcher = MARKDOWN_HEADING.matcher(trimmed);
            if (headingMatcher.matches()) {
                String headingText = cleanHeadingText(headingMatcher.group(2));
                String headingKey = key(headingText);

                if (isNoiseHeading(headingKey)) {
                    skippingNoiseBlock = true;
                    continue;
                }

                if (skippingNoiseBlock && !isPlausibleArticleHeading(headingText)) {
                    continue;
                }
                skippingNoiseBlock = false;

                if (isTheoremLike(headingText)) {
                    appendLine(normalized, headingText);
                    continue;
                }

                if (!titleSeen && isPlausibleTitle(headingText)) {
                    String title = cleanTitle(headingText);
                    titleKey = key(title);
                    emittedTitleKeys.add(titleKey);
                    titleSeen = true;
                    appendLine(normalized, "# " + title);
                    continue;
                }

                if (titleSeen && titleKey != null && titleKey.equals(key(cleanTitle(headingText))) && !isStandardSection(headingText)) {
                    if (emittedTitleKeys.contains(titleKey)) {
                        continue;
                    }
                    emittedTitleKeys.add(titleKey);
                }

                int level = normalizedLevel(headingText, true);
                if (level > 0) {
                    if (isReferencesHeading(headingText)) {
                        inReferences = true;
                    }
                    appendLine(normalized, "#".repeat(level) + " " + normalizeSectionTitle(headingText));
                } else {
                    appendLine(normalized, line);
                }
                continue;
            }

            if (skippingNoiseBlock) {
                if (isBareHeading(trimmed)) {
                    skippingNoiseBlock = false;
                } else {
                    continue;
                }
            }

            if (!titleSeen) {
                if (isFrontMatterNoise(trimmed) || !isPlausibleTitle(trimmed)) {
                    continue;
                }
                String title = cleanTitle(trimmed);
                titleKey = key(title);
                emittedTitleKeys.add(titleKey);
                titleSeen = true;
                appendLine(normalized, "# " + title);
                continue;
            }

            Matcher inlineAbstract = INLINE_ABSTRACT.matcher(trimmed);
            if (!inReferences && inlineAbstract.matches()) {
                appendLine(normalized, "## Abstract");
                appendLine(normalized, inlineAbstract.group(2).trim());
                continue;
            }

            if (!inReferences && isBareHeading(trimmed)) {
                int level = normalizedLevel(trimmed, false);
                if (level > 0) {
                    if (isReferencesHeading(trimmed)) {
                        inReferences = true;
                    }
                    appendLine(normalized, "#".repeat(level) + " " + normalizeSectionTitle(trimmed));
                    continue;
                }
            }

            if (isLineNoise(trimmed)) {
                continue;
            }

            appendLine(normalized, line);
        }

        return normalized.toString().trim() + "\n";
    }

    private int normalizedLevel(String heading, boolean fromMarkdownHeading) {
        String text = normalizeSectionTitle(heading);
        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.equals("abstract") || lower.equals("references") ||
                lower.startsWith("acknowledg") || lower.startsWith("data availability") ||
                lower.startsWith("orcid")) {
            return 2;
        }
        if (APPENDIX_SECTION.matcher(text).matches()) {
            return 2;
        }
        Matcher decimal = DECIMAL_SECTION.matcher(text);
        if (decimal.matches()) {
            int depth = decimal.group(1).split("\\.").length;
            return Math.min(depth + 1, 4);
        }
        if (NUMBERED_SECTION.matcher(text).matches()) {
            return 2;
        }
        if (ROMAN_SECTION.matcher(text).matches()) {
            return 2;
        }
        if (LETTER_SECTION.matcher(text).matches()) {
            return 3;
        }
        return fromMarkdownHeading && isStandardSection(text) ? 2 : 0;
    }

    private boolean isBareHeading(String text) {
        if (text == null || text.isBlank() || text.length() > 140 || isTheoremLike(text)) {
            return false;
        }
        return INLINE_ABSTRACT.matcher(text).matches() ||
                DECIMAL_SECTION.matcher(text).matches() ||
                NUMBERED_SECTION.matcher(text).matches() ||
                ROMAN_SECTION.matcher(text).matches() ||
                LETTER_SECTION.matcher(text).matches() ||
                APPENDIX_SECTION.matcher(text).matches() ||
                isStandardSection(text);
    }

    private boolean isPlausibleTitle(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String cleaned = cleanTitle(text);
        String k = key(cleaned);
        return cleaned.length() >= 8 &&
                !isNoiseHeading(k) &&
                !isFrontMatterNoise(cleaned) &&
                !isStandardSection(cleaned) &&
                !isBareHeading(cleaned);
    }

    private boolean isPlausibleArticleHeading(String text) {
        return isPlausibleTitle(text) || isStandardSection(text) || isBareHeading(text);
    }

    private boolean isStandardSection(String text) {
        String lower = key(text);
        return lower.equals("abstract") ||
                lower.equals("references") ||
                lower.startsWith("acknowledg") ||
                lower.startsWith("data availability") ||
                lower.startsWith("orcid") ||
                lower.startsWith("appendix");
    }

    private boolean isReferencesHeading(String text) {
        return key(text).equals("references");
    }

    private boolean isNoiseHeading(String headingKey) {
        return NOISE_HEADINGS.contains(headingKey);
    }

    private boolean isFrontMatterNoise(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.startsWith("to cite this article") ||
                lower.startsWith("view the article online") ||
                lower.startsWith("check for updates") ||
                lower.startsWith("research article |") ||
                lower.equals("chorus") ||
                lower.equals("aip publishing") ||
                lower.equals("learn more") ||
                lower.matches("\\d{1,2}\\s+[a-z]+\\s+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}");
    }

    private boolean isLineNoise(String text) {
        return isFrontMatterNoise(text);
    }

    private boolean isTheoremLike(String text) {
        return THEOREM_LIKE.matcher(text).matches();
    }

    private String cleanHeadingText(String text) {
        return text.replaceAll("\\s{2,}", " ").trim();
    }

    private String cleanTitle(String text) {
        return cleanHeadingText(text)
                .replaceAll("\\s+\\*$", "")
                .replaceAll("(?i)\\s+(FREE|EP)$", "")
                .trim();
    }

    private String normalizeSectionTitle(String text) {
        String cleaned = cleanHeadingText(text);
        if (cleaned.equalsIgnoreCase("ABSTRACT")) {
            return "Abstract";
        }
        if (cleaned.equalsIgnoreCase("REFERENCES")) {
            return "References";
        }
        return cleaned;
    }

    private String key(String text) {
        if (text == null) {
            return "";
        }
        return cleanHeadingText(text)
                .replaceAll("^#+\\s*", "")
                .replaceAll("\\s+\\*$", "")
                .replaceAll("(?i)\\s+(FREE|EP)$", "")
                .toLowerCase(Locale.ROOT);
    }

    private void appendLine(StringBuilder sb, String line) {
        sb.append(line).append('\n');
    }
}
