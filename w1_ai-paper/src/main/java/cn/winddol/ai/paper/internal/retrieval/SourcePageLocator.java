package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.domain.retrieval.SourceTextBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * 尽可能将归一化章节文本映射回 MonkeyOCR 文本块中的页码。
 * OCR 文本缺失或匹配失败时返回未知页码，但不应阻断论文摄取流程。
 */
final class SourcePageLocator {

    private final String sourceText;
    private final List<PageSpan> pageSpans;
    private int searchOffset;

    /**
     * 将按页排列的 OCR 文本块拼接为可搜索文本，并记录每一页的字符区间。
     */
    SourcePageLocator(List<SourceTextBlock> blocks) {
        StringBuilder source = new StringBuilder();
        List<PageSpan> spans = new ArrayList<>();
        for (SourceTextBlock block : blocks) {
            String text = normalize(block.text());
            if (text.isEmpty()) {
                continue;
            }
            int start = source.length();
            if (start > 0) {
                source.append(' ');
                start++;
            }
            source.append(text);
            spans.add(new PageSpan(start, source.length(), block.pageIndex() + 1));
        }
        this.sourceText = source.toString();
        this.pageSpans = spans;
    }

    /**
     * 根据 Chunk 正文在 OCR 文本中的位置推算起止页；匹配失败时返回未知页码。
     */
    PageLocation locate(String content) {
        String normalized = normalize(content);
        if (normalized.length() < 24 || sourceText.isEmpty()) {
            return PageLocation.unknown();
        }

        int anchorLength = Math.min(180, normalized.length());
        String anchor = normalized.substring(0, anchorLength);
        // 从上一次命中位置之后继续查找，避免重复文本总是被映射到第一次出现的页面。
        int start = sourceText.indexOf(anchor, searchOffset);
        if (start < 0 && normalized.length() > 100) {
            anchor = normalized.substring(0, 100);
            start = sourceText.indexOf(anchor, searchOffset);
        }
        if (start < 0) {
            // 顺序查找失败后退回全文搜索，以容忍部分文本归一化差异。
            start = sourceText.indexOf(anchor);
        }
        if (start < 0) {
            return PageLocation.unknown();
        }

        searchOffset = Math.min(sourceText.length(), start + 1);
        int end = Math.min(sourceText.length(), start + normalized.length());
        return new PageLocation(pageAt(start), pageAt(Math.max(start, end - 1)));
    }

    /**
     * 将拼接文本中的字符偏移量转换成从 1 开始的 PDF 页码。
     */
    private Integer pageAt(int offset) {
        for (PageSpan span : pageSpans) {
            if (offset >= span.start() && offset < span.end()) {
                return span.page();
            }
        }
        return null;
    }

    /**
     * 压缩空白字符，降低 Markdown 与 OCR 文本格式差异对匹配的影响。
     */
    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    record PageLocation(Integer start, Integer end) {
        static PageLocation unknown() {
            return new PageLocation(null, null);
        }
    }

    private record PageSpan(int start, int end, int page) {
    }
}
