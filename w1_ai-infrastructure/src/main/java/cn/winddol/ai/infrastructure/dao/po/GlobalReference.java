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
@TableName("global_references")
public class GlobalReference {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String s2Id;
    private String fingerprint;
    private String title;
    @TableField("abstract")
    private String abstractText; // 对应数据库 abstract 字段

    @TableField(typeHandler = PgVectorHandler.class)
    private float[] embedding;
    private String sourceType;
    private Integer citationCount;
    private Long linkedPaperId;
}