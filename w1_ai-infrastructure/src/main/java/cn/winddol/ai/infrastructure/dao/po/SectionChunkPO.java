package cn.winddol.ai.infrastructure.dao.po;

import cn.winddol.ai.infrastructure.dao.handler.PgVectorHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "section_chunks", autoResultMap = true)
public class SectionChunkPO {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long paperId;
    private String sectionId;
    private Integer chunkIndex;
    private String heading;
    private String headingPath;
    private String content;
    private Integer pageStart;
    private Integer pageEnd;
    private Integer tokenCount;
    private String contentHash;
    private String chunkerVersion;
    private String embeddingModel;
    @TableField(typeHandler = PgVectorHandler.class)
    private float[] embedding;
}
