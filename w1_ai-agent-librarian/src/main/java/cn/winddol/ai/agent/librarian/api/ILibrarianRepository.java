package cn.winddol.ai.agent.librarian.api;

import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.agent.librarian.domain.RelationAuditRecord;

import java.util.List;

public interface ILibrarianRepository {
    AgentPaperEntity getPaperById(Long id);
    List<AgentPaperEntity> searchSimilarPapers(float[] vector, int limit, Long excludeId);
    /**
     * 按论文对和审计版本替换旧结果，使重跑不会累计重复关系。
     */
    void replaceRelation(RelationAuditRecord record);
    void updateStatus(Long paperId, String status);
    void updateStatusWithError(Long paperId, String status, String errorMessage);
}
