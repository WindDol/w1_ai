package cn.winddol.ai.infrastructure.adapter.parser;

import cn.winddol.ai.paper.api.IParserArtifactReader;
import cn.winddol.ai.paper.domain.retrieval.SourceTextBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Component
public class MonkeyOcrArtifactReader implements IParserArtifactReader {

    private final ObjectMapper objectMapper;

    public MonkeyOcrArtifactReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 根据 artifact 实际形态读取 MonkeyOCR 文本块，读取失败时降级为空列表。
     */
    @Override
    public List<SourceTextBlock> readTextBlocks(String parserArtifactPath) {
        if (parserArtifactPath == null || parserArtifactPath.isBlank()) {
            return List.of();
        }
        Path artifact = Path.of(parserArtifactPath);
        if (!Files.exists(artifact)) {
            log.warn("Parser artifact does not exist, page location will be unavailable: {}", artifact);
            return List.of();
        }

        try {
            // 摄取任务可能保存解压目录、下载后的 ZIP，或者单独的 content_list.json。
            if (Files.isDirectory(artifact)) {
                return readDirectory(artifact);
            }
            if (artifact.getFileName().toString().toLowerCase().endsWith(".zip")) {
                return readZip(artifact);
            }
            if (artifact.getFileName().toString().endsWith("_content_list.json")) {
                try (InputStream input = Files.newInputStream(artifact)) {
                    return parse(input);
                }
            }
        } catch (Exception error) {
            // 页码归属属于增强信息，读取失败不能让原本可用的 Chunk 失效。
            log.warn("Unable to read parser page metadata from {}", artifact, error);
        }
        return List.of();
    }

    /**
     * 从解析产物目录中递归查找并读取 content_list.json。
     */
    private List<SourceTextBlock> readDirectory(Path directory) throws IOException {
        try (var files = Files.walk(directory)) {
            Optional<Path> contentList = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("_content_list.json"))
                    .findFirst();
            if (contentList.isEmpty()) {
                return List.of();
            }
            try (InputStream input = Files.newInputStream(contentList.get())) {
                return parse(input);
            }
        }
    }

    /**
     * 从解析产物 ZIP 中查找并读取 content_list.json，不在本地解压文件。
     */
    private List<SourceTextBlock> readZip(Path zipPath) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry selected = null;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith("_content_list.json")) {
                    selected = entry;
                    break;
                }
            }
            if (selected == null) {
                return List.of();
            }
            try (InputStream input = zip.getInputStream(selected)) {
                return parse(input);
            }
        }
    }

    /**
     * 将 MonkeyOCR content_list.json 转换为按原始顺序排列的页码文本块。
     */
    private List<SourceTextBlock> parse(InputStream input) throws IOException {
        JsonNode root = objectMapper.readTree(input);
        if (!root.isArray()) {
            return List.of();
        }
        List<SourceTextBlock> blocks = new ArrayList<>();
        for (JsonNode node : root) {
            // 当前无法可靠地把图片、表格等非文本块匹配到归一化章节正文，因此只读取文本块。
            if (!"text".equals(node.path("type").asText()) || !node.hasNonNull("text")) {
                continue;
            }
            blocks.add(new SourceTextBlock(node.path("text").asText(), node.path("page_idx").asInt(0)));
        }
        return blocks;
    }
}
