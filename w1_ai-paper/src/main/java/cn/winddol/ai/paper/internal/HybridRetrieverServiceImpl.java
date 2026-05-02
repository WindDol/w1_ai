package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IEmbeddingService;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.SearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class HybridRetrieverServiceImpl {

    private final IPaperRepository repository;
    private final IEmbeddingService embeddingService;

    private static final double DEFAULT_THRESHOLD = 0.40;
    private static final double MIN_THRESHOLD = 0.35;
    private static final double MAX_THRESHOLD = 0.70;

    public HybridRetrieverServiceImpl(IPaperRepository repository,
                                      IEmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public SearchResultDTO searchLibrary(String query, Long paperId, Double customThreshold) {
        SearchResultDTO resultDTO = new SearchResultDTO();

        double threshold = DEFAULT_THRESHOLD;
        if (customThreshold != null) {
            threshold = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, customThreshold));
        }
        log.info("Searching with threshold: {}", threshold);

        if (query.length() < 10 && !query.contains(" ")) {
            List<SearchResultDTO.SymbolDTO> exactMatches = repository.searchSymbolsByKeyword(query, paperId);
            if (!exactMatches.isEmpty()) {
                log.info("Exact keyword match found for: {}", query);
                resultDTO.setSymbols(exactMatches);
            }
        }
        float[] queryVector = embeddingService.embed(query);
        String vector = Arrays.toString(queryVector);
        double finalThreshold = threshold;
        List<SearchResultDTO.SectionDTO> sections = repository.searchSectionsByVector(paperId, vector, 5)
                .stream().filter(s -> s.getScore() != null && s.getScore() > finalThreshold)
                .toList();
        resultDTO.setSections(sections);
        if (resultDTO.getSymbols() == null || resultDTO.getSymbols().isEmpty()) {
            List<SearchResultDTO.SymbolDTO> symbols = repository.searchSymbolsByVector(paperId, vector, 5)
                    .stream().filter(s -> s.getScore() != null && s.getScore() > finalThreshold)
                    .toList();
            resultDTO.setSymbols(symbols);
        }
        List<SearchResultDTO.ReferenceDTO> references = repository.searchReferencesByVector(paperId, vector, 3)
                .stream()
                .filter(r -> r.getScore() != null && r.getScore() > finalThreshold)
                .toList();
        resultDTO.setReferences(references);
        return resultDTO;
    }

    public SearchResultDTO searchLibrary(String query, Double customThreshold) {
        return searchLibrary(query, null, customThreshold);
    }
}
