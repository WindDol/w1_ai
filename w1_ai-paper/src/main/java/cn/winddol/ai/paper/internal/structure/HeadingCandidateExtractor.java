package cn.winddol.ai.paper.internal.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HeadingCandidateExtractor {

    static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    static final Pattern INLINE_ABSTRACT = Pattern.compile("^(?i)(abstract)\\s*[:.]\\s+(.+)$");
    static final Pattern DECIMAL_SECTION = Pattern.compile("^(\\d+(?:\\.\\d+)+)\\.?\\s+(.+)$");
    static final Pattern NUMBERED_SECTION = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
    static final Pattern SECTION_SIGN_NUMBERED = Pattern.compile("^§\\s*(\\d+)\\.\\s+(.+)$");
    static final Pattern ROMAN_SECTION = Pattern.compile("^([IVXLCDM]+)\\.\\s+(.+)$");
    static final Pattern LETTER_SECTION = Pattern.compile("^([A-Z])\\.\\s+(.+)$");
    static final Pattern APPENDIX_SECTION = Pattern.compile("^(?i)(appendix\\s+[A-Z])[:.]?\\s+(.+)$");
    static final Pattern CHAPTER_SECTION = Pattern.compile("^(?i)chapter\\s+\\d+\\b.*$");
    static final Pattern IMAGE_ONLY = Pattern.compile("^!\\[[^\\]]*\\]\\([^)]+\\)\\s*$");
    static final Pattern THEOREM_LIKE = Pattern.compile(
            "^(?i)(theorem|lemma|proposition|definition|corollary|remark|example|proof|fig\\.?|figure|table)\\b.*"
    );
    static final Pattern REFERENCE_ITEM = Pattern.compile(
            "^(?:\\[\\d+]|\\$\\^\\{?\\d+\\}?\\$|\\d+\\.|[A-Z][a-zA-ZÀ-ÿ'\\-]+,?\\s+[A-Z].*)\\s+.+$"
    );

    List<HeadingCandidate> extract(String markdown) {
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<HeadingCandidate> candidates = new ArrayList<>();
        boolean afterReferences = false;

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i].stripTrailing();
            String trimmed = rawLine.trim();
            boolean blankBefore = i == 0 || lines[i - 1].trim().isEmpty();
            boolean blankAfter = i == lines.length - 1 || lines[i + 1].trim().isEmpty();

            Matcher markdownHeading = MARKDOWN_HEADING.matcher(trimmed);
            boolean isMarkdownHeading = markdownHeading.matches();
            int sourceLevel = isMarkdownHeading ? markdownHeading.group(1).length() : 0;
            String text = isMarkdownHeading ? clean(markdownHeading.group(2)) : clean(trimmed);

            CandidateKind kind = classify(text, isMarkdownHeading, afterReferences);
            String marker = extractMarker(text, kind);
            String remainder = extractRemainder(text, kind);

            candidates.add(new HeadingCandidate(
                    i + 1,
                    rawLine,
                    text,
                    isMarkdownHeading,
                    sourceLevel,
                    blankBefore,
                    blankAfter,
                    afterReferences,
                    kind,
                    marker,
                    remainder
            ));

            if (isReferences(text)) {
                afterReferences = true;
            }
        }
        return candidates;
    }

    static String clean(String text) {
        if (text == null) {
            return "";
        }
        return stripWrappingQuotes(text.replaceAll("\\s{2,}", " ").trim());
    }

    static String key(String text) {
        return clean(text)
                .replaceAll("^#+\\s*", "")
                .replaceAll("\\s+\\*$", "")
                .replaceAll("(?i)\\s+(FREE|EP)$", "")
                .replaceAll("\\s*[\\u2610\\u2611\\u2713\\u2714]\\s*$", "")
                .toLowerCase(Locale.ROOT);
    }

    static String cleanTitle(String text) {
        return clean(text)
                .replaceAll("\\s+\\*$", "")
                .replaceAll("(?i)\\s+(FREE|EP)$", "")
                .replaceAll("\\s*[\\u2610\\u2611\\u2713\\u2714]\\s*$", "")
                .trim();
    }

    static boolean isReferences(String text) {
        String lower = key(text);
        return lower.equals("references") || lower.equals("bibliography");
    }

    static boolean isStandardSection(String text) {
        String lower = key(text);
        return lower.equals("abstract")
                || lower.equals("references")
                || lower.startsWith("acknowledg")
                || lower.startsWith("data availability")
                || lower.startsWith("orcid")
                || lower.startsWith("appendix");
    }

    static boolean isNoiseHeading(String text) {
        String lower = key(text);
        return lower.equals("paper")
                || lower.equals("affiliations")
                || lower.equals("a preprint")
                || lower.equals("preprint")
                || lower.equals("aip advances")
                || lower.equals("why publish with us")
                || lower.equals("contents")
                || lower.equals("cc creative commons")
                || lower.equals("common deed")
                || lower.equals("commons deed")
                || lower.equals("you may also like")
                || lower.equals("articles you may be interested in");
    }

    static boolean isFrontMatterNoise(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return IMAGE_ONLY.matcher(text).matches()
                || lower.startsWith("to cite this article")
                || lower.startsWith("view the article online")
                || lower.startsWith("check for updates")
                || lower.startsWith("research article |")
                || lower.startsWith("cite as:")
                || lower.startsWith("submitted:")
                || lower.startsWith("published online:")
                || lower.startsWith("received:")
                || lower.startsWith("accepted:")
                || lower.contains("creative commons")
                || lower.contains("commons deed")
                || lower.contains("legal code")
                || lower.contains("disclaimer")
                || lower.contains("all rights reserved")
                || lower.contains("著作权")
                || lower.contains("저작권")
                || lower.contains("비영리")
                || lower.equals("chorus")
                || lower.equals("aip publishing")
                || lower.equals("learn more")
                || lower.matches("\\d{1,2}\\s+[a-z]+\\s+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}");
    }

    private CandidateKind classify(String text, boolean markdownHeading, boolean afterReferences) {
        if (text.isBlank()) {
            return CandidateKind.BODY;
        }
        if (afterReferences && REFERENCE_ITEM.matcher(text).matches()) {
            return CandidateKind.REFERENCE_ITEM;
        }
        if (isNoiseHeading(text) || isFrontMatterNoise(text)) {
            return CandidateKind.NOISE;
        }
        if (THEOREM_LIKE.matcher(text).matches()) {
            return CandidateKind.THEOREM_LIKE;
        }
        Matcher inlineAbstract = INLINE_ABSTRACT.matcher(text);
        if (inlineAbstract.matches() || key(text).equals("abstract")) {
            return CandidateKind.ABSTRACT;
        }
        if (isReferences(text)) {
            return CandidateKind.REFERENCES;
        }
        if (APPENDIX_SECTION.matcher(text).matches() || key(text).startsWith("appendix")) {
            return CandidateKind.APPENDIX;
        }
        if (CHAPTER_SECTION.matcher(text).matches()) {
            return CandidateKind.SECTION;
        }
        if (SECTION_SIGN_NUMBERED.matcher(text).matches()) {
            return CandidateKind.SECTION;
        }
        if (DECIMAL_SECTION.matcher(text).matches()) {
            return CandidateKind.SUBSECTION;
        }
        Matcher numbered = NUMBERED_SECTION.matcher(text);
        if (numbered.matches()) {
            return isLikelyListItem(numbered.group(2)) && !markdownHeading
                    ? CandidateKind.LIST_ITEM
                    : CandidateKind.SECTION;
        }
        if (ROMAN_SECTION.matcher(text).matches()) {
            return CandidateKind.SECTION;
        }
        if (LETTER_SECTION.matcher(text).matches()) {
            return CandidateKind.SUBSECTION;
        }
        if (isStandardSection(text)) {
            return CandidateKind.SECTION;
        }
        return markdownHeading ? CandidateKind.UNCERTAIN : CandidateKind.BODY;
    }

    private static String stripWrappingQuotes(String text) {
        String cleaned = text;
        while (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("\u201c") && cleaned.endsWith("\u201d")))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private String extractMarker(String text, CandidateKind kind) {
        Pattern pattern = switch (kind) {
            case SUBSECTION -> DECIMAL_SECTION;
            case SECTION -> SECTION_SIGN_NUMBERED.matcher(text).matches() ? SECTION_SIGN_NUMBERED : NUMBERED_SECTION;
            case APPENDIX -> APPENDIX_SECTION;
            default -> null;
        };
        if (pattern == null) {
            return "";
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.matches() ? matcher.group(1) : "";
    }

    private String extractRemainder(String text, CandidateKind kind) {
        if (kind == CandidateKind.ABSTRACT) {
            Matcher matcher = INLINE_ABSTRACT.matcher(text);
            return matcher.matches() ? matcher.group(2).trim() : "";
        }
        if (kind == CandidateKind.SUBSECTION) {
            Matcher matcher = DECIMAL_SECTION.matcher(text);
            if (matcher.matches()) {
                return splitInlineHeadingBody(matcher.group(2)).remainder();
            }
        }
        return "";
    }

    static InlineSplit splitInlineHeadingBody(String text) {
        String cleaned = clean(text);
        Matcher matcher = Pattern.compile("^(.{8,100}?\\.)(\\s+)([A-Z].+)$").matcher(cleaned);
        if (matcher.matches()) {
            return new InlineSplit(matcher.group(1).trim(), matcher.group(3).trim());
        }
        return new InlineSplit(cleaned, "");
    }

    static boolean isLikelyListItem(String text) {
        String cleaned = clean(text).replaceAll("^\"(.+)\"$", "$1").trim();
        if (cleaned.isBlank()) {
            return false;
        }
        String firstWord = cleaned.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return switch (firstWord) {
            case "sample", "set", "obtain", "denote", "choose", "draw", "generate", "repeat",
                 "return", "compute", "solve", "let" -> true;
            default -> false;
        };
    }

    record InlineSplit(String heading, String remainder) {
    }
}
