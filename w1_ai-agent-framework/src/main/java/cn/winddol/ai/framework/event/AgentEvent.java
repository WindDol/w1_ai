package cn.winddol.ai.framework.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentEvent {

    public enum Type { THOUGHT, ACTION, OBSERVATION, ANSWER, EVIDENCE, ERROR }

    private final String sessionId;
    private final Type type;
    private final String content;
    private final String data;
    private final Integer step;
}
