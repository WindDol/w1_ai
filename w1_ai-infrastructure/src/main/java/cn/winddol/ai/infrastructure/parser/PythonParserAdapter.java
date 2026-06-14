package cn.winddol.ai.infrastructure.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Slf4j
public class PythonParserAdapter {
    private static final String PARSER_LLAMA = "llama";
    private static final String PARSER_LEGACY = "legacy";
    private static final String PARSER_MONKEYOCR = "monkeyocr";
    private static final String PARSER_MONKEYOCR_HTTP = "monkeyocr-http";

    @Value("${python.parser-type:llama}")
    private String parserType;

    @Value("${python.path-python:python}")
    private String pythonExecutable;

    @Value("${python.script-path:python/paper_parser.py}")
    private String scriptPath;

    @Value("${python.monkeyocr.python-path:}")
    private String monkeyOcrPythonExecutable;

    @Value("${python.monkeyocr.repo-path:}")
    private String monkeyOcrRepoPath;

    @Value("${python.monkeyocr.script-path:parse.py}")
    private String monkeyOcrScriptPath;

    @Value("${python.monkeyocr.output-dir:data/monkeyocr-output}")
    private String monkeyOcrOutputDir;

    @Value("${python.monkeyocr.config-path:}")
    private String monkeyOcrConfigPath;

    @Value("${python.monkeyocr.timeout-seconds:1800}")
    private long monkeyOcrTimeoutSeconds;

    @Value("${python.monkeyocr.omp-num-threads:1}")
    private String monkeyOcrOmpNumThreads;

    @Value("${python.monkeyocr.base-url:http://127.0.0.1:8000}")
    private String monkeyOcrBaseUrl;

    @Value("${python.monkeyocr.http-output-dir:data/monkeyocr-http-output}")
    private String monkeyOcrHttpOutputDir;

    @Value("${python.monkeyocr.http-timeout-seconds:1800}")
    private long monkeyOcrHttpTimeoutSeconds;

    public String parsePdfToMarkdown(String absolutePath) {
        String type = normalizeParserType(parserType);
        log.info("Calling PDF parser [{}] for: {}", type, absolutePath);

        if (PARSER_MONKEYOCR.equals(type)) {
            return parseWithMonkeyOcr(absolutePath);
        }
        if (PARSER_MONKEYOCR_HTTP.equals(type)) {
            return parseWithMonkeyOcrHttp(absolutePath);
        }
        return parseWithJsonScript(absolutePath);
    }

    private String parseWithJsonScript(String absolutePath) {
        try {
            List<String> command = List.of(pythonExecutable, scriptPath, absolutePath);
            String rawOutput = runProcess(command, null, 0, Map.of()).trim();
            log.info("Raw Python parser output: {}", rawOutput);
            int jsonStartIndex = rawOutput.indexOf("{");
            if (jsonStartIndex == -1) {
                throw new RuntimeException("No JSON object found in Python output: " + rawOutput);
            }
            String jsonStr = rawOutput.substring(jsonStartIndex);
            JSONObject jsonResponse = JSON.parseObject(jsonStr);
            if ("success".equals(jsonResponse.getString("status"))) {
                return jsonResponse.getString("content");
            } else {
                throw new RuntimeException("Parser Error: " + jsonResponse.getString("message"));
            }

        } catch (Exception e) {
            log.error("Failed to execute JSON python parser", e);
            throw new RuntimeException("PDF Parsing system error", e);
        }
    }

