package cn.winddol.ai.paper.domain.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalCandidate {
    private String evidenceKey;
    private EvidenceType evidenceType;
    private Long paperId;
    private String paperTitle;
    private String sectionId;
    private String chunkId;
    private String heading;
    private String headingPath;
    private String content;
    private Integer pageStart;
    private Integer pageEnd;
    private String referenceIndex;
    private Double semanticScore;
    private Double lexicalScore;
    private Double fusionScore;
    @Builder.Default
    private Set<RetrievalChannel> matchedChannels = new LinkedHashSet<>();
}
