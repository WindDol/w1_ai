package cn.winddol.ai.infrastructure.parser;

import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReferenceParser {

    // 匹配 "[1] Author..." 或 "1. Author..."
    private static final Pattern NUMBERED_PATTERN = Pattern.compile(
            "^(<sup>(\\d+)</sup>|\\[(\\d+)\\]|(\\d+)\\.?)\\s*(.*)$"
    );

    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            "^([A-Z][a-zA-Z\\-]+(?:, [A-Z]\\.| et al\\.)?.*?)\\s*\\((\\d{4}[a-z]?)\\)(?:\\.|\\s)(.*)$"
    );

    public List<ReferenceItem> parse(Long paperId,String content) {
        List<ReferenceItem> refs = new ArrayList<>();
        String[] lines = content.split("\n");
        ReferenceItem currentRef = null;

        StringBuilder buffer = new StringBuilder();
        String currentId = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher numMatcher = NUMBERED_PATTERN.matcher(line);
            // 2. 尝试匹配作者-年份
            Matcher authMatcher = AUTHOR_YEAR_PATTERN.matcher(line);
            boolean isNewRef = false;
            String newId = null;
            String newContent = null;

            if (numMatcher.find()) {
                // 命中数字格式
                isNewRef = true;
                if (numMatcher.group(1) != null) newId = numMatcher.group(1);
                else if (numMatcher.group(2) != null) newId = numMatcher.group(2);
                else newId = numMatcher.group(3);
                newContent = numMatcher.group(4);
            } else if (authMatcher.find()) {
                // 命中作者年份格式
                isNewRef = true;
                String authorPart = authMatcher.group(1);
                String yearPart = authMatcher.group(2);
                String restContent = authMatcher.group(3);

                // 【关键】生成标准化的 ID: "Altafini 2013"
                // 必须与 CitationLinkExtractor 的生成逻辑保持一致
                newId = generateAuthorYearId(authorPart, yearPart);

                // 重组内容，保留原始文本
                newContent = line;
            }

            if (isNewRef) {
                // --- 保存上一条 ---
                if (currentId != null) {
                    refs.add(ReferenceItem.builder()
                            .paperId(paperId)
                            .refId(currentId)
                            .rawText(buffer.toString().trim())
                            .build());
                }
                // --- 开始新一条 ---
                currentId = newId;
                buffer = new StringBuilder();
                if (newContent != null) {
                    buffer.append(newContent);
                }
            } else {
                // --- 续行 ---
                // 如果不是新编号，说明是上一条引用的第二行
                if (currentId != null) {
                    buffer.append(" ").append(line);
                }
            }
        }

        // 保存最后一条
        if (currentId != null) {
            refs.add(ReferenceItem.builder()
                    .paperId(paperId)
                    .refId(currentId)
                    .rawText(buffer.toString().trim())
                    .build());
        }
        return refs;
    }

    /**
     * 生成与 CitationLinkExtractor 一致的 Key
     * 输入: "Altafini, C.", "2013" -> 输出: "Altafini 2013"
     */
    private String generateAuthorYearId(String authorPart, String year) {
        // 提取第一个作者的姓氏
        // "Altafini, C." -> "Altafini"
        // "Franci et al." -> "Franci"
        String surname = authorPart.split("[,\\s]")[0].trim();
        // 移除可能残留的非字母字符
        surname = surname.replaceAll("[^a-zA-Z\\-]", "");
        return surname + " " + year;
    }
}