package cn.winddol.ai.domain.paper.service;

import cn.winddol.ai.domain.paper.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paper.model.aggregate.SearchResultDTO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class HybridRetrieverService {
    @Resource
    private IPaperRepository repository;
    @Resource
    private EmbeddingModel embeddingModel;

    private static final double SIMILARITY_THRESHOLD = 0.60;
    /*
       searchLibrary
       @param query 查询的语句
       根据查询的语句获得相似度前5的符号和段落
     */
    public SearchResultDTO searchLibrary(String query){
        SearchResultDTO resultDTO = new SearchResultDTO();
        if (query.length() < 10 && !query.contains(" ")) {
            List<SearchResultDTO.SymbolDTO> exactMatches = repository.searchSymbolsByKeyword(query);
            if (!exactMatches.isEmpty()) {
                log.info("Exact keyword match found for: {}", query);
                resultDTO.setSymbols(exactMatches);
            }
        }
        float[] queryVector = embeddingModel.embed(query).content().vector();
        String vector = Arrays.toString(queryVector);

        List<SearchResultDTO.SectionDTO> sections = repository.searchSectionsByVector(vector,5)
                .stream().filter(s -> s.getScore() != null && s.getScore() > SIMILARITY_THRESHOLD)
                .toList();
        resultDTO.setSections(sections);
        if (resultDTO.getSymbols() == null || resultDTO.getSymbols().isEmpty()) {
            List<SearchResultDTO.SymbolDTO> symbols = repository.searchSymbolsByVector(vector, 5)
                    .stream().filter(s -> s.getScore() != null && s.getScore() > SIMILARITY_THRESHOLD)
                    .toList();
            resultDTO.setSymbols(symbols);
        }
        return resultDTO;
    }
}
