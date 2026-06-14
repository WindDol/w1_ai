package cn.winddol.ai.infrastructure.parser;

import cn.winddol.ai.paper.domain.SectionPO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class OutlineRepairer {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern BARE_TOP_NUMBERED = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
    private static final Pattern DECIMAL_HEADER = Pattern.compile("^(\\d+)\\.\\d+(?:\\.|\\s).*$");
    private static final Pattern TOP_NUMBERED_HEADER = Pattern.compile("^(\\d+)(?:\\.\\s+|\\s+).*$");
    private static final Pattern CHAPTER_HEADER = Pattern.compile("(?i)^chapter\\s+(\\d+)\\b.*$");
    private static final Pattern CHAPTER_WITH_TITLE = Pattern.compile("(?i)^chapter\\s+(\\d+)\\.\\s*(.+)$");
    private static final Pattern INLINE_HEADING_BODY = Pattern.compile("^(.{8,100}?\\.)(\\s+)([A-Z].+)$");
    private static final Pattern REFERENCE_ITEM_START = Pattern.compile("^(?:\\[\\d+]|\\$\\^\\{?\\d+\\}?\\$)\\s*.+$");
    private static final int MAX_LEVEL = 9;

    String recoverMissingParentHeadings(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }

        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder repaired = new StringBuilder(normalized.length() + 128);
        boolean afterReferences = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            String text = stripMarkdownHeading(trimmed);

            if (isReferences(text)) {
                afterReferences = true;
            }

            if (!afterReferences && isReferenceItemStart(text)) {
                repaired.append("## References").append('\n').append(line);
                afterReferences = true;
            } else if (afterReferences) {
                repaired.append(line);
            } else {
                ParentHeading parentHeading = recoverParentHeading(lines, i);
                if (parentHeading == null) {
                    repaired.append(line);
                } else {
                    repaired.append("## ").append(parentHeading.heading());
                    if (!parentHeading.remainder().isBlank()) {
                        repaired.append('\n').append(parentHeading.remainder());
                    }
                }
            }

            if (i < lines.length - 1) {
                repaired.append('\n');
            }
        }
        return repaired.toString();
    }

    List<SectionPO> repair(List<SectionPO> sections) {
        if (sections == null || sections.isEmpty()) {
            return sections == null ? List.of() : sections;
        }

        List<SectionPO> repaired = new ArrayList<>();
        List<Integer> numberedParents = new ArrayList<>();

        for (SectionPO section : sections) {
            if (section == null) {
                continue;
            }
            if (shouldDropSection(section, repaired)) {
                continue;
            }

            Integer decimalTop = decimalTop(section.header);
            if (section.level >= 3 && decimalTop != null && !numberedParents.contains(decimalTop)) {
                if (canUsePreviousUnnumberedParent(repaired)) {
                    numberedParents.add(decimalTop);
                } else {
                    section.level = 2;
                }
            }

            repaired.add(section);
            Integer topNumber = topNumber(section.header);
            if (section.level == 2 && topNumber != null) {
                numberedParents.add(topNumber);
            }
        }

        rebuildParentIds(repaired);
        return repaired;
    }

    private ParentHeading recoverParentHeading(String[] lines, int index) {
        String raw = lines[index];
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (MARKDOWN_HEADING.matcher(trimmed).matches()) {
            return null;
        }
        Matcher matcher = BARE_TOP_NUMBERED.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }
        int number = Integer.parseInt(matcher.group(1));
        String titleAndMaybeBody = matcher.group(2).trim();
        if (isLikelyListItem(titleAndMaybeBody) || isReferenceLike(titleAndMaybeBody)) {
            return null;
        }
        if (!hasChildBeforeNextMainSection(lines, index + 1, number)) {
            return null;
        }

        InlineSplit split = splitInlineHeadingBody(titleAndMaybeBody);
        return new ParentHeading(number + ". " + split.heading(), split.remainder());
    }

    private boolean hasChildBeforeNextMainSection(String[] lines, int start, int parentNumber) {
        for (int i = start; i < lines.length; i++) {
            String text = stripMarkdownHeading(lines[i] == null ? "" : lines[i].trim());
            if (text.isBlank()) {
                continue;
            }
            if (isReferences(text)) {
                return false;
            }

            Integer nextTop = topNumber(text);
            if (nextTop != null) {
                return false;
            }
            Integer decimalTop = decimalTop(text);
            if (decimalTop != null) {
                return decimalTop == parentNumber;
            }
        }
        return false;
    }

    private InlineSplit splitInlineHeadingBody(String text) {
        Matcher matcher = INLINE_HEADING_BODY.matcher(clean(text));
        if (matcher.matches()) {
            return new InlineSplit(matcher.group(1).trim(), matcher.group(3).trim());
        }
        return new InlineSplit(clean(text), "");
    }

    private String stripMarkdownHeading(String line) {
        Matcher matcher = MARKDOWN_HEADING.matcher(line);
        return matcher.matches() ? matcher.group(2).trim() : line;
    }

    private boolean shouldDropSection(SectionPO section, List<SectionPO> repaired) {
        if (section.header == null || section.header.isBlank()) {
            return false;
        }
        return isChapterCompositeDuplicate(section.header, repaired);
    }

    private boolean isChapterCompositeDuplicate(String header, List<SectionPO> repaired) {
        Matcher matcher = CHAPTER_WITH_TITLE.matcher(header.trim());
        if (!matcher.matches()) {
            return false;
        }
        String chapter = "chapter " + matcher.group(1);
        String title = normalizeKey(matcher.group(2));
        int start = Math.max(0, repaired.size() - 4);
        boolean chapterSeen = false;
        boolean titleSeen = false;
        for (int i = start; i < repaired.size(); i++) {
            String key = normalizeKey(repaired.get(i).header);
            chapterSeen = chapterSeen || key.equals(chapter);
            titleSeen = titleSeen || key.equals(title);
        }
        return chapterSeen && titleSeen;
    }

    private boolean canUsePreviousUnnumberedParent(List<SectionPO> repaired) {
        if (repaired.isEmpty()) {
            return false;
        }
        SectionPO previous = repaired.get(repaired.size() - 1);
        if (previous.level != 2) {
            return false;
        }
        if (topNumber(previous.header) != null) {
            return false;
        }
        String key = normalizeKey(previous.header);
        return !key.equals("abstract")
                && !key.equals("references")
                && !key.equals("bibliography")
                && !key.startsWith("acknowledg")
                && !key.startsWith("appendix")
                && !CHAPTER_HEADER.matcher(previous.header == null ? "" : previous.header).matches();
    }

    private void rebuildParentIds(List<SectionPO> sections) {
        String[] activeParentIds = new String[MAX_LEVEL + 1];
        for (SectionPO section : sections) {
            int level = Math.max(0, Math.min(section.level, MAX_LEVEL));
            if (level <= 1) {
                section.parentId = null;
            } else {
                section.parentId = nearestParent(activeParentIds, level);
            }
            activeParentIds[level] = section.uuid;
            for (int i = level + 1; i < activeParentIds.length; i++) {
                activeParentIds[i] = null;
            }
        }
    }

    private String nearestParent(String[] activeParentIds, int level) {
        for (int i = level - 1; i >= 1; i--) {
            if (activeParentIds[i] != null) {
                return activeParentIds[i];
            }
        }
        return null;
    }

    private Integer decimalTop(String header) {
        if (header == null) {
            return null;
        }
        Matcher matcher = DECIMAL_HEADER.matcher(header.trim());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private Integer topNumber(String header) {
        if (header == null) {
            return null;
        }
        Matcher matcher = TOP_NUMBERED_HEADER.matcher(header.trim());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private boolean isReferences(String text) {
        String key = normalizeKey(text);
        return key.equals("references") || key.equals("bibliography");
    }

    private boolean isReferenceItemStart(String text) {
        return REFERENCE_ITEM_START.matcher(clean(text)).matches();
    }

    private boolean isLikelyListItem(String text) {
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

    private boolean isReferenceLike(String text) {
        String cleaned = clean(text);
        return cleaned.matches("^[A-Z][a-zA-ZÀ-ÿ'\\-]+,?\\s+[A-Z].*\\b\\d{4}\\b.*")
                || cleaned.matches(".*\\b(doi|journal|vol\\.|no\\.|pp\\.)\\b.*");
    }

    private String normalizeKey(String text) {
        if (text == null) {
            return "";
        }
        return clean(text)
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)^chapter\\s+(\\d+)\\.\\s*", "")
                .toLowerCase(Locale.ROOT);
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replaceAll("\\s{2,}", " ").trim();
        while (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("\u201c") && cleaned.endsWith("\u201d")))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private record ParentHeading(String heading, String remainder) {
    }

    private record InlineSplit(String heading, String remainder) {
    }
}
