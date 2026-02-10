package cn.winddol.ai.domain.agent.adapter.ai;

import cn.winddol.ai.domain.agent.model.entity.PaperAuditResult;
import cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface IAiAdapter {
    String rewriteQueryIfNecessary(String userQuestion, List<ChatMessage> messages);

    PaperAuditResult analyzeRelation(AgentPaperEntity newPaper, AgentPaperEntity oldPaper, String newAbstract);
}
