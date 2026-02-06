package cn.winddol.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelationEntity {

    /**
     * 被关联论文的 ID
     */
    private Long targetId;

    /**
     * 被关联论文的标题
     */
    private String targetTitle;

    /**
     * 关系类型：SUPPORT (支持), CONTRADICT (冲突), EXTEND (扩展)
     */
    private String type;

    /**
     * Librarian 给出的详细审计描述（例如：为什么是扩展关系，具体公式的变化）
     */
    private String description;

    /**
     * 辅助方法：将其格式化为 Agent 易读的字符串
     */
    @Override
    public String toString() {
        return String.format(
                "Relation: [%s]\nTarget Paper: (ID: %d) \"%s\"\nLibrarian Insight: %s",
                type, targetId, targetTitle, description
        );
    }
}
