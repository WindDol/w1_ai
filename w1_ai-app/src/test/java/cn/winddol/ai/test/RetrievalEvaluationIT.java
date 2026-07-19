package cn.winddol.ai.test;

import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.api.IPaperRetrievalService;
import cn.winddol.ai.paper.domain.retrieval.PaperEvidence;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalQuery;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
class RetrievalEvaluationIT {

    @Autowired
    private IPaperRetrievalService retrievalService;
    @Autowired
    private IPaperRepository paperRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void evaluateVersionedRetrievalDataset() throws Exception {
        List<EvaluationCase> cases = loadCases();
        List<Integer> ranks = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        String retrievalVersion = null;

        for (EvaluationCase item : cases) {
            if (paperRepository.selectPaperById(item.paperId()) == null) {
                throw new IllegalStateException(
                        "Evaluation paper is not in the library: paperId=" + item.paperId());
            }
            PaperRetrievalResult result = retrievalService.retrieve(
                    new PaperRetrievalQuery(item.question(), item.paperId(), 0.4, 10));
            retrievalVersion = result.retrievalVersion();
            latencies.add(result.elapsedMillis());
            int rank = relevantRank(result.evidence(), item.expectedHeading());
            ranks.add(rank);
            System.out.printf("%s rank=%s expected=%s%n", item.id(),
                    rank == 0 ? "MISS" : rank, item.expectedHeading());
        }

        int total = ranks.size();
        double recall5 = ranks.stream().filter(rank -> rank > 0 && rank <= 5).count() / (double) total;
        double recall10 = ranks.stream().filter(rank -> rank > 0 && rank <= 10).count() / (double) total;
        double mrr10 = ranks.stream()
                .mapToDouble(rank -> rank > 0 && rank <= 10 ? 1.0 / rank : 0.0)
                .average().orElse(0.0);
        double ndcg10 = ranks.stream()
                .mapToDouble(rank -> rank > 0 && rank <= 10 ? 1.0 / log2(rank + 1) : 0.0)
                .average().orElse(0.0);
        latencies.sort(Comparator.naturalOrder());

        System.out.println("--- ScholarBrain Retrieval Evaluation ---");
        System.out.printf("version=%s dataset=retrieval-eval-v1 cases=%d%n",
                retrievalVersion, total);
        System.out.printf("Recall@5=%.4f Recall@10=%.4f MRR@10=%.4f nDCG@10=%.4f%n",
                recall5, recall10, mrr10, ndcg10);
        System.out.printf("P50=%dms P95=%dms%n", percentile(latencies, 0.50), percentile(latencies, 0.95));
    }

    private int relevantRank(List<PaperEvidence> evidence, String expectedHeading) {
        String expected = normalize(expectedHeading);
        for (int index = 0; index < evidence.size(); index++) {
            PaperEvidence candidate = evidence.get(index);
            if (normalize(candidate.getHeading()).contains(expected)
                    || normalize(candidate.getHeadingPath()).contains(expected)) {
                return index + 1;
            }
        }
        return 0;
    }

    private List<EvaluationCase> loadCases() throws Exception {
        InputStream input = getClass().getResourceAsStream("/retrieval/retrieval-eval-v1.jsonl");
        assertNotNull(input);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            List<EvaluationCase> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    result.add(objectMapper.readValue(line, EvaluationCase.class));
                }
            }
            return result;
        }
    }

    private long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile * values.size()) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }

    private double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private record EvaluationCase(
            String id,
            String type,
            String question,
            Long paperId,
            String expectedHeading
    ) {
    }
}
