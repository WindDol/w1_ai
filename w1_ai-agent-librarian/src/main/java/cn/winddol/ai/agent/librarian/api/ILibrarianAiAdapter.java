package cn.winddol.ai.agent.librarian.api;

import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.agent.librarian.domain.PaperAuditResult;

public interface ILibrarianAiAdapter {
    PaperAuditResult analyzeRelation(AgentPaperEntity newPaper, AgentPaperEntity oldPaper, String newAbstract);
}
