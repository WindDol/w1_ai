package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.domain.SearchResultDTO;
import cn.winddol.ai.paper.api.IEmbeddingService;
import cn.winddol.ai.paper.api.IPaperRepository;
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
    private static final int TOP_K = 5;

    public HybridRetrieverServiceImpl(IPaperRepository repository, IEmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public SearchResultDTO searchLibrary(String query, Long paperId, Double threshold) {
        double actualThreshold = (threshold != null) ? threshold : DEFAULT_THRESHOLD;
        if (actualThreshold < MIN_THRESHOLD) actualThreshold = MIN_THRESHOLD;

        float[] vector = embeddingService.embed(query);
        String vectorStr = Arrays.toString(vector);

        List<SearchResultDTO.SectionDTO> sections = repository.searchSectionsByVector(paperId, vectorStr, TOP_K);
        List<SearchResultDTO.SymbolDTO> symbols = repository.searchSymbolsByVector(paperId, vectorStr, TOP_K);
        List<SearchResultDTO.ReferenceDTO> references = repository.searchReferencesByVector(paperId, vectorStr, TOP_K);

        SearchResultDTO result = new SearchResultDTO();
        result.setSections(sections);
        result.setSymbols(symbols);
        result.setReferences(references);
        return result;
    }
}
