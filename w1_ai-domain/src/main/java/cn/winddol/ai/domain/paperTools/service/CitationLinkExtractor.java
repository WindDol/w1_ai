package cn.winddol.ai.domain.paperTools.service;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Component
public class CitationLinkExtractor {
    // 匹配 [1], [1, 2], [1-3], [1, 2, 5-7]
    private static final Pattern NUMBERED_CITATION_PATTERN  = Pattern.compile(
            "\\[([\\d\\s,\\-–]+)\\]" +              // Group 1: []
                    "|<sup>([\\d\\s,\\-–]+)</sup>" +        // Group 2: <sup>
                    "|\\$\\^\\{([\\d\\s,\\-–]+)\\}\\$" +    // Group 3: $^{...}$
                    "|\\^\\{([\\d\\s,\\-–]+)\\}"            // Group 4: ^{...}
    );
    // 匹配规则：
    // 1. 名字部分: 大写开头, 可能包含连字符, 可能跟着 "et al." 或 "and Xxx"
    // 2. 年份部分: 括号括起来的 4 位数字, 可能包含多个年份 (2017, 2018)
    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            // Group 1: Author Name (e.g., "Franci et al.", "Proskurnikov and Tempo", "Altafini")
            "([A-Z][a-zA-Z\\-À-ÿ]+(?:\\s+et\\s+al\\.|\\s+and\\s+[A-Z][a-zA-Z\\-À-ÿ]+)?)" +
                    "\\s*" +
                    // Group 2: Year(s) (e.g., "(2021)", "(2017, 2018)")
                    "\\((\\d{4}[a-z]?(?:[,;]\\s*\\d{4}[a-z]?)*)\\)"
    );


    public Set<String> extractIndices(String content) {
        Set<String> indices = new HashSet<>();

        // 1. 尝试提取数字格式
        extractNumberedCitations(content, indices);

        // 2. 尝试提取作者-年份格式
        // 如果数字格式提取到了，通常意味着这篇论文主要用数字，但也可能混用，建议都跑一遍
        extractAuthorYearCitations(content, indices);

        return indices;
    }

    private void extractNumberedCitations(String content, Set<String> indices) {
        Matcher matcher = NUMBERED_CITATION_PATTERN.matcher(content);
        while (matcher.find()) {
            String rawNumbers = null;
            if (matcher.group(1) != null) rawNumbers = matcher.group(1);
            else if (matcher.group(2) != null) rawNumbers = matcher.group(2);
            else if (matcher.group(3) != null) rawNumbers = matcher.group(3);

            if (rawNumbers != null) {
                parseNumbers(indices, rawNumbers);
            }
        }
    }

    private void extractAuthorYearCitations(String content, Set<String> indices) {
        Matcher matcher = AUTHOR_YEAR_PATTERN.matcher(content);
        while (matcher.find()) {
            String authorPart = matcher.group(1).trim();
            String yearPart = matcher.group(2).trim(); // "2017, 2018"

            // 处理同一个作者对应多个年份的情况: Proskurnikov and Tempo (2017, 2018)
            String[] years = yearPart.split("[,;]\\s*");
            for (String year : years) {
                // 生成标准化的 Key，例如: "Franci et al. 2021"
                // 这个 Key 将用于去 paper_references 表里做模糊匹配
                String refKey = normalizeAuthorYearKey(authorPart, year);
                indices.add(refKey);
            }
        }
    }

    private String normalizeAuthorYearKey(String authorPart, String year) {
        // 简化作者名，只取第一个姓氏，方便匹配
        // "Franci et al." -> "Franci"
        // "Proskurnikov and Tempo" -> "Proskurnikov"
        String primaryAuthor = authorPart.split("\\s+(et\\s+al\\.|and)")[0].trim();
        return primaryAuthor + " " + year;
    }
    private void parseNumbers(Set<String> indices, String rawNumbers) {
        // 处理逗号分割 (支持中文逗号兼容)
        String[] parts = rawNumbers.split("[,，]");
        for (String part : parts) {
            part = part.trim();
            // 处理范围 (支持多种横杠)
            if (part.contains("-") || part.contains("–") || part.contains("—")) {
                indices.addAll(expandRange(part));
            } else {
                // 过滤掉非数字字符，只保留数字
                if (part.matches("\\d+")) {
                    indices.add(part);
                }
            }
        }
    }
    private List<String> expandRange(String rangeStr) {
        // 实现 1-3 -> 1, 2, 3 的逻辑
        String[] limits = rangeStr.split("[-–—]");
        if (limits.length != 2) return List.of();
        try {
            int start = Integer.parseInt(limits[0].trim());
            int end = Integer.parseInt(limits[1].trim());
            if (start > end || (end - start) > 1000) {
                return List.of();
            }

            return IntStream.rangeClosed(start, end)
                    .mapToObj(String::valueOf)
                    .toList();
        } catch (Exception e) { return List.of(); }
    }
}