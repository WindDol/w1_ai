package cn.winddol.ai.domain.agent.service.bussiness;

import cn.winddol.ai.domain.agent.adapter.ai.IAiAdapter;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentSupervisor {
    @Resource
    private IAiAdapter aiAdapter;

    public String rewriteAndTranslate(String question, List<ChatMessage> history) {
        return aiAdapter.rewriteQueryIfNecessary(question, history);
    }
}
