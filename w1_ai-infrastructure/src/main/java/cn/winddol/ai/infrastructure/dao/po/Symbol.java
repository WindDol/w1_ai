package cn.winddol.ai.infrastructure.dao.po;

import cn.winddol.ai.infrastructure.dao.handler.PgVectorHandler;
import cn.winddol.ai.infrastructure.dao.handler.StringArrayTypeHandler;
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
@TableName(value = "paper_symbols", autoResultMap = true)
public class Symbol {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private String symbol;
    private String latex;
    private String description;
    private String definitionFormula;
    private Boolean isGlobal;

    // MyBatis-Plus 需要 TypeHandler 处理数组
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] sourceIds;
    @TableField(typeHandler = PgVectorHandler.class) // 应用刚才写的 Handler
    private float[] embedding;

}
