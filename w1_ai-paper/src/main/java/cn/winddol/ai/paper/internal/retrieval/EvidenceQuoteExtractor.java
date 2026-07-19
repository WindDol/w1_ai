package cn.winddol.ai.paper.internal.retrieval;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
public class EvidenceQuoteExtractor {

    private static final int MAX_QUOTE_LENGTH = 700;

    /**
     * 从候选正文中截取查询词附近的证据片段，正文较短时直接完整返回。
     */
    public String extract(String query, String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String clean = content.replaceAll("\\s+", " ").trim();
        if (clean.length() <= MAX_QUOTE_LENGTH) {
            return clean;
        }

        String lower = clean.toLowerCase(Locale.ROOT);
        // 优先围绕最早命中的有效查询词截取证据，而不是始终返回正文开头。
        int match = Arrays.stream(query == null ? new String[0] : query.split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() >= 4)
                .map(token -> lower.indexOf(token.toLowerCase(Locale.ROOT)))
                .filter(index -> index >= 0)
                .min(Integer::compareTo)
                .orElse(0);
        int start = Math.max(0, match - MAX_QUOTE_LENGTH / 3);
        int end = Math.min(clean.length(), start + MAX_QUOTE_LENGTH);
        if (end - start < MAX_QUOTE_LENGTH && end == clean.length()) {
            start = Math.max(0, end - MAX_QUOTE_LENGTH);
        }
        return (start > 0 ? "..." : "") + clean.substring(start, end) + (end < clean.length() ? "..." : "");
    }
}
