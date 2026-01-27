package cn.winddol.ai.infrastructure.dao.po;

import cn.winddol.ai.domain.paper.model.entity.OutlineNode;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "papers", autoResultMap = true) // 开启自动 ResultMap 以支持 JSON 转换
public class Paper {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    // 对应数据库的 jsonb 字段
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<OutlineNode> outline;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    private String filePath;

    private LocalDateTime createdAt;
}