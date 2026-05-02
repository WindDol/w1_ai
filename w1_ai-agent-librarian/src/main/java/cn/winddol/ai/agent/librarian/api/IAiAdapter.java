package cn.winddol.ai.agent.librarian.api;

import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.agent.librarian.domain.PaperAuditResult;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface IAiAdapter {

    String rewriteQueryIfNecessary(String question, List<ChatMessage> history);

    PaperAuditResult analyzeRelation(AgentPaperEntity newPaper, AgentPaperEntity oldPaper, String newAbstract);
}
