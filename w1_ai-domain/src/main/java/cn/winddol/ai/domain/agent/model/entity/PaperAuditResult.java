package cn.winddol.ai.domain.agent.model.entity;

import lombok.Data;

@Data
public class PaperAuditResult {
    private String type;   // SUPPORT, CONTRADICT, EXTEND
    private String reason; // 具体的理由描述
}