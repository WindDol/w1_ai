package cn.winddol.ai.paper.internal.enrichment;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Component
public class CitationLinkExtractor {

    private static final Pattern NUMBERED_CITATION_PATTERN = Pattern.compile(
            "\\[([\\d\\s,\\-–]+)]"
                    + "|<sup>([\\d\\s,\\-–]+)</sup>"
                    + "|\\$\\^\\{([\\d\\s,\\-–]+)}\\$"
                    + "|\\^\\{([\\d\\s,\\-–]+)}"
    );

    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            "([A-Z][a-zA-Z\\-À-ÿ]+(?:\\s+et\\s+al\\.|\\s+and\\s+[A-Z][a-zA-Z\\-À-ÿ]+)?)"
                    + "\\s*"
                    + "\\((\\d{4}[a-z]?(?:[,;]\\s*\\d{4}[a-z]?)*)\\)"
    );

    public Set<String> extractIndices(String content) {
        Set<String> indices = new HashSet<>();
        if (content == null || content.isBlank()) {
            return indices;
        }
        extractNumberedCitations(content, indices);
        extractAuthorYearCitations(content, indices);
        return indices;
    }

    private void extractNumberedCitations(String content, Set<String> indices) {
        Matcher matcher = NUMBERED_CITATION_PATTERN.matcher(content);
        while (matcher.find()) {
            String rawNumbers = null;
            if (matcher.group(1) != null) {
                rawNumbers = matcher.group(1);
            } else if (matcher.group(2) != null) {
                rawNumbers = matcher.group(2);
            } else if (matcher.group(3) != null) {
                rawNumbers = matcher.group(3);
            } else if (matcher.group(4) != null) {
                rawNumbers = matcher.group(4);
            }

            if (rawNumbers != null) {
                parseNumbers(indices, rawNumbers);
            }
        }
    }

    private void extractAuthorYearCitations(String content, Set<String> indices) {
        Matcher matcher = AUTHOR_YEAR_PATTERN.matcher(content);
        while (matcher.find()) {
            String authorPart = matcher.group(1).trim();
            String yearPart = matcher.group(2).trim();
            String[] years = yearPart.split("[,;]\\s*");
            for (String year : years) {
                indices.add(normalizeAuthorYearKey(authorPart, year));
            }
        }
    }

    private String normalizeAuthorYearKey(String authorPart, String year) {
        String primaryAuthor = authorPart.split("\\s+(et\\s+al\\.|and)")[0].trim();
        return primaryAuthor + " " + year;
    }

    private void parseNumbers(Set<String> indices, String rawNumbers) {
        String[] parts = rawNumbers.split("[,，]");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-") || part.contains("–") || part.contains("—")) {
                indices.addAll(expandRange(part));
            } else if (part.matches("\\d+")) {
                indices.add(part);
            }
        }
    }

    private List<String> expandRange(String rangeStr) {
        String[] limits = rangeStr.split("[-–—]");
        if (limits.length != 2) {
            return List.of();
        }
        try {
            int start = Integer.parseInt(limits[0].trim());
            int end = Integer.parseInt(limits[1].trim());
            if (start > end || (end - start) > 1000) {
                return List.of();
            }
            return IntStream.rangeClosed(start, end).mapToObj(String::valueOf).toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
