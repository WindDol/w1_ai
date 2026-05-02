package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperDetailVO {
    private Long id;
    private String title;
    private String abstractText;

    private String noveltyAssessment;

    private Integer symbolCount;
    private Integer referenceCount;

    private List<SymbolEntity> keySymbols;
    private List<String> relatedPaperTitles;
}
