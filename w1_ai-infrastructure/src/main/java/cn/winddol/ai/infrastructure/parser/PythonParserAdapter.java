package cn.winddol.ai.infrastructure.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class PythonParserAdapter {
    @Value("${python.path-python}")
    private String pythonExecutable;

    @Value("${python.script-path}")
    private String scriptPath;

    public String parsePdfToMarkdown(String absolutePath) {
        log.info("🐍 Calling Python Parser for: {}", absolutePath);

        try {
            // 1. 构建命令: python paper_parser.py /path/to/pdf
            StringBuilder output = getStringBuilder(absolutePath);
            String rawOutput = output.toString().trim();
            log.info("🐍 Raw Python Output: {}", rawOutput);
            int jsonStartIndex = rawOutput.indexOf("{");
            if (jsonStartIndex == -1) {
                throw new RuntimeException("No JSON object found in Python output: " + rawOutput);
            }
            // 截取真正的 JSON 部分
            String jsonStr = rawOutput.substring(jsonStartIndex);
            // 3. 解析 Python 返回的 JSON
            JSONObject jsonResponse = JSON.parseObject(jsonStr);
            if ("success".equals(jsonResponse.getString("status"))) {
                return jsonResponse.getString("content");
            } else {
                throw new RuntimeException("Parser Error: " + jsonResponse.getString("message"));
            }

        } catch (Exception e) {
            log.error("🔥 Failed to execute python parser", e);
            throw new RuntimeException("PDF Parsing system error", e);
        }
    }

    @NotNull
    private StringBuilder getStringBuilder(String absolutePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(pythonExecutable, scriptPath, absolutePath);
        pb.redirectErrorStream(true); // 合并错误流到标准输出
        Map<String, String> env = pb.environment();
        env.put("PYTHONIOENCODING", "UTF-8");
        Process process = pb.start();

        // 2. 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script failed with exit code " + exitCode + ". Output: " + output);
        }
        return output;
    }
}
