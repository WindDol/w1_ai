package cn.winddol.ai.agent.librarian.api;

import cn.winddol.ai.agent.librarian.domain.AuditEvidenceBundle;

/**
 * 关系审计获取论文正文、符号和引用证据的端口。
 */
public interface ILibrarianEvidenceProvider {

    AuditEvidenceBundle retrieveEvidence(Long paperId, String query, int limit);
}
