package cn.winddol.ai.paper.domain.workspace;

import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStageRun;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 上传任务时间线和可查看产物。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionWorkspaceView {
    private PaperIngestJob job;
    @Builder.Default
    private List<PaperIngestStageRun> stageRuns = List.of();
    @Builder.Default
    private List<ArtifactSummary> artifacts = List.of();
}
