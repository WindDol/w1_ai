package cn.winddol.ai.api.dto.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactSummaryDTO {
    private String type;
    private String label;
    private boolean available;
}
