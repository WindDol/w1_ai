package cn.winddol.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
public class EvalRecord {
    private String answer;
    private List<String> contents;
}
