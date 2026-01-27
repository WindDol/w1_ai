package cn.winddol.ai.infrastructure.parser;

import java.util.regex.Pattern;

public class PaperCleaner {

    // 预编译正则，提高性能

    private static final Pattern YEAR_PATTERN = Pattern.compile(".*\\b(19|20)\\d{2}\\b.*");

    // 2. 匹配常见的期刊/出版商关键词 (根据需要扩充)
    // 包括: Chaos, Vol, No., pp., doi, ISSN, ISBN, Physics, Institute, University
    private static final Pattern JOURNAL_PATTERN = Pattern.compile(
            ".*(Chaos|Vol\\.|No\\.|pp\\.|doi:|ISSN|ISBN|Physics|Institute|University|Society|Journal|Transactions).*",
            Pattern.CASE_INSENSITIVE
    );

    // 3. 匹配像页码的行 (如 "043104-3", "Page 12", "3")
    // 规则：行很短，且包含数字
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile(
            "^\\s*(Page\\s*)?(\\d+[\\-\\.]?\\d*)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // 4. 匹配论文编号/索引号 (如 "043104-3")
    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile(".*\\d{6}-\\d.*");

    // 5. 匹配 Running Title (页眉标题)
    // 特征：通常以 # 开头 (被误识别为标题)，但后面跟着页码或期刊名
    // 或者这一行跟论文的主标题高度相似 (这个比较难做通用，暂且不放)
    public static boolean isRunningTitle(String line, String mainTitle) {
        if (line == null || mainTitle == null) return false;

        String cleanLine = line.replace("#", "").trim();
        String cleanTitle = mainTitle.replace("#", "").trim();

        // 1. 如果当前行就是主标题的一部分 (比如 "Phase oscillators...")
        // 且它出现在正文中间 (我们假设主标题长度 > 10)
        if (cleanLine.length() > 10 && cleanTitle.contains(cleanLine)) {
            return true;
        }

        // 2. 简单的相似度检查 (可选，这里用简单的包含逻辑通常就够了)
        // 很多 Running Title 是主标题的前半截
        if (cleanTitle.startsWith(cleanLine)) {
            return true;
        }

        return false;
    }

    // 综合判断
    public static boolean isNoise(String line, String mainTitle) {
        if (isNoise(line)) return true; // 调用之前的通用规则
        if (isRunningTitle(line, mainTitle)) return true; // 调用新的标题检查
        return false;
    }
    /**
     * 判断某一行是否是页眉/页脚噪音
     * @param line 原始行文本
     * @return true=噪音(应丢弃), false=正文
     */
    public static boolean isNoise(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false; // 空行保留，Markdown 需要空行分段
        }
        String content = line.trim();

        // 规则 A: 包含版权符号 ©
        if (content.contains("©") || content.contains("(c)")) {
            return true;
        }

        // 规则 B: 像是页码 (行长度 < 10 且符合页码格式)
        if (content.length() < 10 && PAGE_NUMBER_PATTERN.matcher(content).matches()) {
            return true;
        }

        // 规则 C: 典型的期刊引用格式 (必须同时包含 年份 和 期刊特征词)
        // 例如: "Chaos 19, 043104 (2009)"
        boolean hasYear = YEAR_PATTERN.matcher(content).matches();
        boolean hasJournal = JOURNAL_PATTERN.matcher(content).matches();
        boolean hasArticleId = ARTICLE_ID_PATTERN.matcher(content).matches();

        if (hasYear && (hasJournal || hasArticleId)) {
            return true;
        }

        // 规则 D: 单独的 Article ID (如 "043104-1")
        if (hasArticleId && content.length() < 20) {
            return true;
        }

        // 规则 E: 针对 LlamaParse 的特殊处理
        // LlamaParse 经常把页眉识别成标题 (## ...)，如果这个标题里包含期刊信息，那肯定是假的
        if (content.startsWith("#") && (hasJournal || hasArticleId)) {
            return true;
        }

        return false;
    }

}