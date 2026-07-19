package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.retrieval.SectionChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class StructuredSectionChunker {

    // Chunk 边界或重叠策略发生变化时必须升级该版本号，便于识别和重建旧索引。
    public static final String VERSION = "section-chunker-v1";

    private final int maxChars;
    private final int overlapChars;

    public StructuredSectionChunker(
            @Value("${paper.retrieval.chunk.max-chars:3200}") int maxChars,
            @Value("${paper.retrieval.chunk.overlap-chars:400}") int overlapChars) {
        this.maxChars = Math.max(800, maxChars);
        this.overlapChars = Math.max(0, Math.min(overlapChars, this.maxChars / 3));
    }

    /**
     * 在单个章节范围内生成可重复构建的检索 Chunk，并携带章节标题路径和版本信息。
     */
    public List<SectionChunk> chunk(SectionEntity section, String headingPath) {
        if (section == null || section.getContent() == null || section.getContent().isBlank()) {
            return List.of();
        }

        List<String> units = splitIntoUnits(section.getContent());
        List<String> texts = assembleChunks(units);
        List<SectionChunk> chunks = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            String content = texts.get(i).trim();
            String hash = sha256(content);
            // 使用章节、序号和内容哈希生成稳定 ID，保证相同内容重建后仍得到同一个 Chunk。
            String stableKey = section.getId() + '|' + i + '|' + hash;
            chunks.add(SectionChunk.builder()
                    .id(UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString())
                    .paperId(section.getPaperId())
                    .sectionId(section.getId())
                    .chunkIndex(i)
                    .heading(section.getHeader())
                    .headingPath(headingPath)
                    .content(content)
                    .tokenCount(estimateTokens(content))
                    .contentHash(hash)
                    .chunkerVersion(VERSION)
                    .build());
        }
        return chunks;
    }

    /**
     * 将章节正文优先按段落拆成语义单元，超长段落再继续细分。
     */
    private List<String> splitIntoUnits(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        String[] paragraphs = normalized.split("\n\\s*\n+");
        List<String> units = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String clean = paragraph.trim();
            if (clean.isEmpty()) {
                continue;
            }
            if (clean.length() <= maxChars) {
                units.add(clean);
            } else {
                // 优先保留段落边界，仅在单个段落过长时继续按句子拆分。
                units.addAll(splitLongParagraph(clean));
            }
        }
        return units;
    }

    /**
     * 按句子边界拆分超长段落；无法控制长度的超长单句最终按字符硬切。
     */
    private List<String> splitLongParagraph(String paragraph) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(paragraph);
        List<String> sentences = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = paragraph.substring(start, end).trim();
            if (sentence.length() <= maxChars) {
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                continue;
            }
            for (int offset = 0; offset < sentence.length(); offset += maxChars) {
                sentences.add(sentence.substring(offset, Math.min(sentence.length(), offset + maxChars)));
            }
        }
        return sentences;
    }

    /**
     * 将语义单元组装为不超过长度上限且包含相邻重叠上下文的 Chunk。
     */
    private List<String> assembleChunks(List<String> units) {
        List<String> result = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentLength = 0;

        for (String unit : units) {
            int addition = unit.length() + (current.isEmpty() ? 0 : 2);
            if (!current.isEmpty() && currentLength + addition > maxChars) {
                result.add(String.join("\n\n", current));
                // 将当前 Chunk 的末尾作为下一个 Chunk 的开头，保留边界附近的上下文。
                current = overlapTail(current);
                currentLength = joinedLength(current);
                addition = unit.length() + (current.isEmpty() ? 0 : 2);
                if (!current.isEmpty() && currentLength + addition > maxChars) {
                    // 重叠上下文可以舍弃，但当前待加入的正文单元不能丢失。
                    current.clear();
                    currentLength = 0;
                }
            }
            current.add(unit);
            currentLength += unit.length() + (current.size() == 1 ? 0 : 2);
        }
        if (!current.isEmpty()) {
            String finalChunk = String.join("\n\n", current);
            if (result.isEmpty() || !result.get(result.size() - 1).equals(finalChunk)) {
                result.add(finalChunk);
            }
        }
        return result;
    }

    /**
     * 从前一个 Chunk 尾部提取受限长度的上下文，作为下一个 Chunk 的起点。
     */
    private List<String> overlapTail(List<String> previous) {
        if (overlapChars == 0) {
            return new ArrayList<>();
        }
        List<String> tail = new ArrayList<>();
        int length = 0;
        for (int i = previous.size() - 1; i >= 0; i--) {
            String unit = previous.get(i);
            if (tail.isEmpty() && unit.length() > overlapChars) {
                tail.add(unit.substring(unit.length() - overlapChars));
                break;
            }
            if (!tail.isEmpty() && length + unit.length() > overlapChars) {
                break;
            }
            tail.add(0, unit);
            length += unit.length();
            if (length >= overlapChars) {
                break;
            }
        }
        return tail;
    }

    /**
     * 计算多个语义单元使用双换行连接后的字符长度。
     */
    private int joinedLength(List<String> units) {
        return units.stream().mapToInt(String::length).sum() + Math.max(0, units.size() - 1) * 2;
    }

    /**
     * 以四个字符约等于一个 Token 的方式生成近似统计值。
     */
    private int estimateTokens(String text) {
        // 这里只用于记录近似元数据，不代表模型 tokenizer 的真实 Token 数量限制。
        return Math.max(1, (text.length() + 3) / 4);
    }

    /**
     * 计算正文哈希，用于生成稳定 Chunk ID 和识别内容变化。
     */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
