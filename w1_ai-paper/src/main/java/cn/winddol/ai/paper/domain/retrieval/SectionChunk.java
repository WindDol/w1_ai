package cn.winddol.ai.paper.domain.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionChunk {
    private String id;
    private Long paperId;
    private String sectionId;
    private Integer chunkIndex;
    private String heading;
    private String headingPath;
    private String content;
    private Integer pageStart;
    private Integer pageEnd;
    private Integer tokenCount;
    private String contentHash;
    private String chunkerVersion;
    private String embeddingModel;
    private float[] embedding;
}
