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
    private static final Pattern CITATION_MARK_PATTERN = Pattern.compile(
            "\\[([\\d\\s,\\-–]+)\\]" +              // Group 1: []
                    "|<sup>([\\d\\s,\\-–]+)</sup>" +        // Group 2: <sup>
                    "|\\$\\^\\{([\\d\\s,\\-–]+)\\}\\$" +    // Group 3: $^{...}$
                    "|\\^\\{([\\d\\s,\\-–]+)\\}"            // Group 4: ^{...}
    );

    public Set<String> extractIndices(String content) {
        Set<String> indices = new HashSet<>();
        Matcher matcher = CITATION_MARK_PATTERN.matcher(content);
        while (matcher.find()) {
            String rawNumbers = null;
            if (matcher.group(1) != null) rawNumbers = matcher.group(1);
            else if (matcher.group(2) != null) rawNumbers = matcher.group(2);
            else if (matcher.group(3) != null) rawNumbers = matcher.group(3);
            else if (matcher.group(4) != null) rawNumbers = matcher.group(4);

            if (rawNumbers != null) {
                parseNumbers(indices, rawNumbers);
            }
        }
        return indices;
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