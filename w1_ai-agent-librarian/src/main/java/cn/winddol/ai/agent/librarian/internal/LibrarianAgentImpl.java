package cn.winddol.ai.agent.librarian.internal;

import cn.winddol.ai.agent.librarian.api.IAiAdapter;
import cn.winddol.ai.agent.librarian.api.ILibrarianAgent;
import cn.winddol.ai.agent.librarian.api.ILibrarianRepository;
import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.agent.librarian.domain.PaperAuditResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LibrarianAgentImpl implements ILibrarianAgent {

    private final ILibrarianRepository repository;
    private final IAiAdapter aiAdapter;

    public LibrarianAgentImpl(ILibrarianRepository repository, IAiAdapter aiAdapter) {
        this.repository = repository;
        this.aiAdapter = aiAdapter;
    }

    @Override
    public void initiateAudit(Long paperId, String title) {
        log.info("Librarian audit initiated for Paper [{}] - \"{}\"", paperId, title);
    }

    @Override
    public void auditAgainstLibrary(Long paperId) {
        log.info("Starting library audit for Paper [{}]", paperId);

        AgentPaperEntity newPaper = repository.getPaperById(paperId);
        if (newPaper == null) {
            log.warn("Paper [{}] not found, skipping audit", paperId);
            return;
        }
        String newAbstract = newPaper.getAbstractText();
        float[] vector = newPaper.getEmbedding();
        List<AgentPaperEntity> neighbors = repository.searchSimilarPapers(vector, 3, paperId);

        if (neighbors.isEmpty()) {
            log.info("No similar papers found for Paper [{}]. Likely a new sub-field.", paperId);
            return;
        }

        for (AgentPaperEntity oldPaper : neighbors) {
            PaperAuditResult auditResult = aiAdapter.analyzeRelation(newPaper, oldPaper, newAbstract);
            if (auditResult != null && auditResult.getType() != null) {
                repository.saveRelation(newPaper.getId(), oldPaper.getId(),
                        auditResult.getType(), auditResult.getReason());
                log.info("Relation saved: Paper [{}] {} -> Paper [{}]",
                        newPaper.getId(), auditResult.getType(), oldPaper.getId());
            }
        }
    }
}
