package cn.winddol.ai.agent.research.domain;

import lombok.Data;

@Data
public class ResearchAgentStep {
    private String thought;
    private String action;
    private String actionInput;
    private String finalAnswer;
}
