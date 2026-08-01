package cn.winddol.ai.api.dto.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceDTO {
    private Long id;
    private String refId;
    private String rawText;
    private String title;
    private String abstractText;
    private Long linkedPaperId;
    private Integer citationCount;
}
