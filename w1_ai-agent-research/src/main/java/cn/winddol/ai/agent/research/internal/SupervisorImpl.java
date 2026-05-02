package cn.winddol.ai.agent.research.internal;

import cn.winddol.ai.agent.research.api.IAiAdapter;
import cn.winddol.ai.agent.research.api.ISupervisor;
import dev.langchain4j.data.message.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupervisorImpl implements ISupervisor {

    private final IAiAdapter aiAdapter;

    public SupervisorImpl(IAiAdapter aiAdapter) {
        this.aiAdapter = aiAdapter;
    }

    @Override
    public String rewriteAndTranslate(String question, List<ChatMessage> history) {
        return aiAdapter.rewriteQueryIfNecessary(question, history);
    }
}
