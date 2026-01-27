package cn.winddol.ai.infrastructure.parser;

import cn.winddol.ai.domain.paper.model.valobj.ReferenceItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReferenceParser {

    // 匹配 "[1] Author..." 或 "1. Author..."
    private static final Pattern REF_PATTERN = Pattern.compile("^(\\[?(\\d+)\\]?\\.?)\\s+(.*)$");

    public List<ReferenceItem> parse(String content) {
        List<ReferenceItem> refs = new ArrayList<>();
        String[] lines = content.split("\n");

        StringBuilder buffer = new StringBuilder();
        String currentId = null;

        for (String line : lines) {
            Matcher m = REF_PATTERN.matcher(line.trim());
            if (m.find()) {
                // 保存上一条
                if (currentId != null) {
                    refs.add(new ReferenceItem(currentId, buffer.toString().trim(), null));
                }
                // 开始新一条
                currentId = m.group(2); // 捕获数字 ID
                buffer = new StringBuilder(m.group(3));
            } else {
                // 如果不是新编号，可能是上一条的换行，追加进去
                if (currentId != null) {
                    buffer.append(" ").append(line.trim());
                }
            }
        }
        // 保存最后一条
        if (currentId != null) {
            refs.add(new ReferenceItem(currentId, buffer.toString().trim(), null));
        }
        return refs;
    }
}