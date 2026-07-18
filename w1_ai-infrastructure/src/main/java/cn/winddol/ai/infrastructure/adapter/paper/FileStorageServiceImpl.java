package cn.winddol.ai.infrastructure.adapter.paper;

import cn.winddol.ai.paper.api.IFileStorageService;
import cn.winddol.ai.paper.domain.ingest.StoredPaperFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;

@Service
public class FileStorageServiceImpl implements IFileStorageService {

    @Value("${upload.path:./temp_pdf/}")
    private String uploadPath;

    @Override
    public StoredPaperFile storeUploadedFile(String jobId, MultipartFile file) throws IOException {
        Path jobDirectory = resolveJobDirectory(jobId);
        Files.createDirectories(jobDirectory);
        Path source = jobDirectory.resolve("source.pdf").normalize();

        MessageDigest digest = sha256();
        long size;
        try (InputStream input = file.getInputStream();
             var digestInput = new java.security.DigestInputStream(input, digest)) {
            size = Files.copy(digestInput, source, StandardCopyOption.REPLACE_EXISTING);
        }
        return new StoredPaperFile(source.toString(), HexFormat.of().formatHex(digest.digest()), size);
    }

    @Override
    public File resolveFile(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Stored file path must not be blank");
        }
        return Path.of(path).toAbsolutePath().normalize().toFile();
    }

    @Override
    public String writeTextArtifact(String jobId, String filename, String content) throws IOException {
        Path jobDirectory = resolveJobDirectory(jobId);
        Files.createDirectories(jobDirectory);
        Path target = jobDirectory.resolve(filename).normalize();
        if (!target.startsWith(jobDirectory)) {
            throw new IOException("Invalid artifact filename: " + filename);
        }
        Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
        return target.toString();
    }

    @Override
    public String readTextArtifact(String path) throws IOException {
        return Files.readString(resolveFile(path).toPath(), StandardCharsets.UTF_8);
    }

    @Override
    public void deleteJobFiles(String jobId) {
        Path directory = resolveJobDirectory(jobId);
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Duplicate upload cleanup is best effort; the persisted job is unaffected.
                }
            });
        } catch (IOException ignored) {
            // See comment above.
        }
    }

    private Path resolveJobDirectory(String jobId) {
        if (jobId == null || !jobId.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid ingestion job id");
        }
        Path root = Path.of(uploadPath).toAbsolutePath().normalize();
        Path jobDirectory = root.resolve(jobId).normalize();
        if (!jobDirectory.startsWith(root)) {
            throw new IllegalArgumentException("Invalid ingestion job path");
        }
        return jobDirectory;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
