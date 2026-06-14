package cn.winddol.ai.infrastructure.parser;

import cn.winddol.ai.paper.domain.ReferenceItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReferenceParser {

    // --- 模式 A: 数字引用 ---
    // 匹配 [1], 1., <sup>1</sup>
    private static final Pattern NUMBERED_PATTERN = Pattern.compile(
            "^(?:<sup>(\\d+)</sup>|\\[(\\d+)]|\\$\\^\\{?(\\d+)\\}?\\$|(\\d+)\\.?)\\s*(.*)$"
    );

    // --- 模式 B: 作者-年份引用 ---
    // 修改点：[a-zA-Z\\-\\.] 增加了点号，支持 "J. T. Beale" 这种缩写形式
    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            "^([A-Z][a-zA-Z\\-\\.]+(?:, [A-Z]\\.| et al\\.|\\s+[A-Z]\\.)?.*?)\\s*\\((\\d{4}[a-z]?)\\)(?:\\.|\\s)(.*)$"
    );

    // --- 预处理正则：用于炸开粘连的引用 ---
    // 逻辑：寻找 "空格/标点 + 数字 + 点 + 空格 + 大写字母"
    // 例如匹配 "...50586 2. R. C..." 中的 " 2. R"
    private static final Pattern INLINE_NUMBERED_REF = Pattern.compile(
            "(?<=[\\s.;])(\\d+)\\.\\s+(?=[A-Z])"
    );

    public List<ReferenceItem> parse(Long paperId, String content) {
        // 1. 【核心修复】预处理：把粘连在一起的引用强制换行
        String normalizedContent = normalizeContent(content);

        List<ReferenceItem> refs = new ArrayList<>();
        String[] lines = normalizedContent.split("\n");

        StringBuilder buffer = new StringBuilder();
        String currentId = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher numMatcher = NUMBERED_PATTERN.matcher(line);
            Matcher authMatcher = AUTHOR_YEAR_PATTERN.matcher(line);

            boolean isNewRef = false;
            String newId = null;
            String newContent = null;

            if (numMatcher.find()) {
                // 命中数字格式 (2., [2], <sup>2</sup>)
                isNewRef = true;
                // 智能获取 ID
                newId = getFirstNonNullGroup(numMatcher, 1, 2, 3, 4);
                newContent = numMatcher.group(5);
            } else if (authMatcher.find()) {
                isNewRef = true;
                String authorPart = authMatcher.group(1);
                String yearPart = authMatcher.group(2);
                newId = generateAuthorYearId(authorPart, yearPart);
                newContent = line;
            }

            if (isNewRef) {
                // 保存上一条
                if (currentId != null) {
                    saveCurrentRef(refs, paperId, currentId, buffer);
                }
                // 开始新一条
                currentId = newId;
                buffer = new StringBuilder();
                if (newContent != null) {
                    buffer.append(newContent);
                }
            } else {
                // 续行
                if (currentId != null) {
                    buffer.append(" ").append(line);
                }
            }
        }
        // 保存最后一条
        if (currentId != null) {
            saveCurrentRef(refs, paperId, currentId, buffer);
        }
        return refs;
    }

    /**
     * 【核心修复逻辑】
     * 将 "...BF00250586 2. R. C. Budzinski..." 替换为 "...BF00250586\n2. R. C. Budzinski..."
     */
    private String normalizeContent(String content) {
        if (content == null) return "";

        Matcher matcher = INLINE_NUMBERED_REF.matcher(content);
        return matcher.replaceAll("\n$0").trim();
    }

    private void saveCurrentRef(List<ReferenceItem> refs, Long paperId, String id, StringBuilder buf) {
        refs.add(ReferenceItem.builder()
                .paperId(paperId)
                .refId(id)
                .rawText(buf.toString().trim())
                .build());
    }

    private String getFirstNonNullGroup(Matcher m, int... indices) {
        for (int i : indices) {
            if (m.group(i) != null) return m.group(i);
        }
        return null;
    }

    private String generateAuthorYearId(String authorPart, String year) {
        String surname;
        if (authorPart.contains(",")) {
            surname = authorPart.split(",")[0].trim(); // "Altafini, C." -> "Altafini"
        } else {
            // "J. T. Beale" -> split space -> last element -> "Beale"
            String[] parts = authorPart.split("\\s+");
            // 过滤掉 "et" "al."
            int lastIdx = parts.length - 1;
            while (lastIdx >= 0 && (parts[lastIdx].equalsIgnoreCase("al.") || parts[lastIdx].equalsIgnoreCase("et"))) {
                lastIdx--;
            }
            if (lastIdx >= 0) {
                surname = parts[lastIdx];
            } else {
                surname = authorPart; // fallback
            }
        }

        surname = surname.replaceAll("[^a-zA-Z\\-]", "");
        return surname + " " + year;
    }
}
