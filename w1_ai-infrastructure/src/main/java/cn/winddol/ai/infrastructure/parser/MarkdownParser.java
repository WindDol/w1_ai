package cn.winddol.ai.infrastructure.parser;
import cn.winddol.ai.domain.paper.model.entity.SectionPO;
import com.alibaba.fastjson2.JSONObject;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MarkdownParser {


    /**
     * 核心方法：解析 Markdown 文本
     * @param markdown 昨天的 output_test.md 的内容
     * @return 解析好的章节列表
     */
    public List<SectionPO> parse(String markdown) {
        List<SectionPO> sections = new ArrayList<>();
        String mainTitle = null;
        // 用于匹配标题行：# Title, ## Header
        // ^\s* 允许前面有空格
        // (#+) 捕获井号数量
        // \s+ 必须有空格
        // (.*) 捕获标题内容
        Pattern headerPattern = Pattern.compile("^\\s*(#+)\\s+(.*)$");
        String[] activeParentIds = new String[10];

        try (BufferedReader reader = new BufferedReader(new StringReader(markdown))) {
            String line;
            SectionPO currentSection = new SectionPO("Pre-Introduction", 0, null); // 默认前言部分
            activeParentIds[0] = currentSection.uuid;

            while ((line = reader.readLine()) != null) {
                if (mainTitle == null && line.trim().startsWith("# ")) {
                    mainTitle = line.trim();
                }

                if (PaperCleaner.isNoise(line, mainTitle)) {
                    continue; // 丢弃页眉
                }

                if (PaperCleaner.isNoise(line)) {
                    continue;
                }

                Matcher matcher = headerPattern.matcher(line);

                // 判断是否是标题行
                if (matcher.matches()) {
                    // 1. 保存上一章 (如果内容不为空)
                    if (currentSection.contentBuffer.length() > 0 || currentSection.level > 0) {
                        sections.add(currentSection);
                    }

                    // 2. 开启新一章
                    int level = matcher.group(1).length(); // # 的数量
                    String headerText = matcher.group(2).trim();
                    String parentId = (level > 1) ? activeParentIds[level - 1] : null;
                    currentSection = new SectionPO(headerText, level,parentId);
                    activeParentIds[level] = currentSection.uuid;
                    for (int i = level + 1; i < activeParentIds.length; i++) {
                        activeParentIds[i] = null;
                    }
                } else {
                    // 普通行：追加到当前章节内容
                    currentSection.contentBuffer.append(line).append("\n");
                }
            }
            // 保存最后一章
            sections.add(currentSection);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sections;
    }

    /**
     * 构建树状 JSON Outline
     * 逻辑：简单的扁平转树逻辑，或者直接做成一级 Map
     * 为了给 Agent 用，最简单的结构是 Map: "Header Name" -> "UUID"
     */
    public JSONObject buildFlatOutline(List<SectionPO> sections) {
        JSONObject outline = new JSONObject(new LinkedHashMap<>()); // 保持顺序
        for (SectionPO sec : sections) {
            // Key: 标题 (加上 # 符号方便 Agent 识别级别)
            // Value: UUID (用于查数据库)
            String key = "#".repeat(sec.level) + " " + sec.header;
            outline.put(key, sec.uuid);
        }
        return outline;
    }
}