    private String parseWithMonkeyOcr(String absolutePath) {
        Path pdfPath = Path.of(absolutePath).toAbsolutePath().normalize();
        if (!Files.exists(pdfPath)) {
            throw new RuntimeException("PDF file not found: " + pdfPath);
        }

        Path script = resolveMonkeyOcrScriptPath();
        Path workingDir = resolveMonkeyOcrWorkingDir(script);
        Path outputDir = Path.of(monkeyOcrOutputDir).toAbsolutePath().normalize();
        long startedAt = System.currentTimeMillis();

        try {
            Files.createDirectories(outputDir);

            List<String> command = new ArrayList<>();
            command.add(resolveMonkeyOcrPythonExecutable());
            command.add(script.toString());
            command.add(pdfPath.toString());
            command.add("-o");
            command.add(outputDir.toString());
            if (!isBlank(monkeyOcrConfigPath)) {
                command.add("--config");
                command.add(Path.of(monkeyOcrConfigPath).toAbsolutePath().normalize().toString());
            }

            String output = runProcess(command, workingDir, monkeyOcrTimeoutSeconds, Map.of(
                    "OMP_NUM_THREADS", isBlank(monkeyOcrOmpNumThreads) ? "1" : monkeyOcrOmpNumThreads
            ));
            log.info("MonkeyOCR process completed. Output: {}", output);

            Path markdown = findMonkeyOcrMarkdown(outputDir, pdfPath, startedAt);
            log.info("MonkeyOCR markdown output: {}", markdown);
            return Files.readString(markdown, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to execute MonkeyOCR parser", e);
            throw new RuntimeException("MonkeyOCR PDF parsing system error", e);
        }
    }

    private String parseWithMonkeyOcrHttp(String absolutePath) {
        Path pdfPath = Path.of(absolutePath).toAbsolutePath().normalize();
        if (!Files.exists(pdfPath)) {
            throw new RuntimeException("PDF file not found: " + pdfPath);
        }
        if (isBlank(monkeyOcrBaseUrl)) {
            throw new RuntimeException("MonkeyOCR HTTP base URL is empty");
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(monkeyOcrHttpTimeoutSeconds))
                    .build();

            JSONObject parseResponse = uploadPdfToMonkeyOcr(client, pdfPath);
            String downloadUrl = parseResponse.getString("download_url");
            if (isBlank(downloadUrl)) {
                throw new RuntimeException("MonkeyOCR HTTP response missing download_url: " + parseResponse);
            }

            byte[] zipBytes = downloadMonkeyOcrZip(client, downloadUrl);
            Path outputDir = extractMonkeyOcrZip(zipBytes, pdfPath);
            Path markdown = findMarkdownInDirectory(outputDir, pdfPath);
            log.info("MonkeyOCR HTTP markdown output: {}", markdown);
            return Files.readString(markdown, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to execute MonkeyOCR HTTP parser", e);
            throw new RuntimeException("MonkeyOCR HTTP PDF parsing system error", e);
        }
    }

    private JSONObject uploadPdfToMonkeyOcr(HttpClient client, Path pdfPath) throws IOException, InterruptedException {
        String boundary = "----ScholarBrainMonkeyOCR" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, pdfPath);


        HttpRequest request = HttpRequest.newBuilder(resolveMonkeyOcrUri("/parse"))
                .timeout(Duration.ofSeconds(monkeyOcrHttpTimeoutSeconds))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("MonkeyOCR HTTP parse failed with status "
                    + response.statusCode() + ": " + response.body());
        }

