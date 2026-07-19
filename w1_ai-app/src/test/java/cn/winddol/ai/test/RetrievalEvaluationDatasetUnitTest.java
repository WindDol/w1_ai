package cn.winddol.ai.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalEvaluationDatasetUnitTest {

    @Test
    void evaluationDatasetIsVersionedAndHasStableEvidenceLabels() throws Exception {
        InputStream resource = getClass().getResourceAsStream(
                "/retrieval/retrieval-eval-v1.jsonl");
        assertNotNull(resource);

        ObjectMapper objectMapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().filter(line -> !line.isBlank()).toList();
            assertTrue(lines.size() >= 8);
            for (String line : lines) {
                JsonNode item = objectMapper.readTree(line);
                assertFalse(item.path("id").asText().isBlank());
                assertFalse(item.path("question").asText().isBlank());
                assertTrue(item.path("paperId").canConvertToLong());
                assertTrue(item.path("paperId").asLong() > 0);
                assertFalse(item.path("expectedHeading").asText().isBlank());
                assertFalse(item.path("type").asText().isBlank());
            }
        }
    }
}
