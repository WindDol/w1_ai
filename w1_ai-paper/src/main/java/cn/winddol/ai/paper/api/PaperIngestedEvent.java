package cn.winddol.ai.paper.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;

@Data
@AllArgsConstructor
public class PaperIngestedEvent {

    private String jobId;
    private Long paperId;
    private String title;
    private PaperIngestStage startStage;
    private int attempt;
}
