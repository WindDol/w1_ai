package cn.winddol.ai.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("paper_knowledge_relations")
public class PaperKnowledgeRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 新入库的论文ID（审计源）
     */
    private Long sourcePaperId;

    /**
     * 库中已有的论文ID（被对比目标）
     */
    private Long targetPaperId;

    /**
     * 关系类型：SUPPORT (支持), CONTRADICT (冲突), EXTEND (扩展), NEUTRAL (中立)
     */
    private String relationType;

    /**
     * 具体冲突或联系的详细描述
     */
    private String description;

    /**
     * 记录创建时间
     */
    private Date createdAt;

}