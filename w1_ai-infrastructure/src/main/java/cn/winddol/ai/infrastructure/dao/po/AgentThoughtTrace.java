package cn.winddol.ai.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("agent_thought_trace")
public class AgentThoughtTrace {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer stepNumber;
    private String thought;
    private String action;
    private String actionInput;
    private String observation;
    private Integer citationCount;
}