        JSONObject json = JSON.parseObject(response.body());
        if (!json.getBooleanValue("success")) {
            throw new RuntimeException("MonkeyOCR HTTP parse failed: " + response.body());
        }
        return json;
    }

    private byte[] buildMultipartBody(String boundary, Path pdfPath) throws IOException {
        String filename = pdfPath.getFileName().toString().replace("\"", "");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write("Content-Type: application/pdf\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(Files.readAllBytes(pdfPath));
        output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private byte[] downloadMonkeyOcrZip(HttpClient client, String downloadUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(resolveMonkeyOcrUri(downloadUrl))
                .timeout(Duration.ofSeconds(monkeyOcrHttpTimeoutSeconds))
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("MonkeyOCR zip download failed with status " + response.statusCode());
        }
        return response.body();
    }

    private Path extractMonkeyOcrZip(byte[] zipBytes, Path pdfPath) throws IOException {
        String stem = stripExtension(pdfPath.getFileName().toString());
        Path outputRoot = Path.of(monkeyOcrHttpOutputDir).toAbsolutePath().normalize();
        Path outputDir = outputRoot.resolve(stem + "-" + System.currentTimeMillis()).normalize();
        Files.createDirectories(outputDir);

        try (ZipInputStream zipInput = new ZipInputStream(
                new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                Path target = outputDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(outputDir)) {
                    throw new RuntimeException("Unsafe MonkeyOCR zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zipInput, target);
                }
                zipInput.closeEntry();
            }
        }
        return outputDir;
    }

    private URI resolveMonkeyOcrUri(String pathOrUrl) {
        URI uri = URI.create(pathOrUrl);
        if (uri.isAbsolute()) {
            return uri;
        }
        String base = monkeyOcrBaseUrl.endsWith("/") ? monkeyOcrBaseUrl : monkeyOcrBaseUrl + "/";
        String relative = pathOrUrl.startsWith("/") ? pathOrUrl.substring(1) : pathOrUrl;
        return URI.create(base).resolve(relative);
    }

    private Path findMarkdownInDirectory(Path outputDir, Path pdfPath) throws IOException {
        String stem = stripExtension(pdfPath.getFileName().toString());
        try (Stream<Path> stream = Files.walk(outputDir, 5)) {
            List<Path> markdownFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted(Comparator
                            .comparing((Path path) -> !path.getFileName().toString().equalsIgnoreCase(stem + ".md"))
                            .thenComparing(Path::toString))
                    .collect(Collectors.toList());
            if (!markdownFiles.isEmpty()) {
                return markdownFiles.get(0);
            }
        }
        throw new RuntimeException("MonkeyOCR zip did not contain markdown output under: " + outputDir);
    }

    private String runProcess(List<String> command,
                              Path workingDirectory,
                              long timeoutSeconds,
                              Map<String, String> extraEnvironment) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDirectory != null) {
            pb.directory(workingDirectory.toFile());
        }
        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("PYTHONIOENCODING", "UTF-8");
        extraEnvironment.forEach(env::put);
        Process process = pb.start();

        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process));
        boolean finished;
        if (timeoutSeconds <= 0) {
            process.waitFor();
            finished = true;
        } else {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Parser process timed out after " + timeoutSeconds + " seconds: " + command);
        }

        int exitCode = process.exitValue();
        String output = outputFuture.join();
        if (exitCode != 0) {
            throw new RuntimeException("Parser process failed with exit code " + exitCode + ". Output: " + output);
        }
        return output;
    }

    private String readOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read parser process output", e);
        }
    }

    private Path resolveMonkeyOcrScriptPath() {
        String configured = isBlank(monkeyOcrScriptPath) ? "parse.py" : monkeyOcrScriptPath;
        Path script = Path.of(configured);
        if (!script.isAbsolute()) {
            if (isBlank(monkeyOcrRepoPath)) {
                script = script.toAbsolutePath().normalize();
            } else {
                script = Path.of(monkeyOcrRepoPath).toAbsolutePath().normalize().resolve(script).normalize();
            }
        }
        if (!Files.exists(script)) {
            throw new RuntimeException("MonkeyOCR parse script not found: " + script);
        }
        return script;
    }

    private Path resolveMonkeyOcrWorkingDir(Path script) {
        if (!isBlank(monkeyOcrRepoPath)) {
            Path repo = Path.of(monkeyOcrRepoPath).toAbsolutePath().normalize();
            if (!Files.isDirectory(repo)) {
                throw new RuntimeException("MonkeyOCR repo path is not a directory: " + repo);
            }
            return repo;
        }
        return script.getParent();
    }

    private String resolveMonkeyOcrPythonExecutable() {
        return isBlank(monkeyOcrPythonExecutable) ? pythonExecutable : monkeyOcrPythonExecutable;
    }

    private Path findMonkeyOcrMarkdown(Path outputDir, Path pdfPath, long startedAt) throws IOException {
        String stem = stripExtension(pdfPath.getFileName().toString());
        Path nestedExpected = outputDir.resolve(stem).resolve(stem + ".md");
        if (isFreshFile(nestedExpected, startedAt)) {
            return nestedExpected;
        }
        Path flatExpected = outputDir.resolve(stem + ".md");
        if (isFreshFile(flatExpected, startedAt)) {
            return flatExpected;
        }

        try (Stream<Path> stream = Files.walk(outputDir, 5)) {
            List<Path> markdownFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".md"))
                    .filter(path -> isFreshFile(path, startedAt))
                    .sorted(Comparator
                            .comparing((Path path) -> !path.getFileName().toString().equalsIgnoreCase(stem + ".md"))
                            .thenComparing(path -> -lastModified(path)))
                    .collect(Collectors.toList());
            if (!markdownFiles.isEmpty()) {
                return markdownFiles.get(0);
            }
        }
        throw new RuntimeException("MonkeyOCR completed but no markdown output was found under: " + outputDir);
    }

    private boolean isFreshFile(Path path, long startedAt) {
        return Files.isRegularFile(path) && lastModified(path) >= startedAt - 5000;
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String normalizeParserType(String type) {
        String normalized = isBlank(type) ? PARSER_LLAMA : type.trim().toLowerCase();
        if (PARSER_LEGACY.equals(normalized)) {
            return PARSER_LLAMA;
        }
        if (!PARSER_LLAMA.equals(normalized)
                && !PARSER_MONKEYOCR.equals(normalized)
                && !PARSER_MONKEYOCR_HTTP.equals(normalized)) {
            throw new RuntimeException("Unsupported PDF parser type: " + type);
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
