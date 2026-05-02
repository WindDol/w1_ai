package cn.winddol.ai.agent.librarian.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentPaperEntity {
    private Long id;
    private String title;
    private String abstractText;
    private float[] embedding;
    private Object outline;
    private Integer years;
}
