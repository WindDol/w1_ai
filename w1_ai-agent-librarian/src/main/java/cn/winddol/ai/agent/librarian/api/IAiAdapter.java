package cn.winddol.ai.agent.librarian.api;

import cn.winddol.ai.agent.librarian.domain.PaperAuditResult;
import cn.winddol.ai.agent.librarian.domain.RelationAuditRequest;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface IAiAdapter {

    String rewriteQueryIfNecessary(String question, List<ChatMessage> history);

    /**
     * 根据候选论文和已检索证据生成关系审计结论；不允许模型编造证据键。
     */
    PaperAuditResult analyzeRelation(RelationAuditRequest request);
}
