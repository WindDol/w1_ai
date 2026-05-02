package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDTO {

    private List<SectionDTO> sections;
    private List<SymbolDTO> symbols;
    private List<ReferenceDTO> references;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SectionDTO {
        private String id;
        private Long paperId;
        private String header;
        private String content;
        private String parentId;
        private Integer idx;
        private Double score;
        private String sourcePaperTitle;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SymbolDTO {
        private Long id;
        private Long paperId;
        private String symbol;
        private String description;
        private String definitionFormula;
        private Double score;
        private String sourcePaperTitle;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ReferenceDTO {
        private Long id;
        private Long globalId;
        private String refId;
        private Long paperId;
        private String sourcePaperTitle;
        private String title;
        private String abstractText;
        private Double score;
        private Long linkedPaperId;
        private Integer citationCount;
    }
}
