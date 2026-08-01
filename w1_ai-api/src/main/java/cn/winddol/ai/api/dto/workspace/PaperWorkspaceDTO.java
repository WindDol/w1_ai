package cn.winddol.ai.api.dto.workspace;

import cn.winddol.ai.api.dto.SymbolDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperWorkspaceDTO {
    private Long id;
    private String title;
    private String abstractText;
    private String status;
    private Integer year;
    private LocalDateTime createdAt;
    private String latestJobId;
    @Builder.Default
    private List<OutlineNodeDTO> outline = List.of();
    @Builder.Default
    private List<SymbolDTO> symbols = List.of();
    @Builder.Default
    private List<ReferenceDTO> references = List.of();
    @Builder.Default
    private List<PaperRelationDTO> relations = List.of();
}
