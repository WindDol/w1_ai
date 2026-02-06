package cn.winddol.ai.infrastructure.parser;

import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReferenceParser {

    // 更加健壮的序号匹配：Group 1 永远是我们要的纯数字 ID
    private static final Pattern NUMBERED_PATTERN = Pattern.compile(
            "^(?:<sup>(\\d+)</sup>|\\[(\\d+)\\]|(\\d+)\\.?)\\s*(.*)$"
    );

    // 作者年份正则：放宽行首限制，允许前面有序号或标签后的残余
    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            "([A-Z][a-zA-Z\\-]+(?:, [A-Z]\\.| et al\\.)?.*?)\\s*\\((\\d{4}[a-z]?)\\)(?:\\.|\\s)(.*)$"
    );

    public List<ReferenceItem> parse(Long paperId, String content) {
        List<ReferenceItem> refs = new ArrayList<>();
        String[] lines = content.split("\n");
        StringBuilder buffer = new StringBuilder();
        String currentId = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher numMatcher = NUMBERED_PATTERN.matcher(line);

            if (numMatcher.find()) {
                // --- 发现新条目 (数字驱动型) ---
                if (currentId != null) {
                    saveCurrentRef(refs, paperId, currentId, buffer);
                }

                // 智能获取数字 ID (遍历 1, 2, 3 组，哪个不为空取哪个)
                currentId = getFirstNonNullGroup(numMatcher, 1, 2, 3);
                buffer = new StringBuilder(numMatcher.group(4)); // 第4组是剩余内容

            } else {
                // --- 检查是否是纯作者年份型 (无序号) ---
                Matcher authMatcher = AUTHOR_YEAR_PATTERN.matcher(line);
                if (authMatcher.find() && currentId == null) {
                    // 只有在没找到数字 ID 的情况下才触发纯作者模式
                    currentId = generateAuthorYearId(authMatcher.group(1), authMatcher.group(2));
                    buffer = new StringBuilder(line);
                } else if (currentId != null) {
                    // --- 续行处理 ---
                    buffer.append(" ").append(line);
                }
            }
        }
        // 最后一笔
        if (currentId != null) {
            saveCurrentRef(refs, paperId, currentId, buffer);
        }
        return refs;
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
        String surname = authorPart.split("[,\\s]")[0].trim();
        surname = surname.replaceAll("[^a-zA-Z\\-]", "");
        return surname + " " + year;
    }
}