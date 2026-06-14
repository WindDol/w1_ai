package cn.winddol.ai.infrastructure.parser;
import cn.winddol.ai.paper.domain.SectionPO;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MarkdownParser {

    private final OutlineRepairer outlineRepairer = new OutlineRepairer();

    /**
     * 核心方法：解析 Markdown 文本
     *
     * @param markdown 昨天的 output_test.md 的内容
     * @return 解析好的章节列表
     */
    public List<SectionPO> parse(String markdown) {
        markdown = outlineRepairer.recoverMissingParentHeadings(markdown);
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
                    if (PaperCleaner.isNoise(line)) {
                        continue;
                    }

                    // 普通行：追加到当前章节内容
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        currentSection.contentBuffer.append("\n"); // 真正的空行才换行
                        continue;
                    }

                    if (shouldMergeWithPreviousLine(currentSection.contentBuffer)) {
                        // 删除缓冲区末尾可能的换行符，追加空格
                        trimTrailingNewline(currentSection.contentBuffer);
                        currentSection.contentBuffer.append(" ").append(trimmedLine).append("\n");
                    } else {
                        currentSection.contentBuffer.append(line).append("\n");
                    }
                }
            }
            // 保存最后一章
            sections.add(currentSection);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return outlineRepairer.repair(sections);
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

    private boolean shouldMergeWithPreviousLine(StringBuilder sb) {
        if (sb.length() == 0) return false;
        // 找到最后一个非空字符
        int i = sb.length() - 1;
        while (i >= 0 && Character.isWhitespace(sb.charAt(i))) {
            i--;
        }
        if (i < 0) return false;

        char lastChar = sb.charAt(i);
        // 如果不是句号、问号、感叹号、冒号，说明句子可能断了
        // 这里的规则可以根据需要调整，比如允许分号换行
        return ".!?:;".indexOf(lastChar) == -1;
    }

    // 辅助方法 2
    private void trimTrailingNewline(StringBuilder sb) {
        int i = sb.length() - 1;
        while (i >= 0 && (sb.charAt(i) == '\n' || sb.charAt(i) == '\r')) {
            sb.deleteCharAt(i);
            i--;
        }
    }

    public String extractAbstract(List<SectionPO> sections) {
        if (sections == null || sections.isEmpty()) return "";

        // --- 阶段 1：精准定位 (优先找标题或明显标签) ---
        String content = findByExplicitMethod(sections);
        if (!content.isEmpty()) return cleanAbstractNoise(content);

        // --- 阶段 2：深度扫描 (扫描文档前 50% 的章节内容) ---
        // 这是你要求的：如果前面没找到，就地毯式搜索前半部分
        log.info("🔍 Entering Deep Scan mode for Abstract extraction...");
        String deepScanResult = scanContentHeuristically(sections);
        if (!deepScanResult.isEmpty()) {
            return cleanAbstractNoise(deepScanResult);
        }

        return "Abstract Not Found";
    }

    /**
     * 策略：扫描前 50% 的章节，通过关键词和段落特征寻找摘要
     */
    private String scanContentHeuristically(List<SectionPO> sections) {
        // 只扫描前 50% 的章节，避免扫到结论或参考文献
        int scanLimit = Math.max(1, sections.size() / 2);

        for (int i = 0; i < scanLimit; i++) {
            String content = sections.get(i).getContent();
            if (content == null || content.length() < 100) continue;

            // 1. 寻找文本内部的 "Abstract" 关键词
            int abstractIndex = content.toUpperCase().indexOf("ABSTRACT");
            if (abstractIndex != -1) {
                // 找到了关键词，截取之后的部分
                // 注意：如果这章后面跟着 Introduction，我们只取到 Introduction 之前
                String sub = content.substring(abstractIndex);
                if (sub.toUpperCase().contains("INTRODUCTION")) {
                    sub = sub.substring(0, sub.toUpperCase().indexOf("INTRODUCTION"));
                }
                return sub;
            }

            // 2. 启发式：寻找“看起来像摘要”的段落
            // 如果没有关键词，找这一章里长度在 300-2000 字符之间，
            // 且包含 "in this paper", "we propose", "this study" 等词的段落
            String[] paragraphs = content.split("\n\n");
            for (String p : paragraphs) {
                String pUp = p.toUpperCase();
                if (p.length() > 300 && p.length() < 2500) {
                    if (pUp.contains("IN THIS PAPER") || pUp.contains("WE PROPOSE") ||
                            pUp.contains("STUDY") || pUp.contains("RESULTS SHOW")) {
                        return p;
                    }
                }
            }
        }
        return "";
    }

    /**
     * 整合之前的几种快速定位方法
     */
    private String findByExplicitMethod(List<SectionPO> sections) {
        // A. 找标题
        Optional<SectionPO> headerMatch = sections.stream()
                .filter(s -> s.getHeader().toUpperCase().contains("ABSTRACT"))
                .findFirst();
        if (headerMatch.isPresent()) return headerMatch.get().getContent();

        // B. 找第一章（Pre-Introduction）的末尾
        String preIntro = sections.get(0).getContent();
        if (preIntro.toUpperCase().contains("ABSTRACT")) {
            return preIntro.substring(preIntro.toUpperCase().indexOf("ABSTRACT"));
        }

        return "";
    }
    /**
     * 清理摘要中的噪音（邮件、DOI、版权、期刊名）
     */
    private String cleanAbstractNoise(String text) {
        if (text == null) return "";

        String cleaned = text;
        // 1. 去掉起始标签
        cleaned = cleaned.replaceAll("^(?i)Abstract[:\\s\\*]*", "");

        // 2. 去掉版权信息 (针对第 3 篇)
        cleaned = cleaned.replaceAll("(?i)Copyright ©.*", "");
        cleaned = cleaned.replaceAll("(?i)All rights reserved.*", "");

        // 3. 去掉关键字标签 (针对第 2, 3 篇)
        cleaned = cleaned.replaceAll("(?i)\\*\\*Keywords:\\*\\*.*", "");
        cleaned = cleaned.replaceAll("(?i)Keywords:.*", "");

        // 4. 去掉作者邮件和地址 (针对第 1 篇)
        // 匹配类似 {ziqiao.zhang,...}@gatech.edu 或 (e-mails: ...)
        cleaned = cleaned.replaceAll("\\{?[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\}?", "");
        cleaned = cleaned.replaceAll("(?i)\\(e-mails:.*\\)", "");

        // 5. 去掉 DOI 和 URL
        cleaned = cleaned.replaceAll("https?://\\S+", "");
        cleaned = cleaned.replaceAll("(?i)DOI:.*", "");

        // 6. 去掉多余的空行和首尾空格
        return cleaned.trim().replaceAll("\n{3,}", "\n\n");
    }
}
