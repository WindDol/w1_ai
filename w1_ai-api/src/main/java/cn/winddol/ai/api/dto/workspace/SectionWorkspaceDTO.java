package cn.winddol.ai.api.dto.workspace;

import cn.winddol.ai.api.dto.SymbolDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionWorkspaceDTO {
    private String id;
    private Long paperId;
    private String paperTitle;
    private String title;
    private String parentId;
    private Integer index;
    private String headingPath;
    private String content;
    @Builder.Default
    private List<SymbolDTO> symbols = List.of();
    @Builder.Default
    private List<ReferenceDTO> references = List.of();
}
