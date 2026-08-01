package cn.winddol.ai.paper.domain.workspace;

import cn.winddol.ai.paper.domain.KnowledgeRelationEntity;
import cn.winddol.ai.paper.domain.PaperEntity;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.domain.SymbolEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 阅读工作台打开一篇论文时所需的聚合只读视图。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperWorkspaceView {
    private PaperEntity paper;
    private String status;
    private String latestJobId;
    @Builder.Default
    private List<SymbolEntity> symbols = List.of();
    @Builder.Default
    private List<ReferenceItem> references = List.of();
    @Builder.Default
    private List<KnowledgeRelationEntity> relations = List.of();
}
