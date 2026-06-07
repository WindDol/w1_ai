package cn.winddol.ai.paper.model.aggregate;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDTO {

    /**
     * 命中的正文章节列表
     * Agent 用它来定位“去哪一章读书”
     */
    private List<SectionDTO> sections;

    /**
     * 命中的符号定义列表
     * Agent 用它来快速回答“符号含义”或进行“上下文增强”
     */
    private List<SymbolDTO> symbols;

    private List<ReferenceDTO> references;

    // ==========================================
    // 内部静态类：SectionDTO (对应 SectionMapper 的查询结果)
    // ==========================================
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SectionDTO {
        private String id;          // 章节 UUID (Agent 用它调用 read_section)
        private Long paperId;       // 归属论文 ID
        private String header;      // 章节标题 (如 "III. MÖBIUS GROUP")
        private String content;
        private String parentId;    // 父章节 ID (用于展示层级)
        private Integer idx;        // 章节顺序索引
        private Double score;       // 向量相似度分数 (越接近 1 越相关)
        private String sourcePaperTitle;
        // 注意：这里通常不放 content 全文，只放 preview，防止撑爆 Agent 上下文
    }

    // ==========================================
    // 内部静态类：SymbolDTO (对应 SymbolMapper 的查询结果)
    // ==========================================
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SymbolDTO {
        private Long id;
        private Long paperId;
        private String symbol;            // 符号文本 (如 "\alpha")
        private String description;       // 含义描述
        private String definitionFormula; // 定义公式 (如 "re^{i\psi}")
        private Double score;             // 向量相似度分数
        private String sourcePaperTitle;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ReferenceDTO {
        private Long id;
        private Long globalId;
        private String refId;
        private Long paperId;
        private String sourcePaperTitle;
        private String title;
        private String abstractText;
        private Double score;
        private Long linkedPaperId;
        private Integer citationCount;
    }
}
