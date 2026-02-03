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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("paper_references")
public class Reference{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private String refIndex;
    private String rawText;
    private String title;
    @TableField(typeHandler = PgVectorHandler.class) // 应用刚才写的 Handler
    private float[] embedding;
    @TableField("abstract")
    private String paperAbstract;
    private Long linkedPaperId;
    private String sourceType;

}