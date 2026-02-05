package cn.winddol.ai.infrastructure.parser;
import cn.winddol.ai.domain.paperTools.model.entity.SectionPO;
import com.alibaba.fastjson2.JSONObject;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MarkdownParser {


    /**
     * 核心方法：解析 Markdown 文本
     *
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


                if (PaperCleaner.isNoise(line)) {
                    continue;
                }

                Matcher matcher = headerPattern.matcher(line);

                // 判断是否是标题行
                if (matcher.matches()) {
                    // 1. 保存上一章 (如果内容不为空)
                    if (!currentSection.contentBuffer.isEmpty() || currentSection.level > 0) {
                        sections.add(currentSection);
                    }

                    // 2. 开启新一章
                    int level = matcher.group(1).length(); // # 的数量
                    String headerText = matcher.group(2).trim();
                    String parentId = (level > 1) ? activeParentIds[level - 1] : null;
                    currentSection = new SectionPO(headerText, level, parentId);
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

    /**
     * 专门提取论文标题的方法
     * 逻辑：寻找文档中第一个一级标题 (# )
     *
     * @param markdown 全文 Markdown
     * @return 标题字符串，如果没找到则返回 "Untitled Paper"
     */
    public String extractTitle(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "Untitled Paper";
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(markdown))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                // 跳过空行
                if (trimmedLine.isEmpty()) continue;

                // 跳过可能存在的噪音 (复用之前的逻辑)
                if (PaperCleaner.isNoise(trimmedLine)) continue;

                // 1. 优先匹配一级标题 "# Title"
                if (trimmedLine.startsWith("# ")) {
                    return cleanMarkdownSyntax(trimmedLine);
                }

                // 2. 兜底策略：如果 LLM 没生成 #，而是生成了 ## Title (有时候会发生)
                // 且这一行不是常见的章节名 (Abstract, Introduction)，那它可能是标题
                if (trimmedLine.startsWith("## ")) {
                    String potentialTitle = cleanMarkdownSyntax(trimmedLine);
                    if (!isStandardSectionHeader(potentialTitle)) {
                        return potentialTitle;
                    }
                }

                // 为了防止读取整个文件，如果读了前 50 行还没找到标题，就停止
                // (通常标题肯定在前 20 行内)
                // 这里可以加个计数器，略。
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Untitled Paper";
    }

    /**
     * 辅助方法：清洗标题中的 Markdown 符号
     * 例如: "# **The Theory of Everything**" -> "The Theory of Everything"
     */
    private String cleanMarkdownSyntax(String text) {
        // 1. 去掉开头的 # 和空格
        String temp = text.replaceAll("^#+\\s*", "");
        // 2. 去掉粗体/斜体符号 (*, _)
        temp = temp.replaceAll("[*_]{2,}", ""); // 去掉 ** 或 __
        temp = temp.replaceAll("[*_]", "");     // 去掉单 * 或 _
        return temp.trim();
    }

    /**
     * 辅助方法：判断是不是标准的章节名
     * 用于防止把 "## Abstract" 误判为论文标题
     */
    private boolean isStandardSectionHeader(String title) {
        String t = title.toLowerCase();
        return t.equals("abstract") ||
                t.startsWith("introduction") ||
                t.startsWith("author") ||
                t.equals("index");

    }
}
