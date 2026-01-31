package cn.winddol.ai.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sections")
public class Section {
    @TableId(type = IdType.INPUT) // 手动输入 UUID
    private String id;

    private Long paperId;

    private String header;

    private String content;

    private Integer tokenCount;

    private Object embedding;

    private Integer idx;

    private String parentId;

    @TableField(exist = false)
    private Double score;
}