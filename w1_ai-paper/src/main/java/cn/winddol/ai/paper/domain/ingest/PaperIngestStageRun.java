package cn.winddol.ai.paper.domain.ingest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 一次摄取阶段的可审计执行记录。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperIngestStageRun {
    private Long id;
    private String jobId;
    private PaperIngestStage stage;
    private Integer attempt;
    private IngestStageRunStatus status;
    private String artifactPath;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
