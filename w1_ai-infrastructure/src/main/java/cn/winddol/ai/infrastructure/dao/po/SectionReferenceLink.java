package cn.winddol.ai.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 章节与参考文献引用关联表
 * 记录了：在哪个章节(section_id)引用了哪篇文献(ref_index)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("section_reference_links")
public class SectionReferenceLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属论文ID
     */
    private Long paperId;

    /**
     * 引用发生的章节 UUID
     */
    private String sectionId;

    /**
     * 引用文献的索引标号 (如 "24", "1", "12")
     * 对应 paper_references 表中的 ref_index
     */
    private String refIndex;
}
