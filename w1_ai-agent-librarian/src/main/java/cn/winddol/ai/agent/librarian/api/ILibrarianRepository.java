package cn.winddol.ai.agent.librarian.api;

import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;

import java.util.List;

public interface ILibrarianRepository {
    AgentPaperEntity getPaperById(Long id);
    List<AgentPaperEntity> searchSimilarPapers(float[] vector, int limit, Long excludeId);
    void saveRelation(Long sourceId, Long targetId, String type, String reason);
    void updateStatus(Long paperId, String status);
    void updateStatusWithError(Long paperId, String status, String errorMessage);
}
