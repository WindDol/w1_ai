package cn.winddol.ai.infrastructure.parser;

import java.util.regex.Pattern;

public class PaperCleaner {

    // 1. 年份模式 (1900-2099)
    // 使用 \\b 确保匹配完整的年份单词
    private static final Pattern YEAR_PATTERN = Pattern.compile(".*\\b(19|20)\\d{2}\\b.*");

    // 2. [增强] 期刊/出版商关键词
    // 新增: Phys, Math, Theor, Rev, Lett, Proc, Conf, IEEE, ACM, Springer, Elsevier
    private static final Pattern JOURNAL_PATTERN = Pattern.compile(
            ".*\\b(Chaos|Vol\\.|No\\.|pp\\.|doi|ISSN|ISBN|" +
                    "Phys\\.|Math\\.|Theor\\.|Rev\\.|Lett\\.|Proc\\.|Conf\\.|" + // 常见缩写
                    "Physics|Institute|University|Society|Journal|Transactions|" +
                    "IEEE|ACM|Springer|Elsevier|Nature|Science)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // 3. [新增] 作者列表特征 (et al)
    // 页眉常出现: "Smith et al"
    private static final Pattern ET_AL_PATTERN = Pattern.compile(".*\\b(et\\s+al)\\.?.*", Pattern.CASE_INSENSITIVE);

    // 4. 页码模式 (数字 或 "Page X")
    // 允许: "8", "Page 8", "8 of 12"
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile(
            "^\\s*(Page\\s*)?(\\d+)(\\s*of\\s*\\d+)?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // 5. [增强] 论文编号/索引号 (Article ID)
    // 匹配: "055201", "043104-3"
    // 特征: 5-7位连续数字，可能带连字符
    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile(".*\\b\\d{5,7}(-\\d+)?\\b.*");

    // 6. 版权符号
    private static final Pattern COPYRIGHT_PATTERN = Pattern.compile(".*(©|\\(c\\)|Copyright|Downloaded from|Rights reserved).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CITATION_START_PATTERN = Pattern.compile("^\\s*(\\[\\d+]|\\d+\\.|\\(\\d+\\)).*");
    private static final Pattern MARKDOWN_NUMBERED_HEADER_PATTERN = Pattern.compile(
            "^\\s*#{1,6}\\s+(?:\\d+(?:\\.\\d+)*|[IVXLCDM]+|[A-Z])\\.\\s+.+$"
    );
    private static final Pattern MARKDOWN_STANDARD_HEADER_PATTERN = Pattern.compile(
            "^\\s*#{1,6}\\s+(abstract|references|acknowledg(?:e)?ments?|data availability|appendix\\b.*)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final int MAX_HEADER_LENGTH = 120;

    /**
     * 判断某一行是否是页眉/页脚噪音
     */
    public static boolean isNoise(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false; // 保留空行用于Markdown分段
        }
        String content = line.trim();
        if (CITATION_START_PATTERN.matcher(content).matches()) {
            return false;
        }
        if (isStructuralMarkdownHeader(content)) {
            return false;
        }
        if (content.length() > MAX_HEADER_LENGTH) {
            return false;
        }
        // --- 规则 1: 绝对噪音 (版权、下载信息) ---
        if (COPYRIGHT_PATTERN.matcher(content).matches()) {
            return true;
        }

        // --- 规则 2: 纯页码 ---
        // 你的例子: "8"
        // 限制长度<20，防止误伤正文里的数字开头句子
        if (content.length() < 20 && PAGE_NUMBER_PATTERN.matcher(content).matches()) {
            return true;
        }

        // --- 预计算匹配结果 (提升可读性) ---
        boolean hasYear = YEAR_PATTERN.matcher(content).matches();
        boolean hasJournal = JOURNAL_PATTERN.matcher(content).matches();
        boolean hasEtAl = ET_AL_PATTERN.matcher(content).matches();
        boolean hasArticleId = ARTICLE_ID_PATTERN.matcher(content).matches();

        // --- 规则 3: 典型的学术页眉组合 ---

        // 组合 A: 年份 + (期刊名 或 et al)
        // 你的例子: "J. Phys... 2024... et al" 满足此规则
        if (hasYear && (hasJournal || hasEtAl)) {
            return true;
        }

        // 组合 B: 期刊名 + 文章ID (有些页眉没有年份)
        if (hasJournal && hasArticleId) {
            return true;
        }

        // 组合 C: 期刊名 + et al
        if (hasJournal && hasEtAl) {
            return true;
        }

        // --- 规则 4: LlamaParse 特有的 Markdown 误判 ---
        // 如果一行以 "#" 开头(被误认为标题)，但包含明显的期刊/作者噪音
        if (content.startsWith("#")) {
            return hasJournal || hasArticleId || hasEtAl;
        }

        return false;
    }

    private static boolean isStructuralMarkdownHeader(String content) {
        return MARKDOWN_NUMBERED_HEADER_PATTERN.matcher(content).matches()
                || MARKDOWN_STANDARD_HEADER_PATTERN.matcher(content).matches();
    }
}
