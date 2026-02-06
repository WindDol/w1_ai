package cn.winddol.ai.infrastructure.dao.po;

import cn.winddol.ai.domain.paperTools.model.entity.OutlineNode;
import cn.winddol.ai.infrastructure.dao.handler.OutlineNodeTypeHandler;
import cn.winddol.ai.infrastructure.dao.handler.PgVectorHandler;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "papers", autoResultMap = true) // 开启自动 ResultMap 以支持 JSON 转换
public class Paper {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    // 对应数据库的 jsonb 字段
    @TableField(typeHandler = OutlineNodeTypeHandler.class)
    private List<OutlineNode> outline;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    private String filePath;

    private LocalDateTime createdAt;
    private String fingerprint;
    private String status;
    private String statusMessage;
    @TableField(typeHandler = PgVectorHandler.class)
    private float[] embedding;

    @TableField("abstract")
    private String abstractText;
}