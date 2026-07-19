package cn.winddol.ai.paper.domain.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperEvidence {
    private String evidenceKey;
    private EvidenceType evidenceType;
    private Long paperId;
    private String paperTitle;
    private String sectionId;
    private String chunkId;
    private String heading;
    private String headingPath;
    private Integer pageStart;
    private Integer pageEnd;
    private String quote;
    private String referenceIndex;
    private Double semanticScore;
    private Double lexicalScore;
    private Double fusionScore;
    private Double rerankScore;
    private Set<RetrievalChannel> matchedChannels;
}
