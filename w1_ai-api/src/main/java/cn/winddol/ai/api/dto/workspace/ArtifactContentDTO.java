package cn.winddol.ai.api.dto.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactContentDTO {
    private String type;
    private String label;
    private String content;
    private boolean truncated;
}
