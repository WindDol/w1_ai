package cn.winddol.ai.api.dto.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutlineNodeDTO {
    private String id;
    private String title;
    private Integer level;
    @Builder.Default
    private List<OutlineNodeDTO> children = List.of();
}
