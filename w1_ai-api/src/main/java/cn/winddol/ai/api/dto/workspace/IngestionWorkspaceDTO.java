package cn.winddol.ai.api.dto.workspace;

import cn.winddol.ai.api.dto.PaperIngestJobDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionWorkspaceDTO {
    private PaperIngestJobDTO job;
    @Builder.Default
    private List<IngestStageRunDTO> stageRuns = List.of();
    @Builder.Default
    private List<ArtifactSummaryDTO> artifacts = List.of();
}
