package cn.winddol.ai.paper.domain.workspace;

import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.SymbolEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 章节正文及其局部符号、引用和 Outline 定位。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionWorkspaceView {
    private SectionEntity section;
    private String paperTitle;
    private String headingPath;
    @Builder.Default
    private List<SymbolEntity> symbols = List.of();
    @Builder.Default
    private List<ReferenceItem> references = List.of();
}
