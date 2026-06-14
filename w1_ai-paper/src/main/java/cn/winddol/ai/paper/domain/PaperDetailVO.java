package cn.winddol.ai.paper.domain;

import cn.winddol.ai.paper.domain.SymbolEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperDetailVO {
    private Long id;
    private String title;
    private String abstractText;

    // Librarian 的审计报告
    private String noveltyAssessment; // 创新点

    // 统计数据
    private Integer symbolCount;
    private Integer referenceCount;

    // 详情列表 (只取前 10 个展示，避免太长)
    private List<SymbolEntity> keySymbols;
    private List<String> relatedPaperTitles; // 关联/冲突的论文标题
}
