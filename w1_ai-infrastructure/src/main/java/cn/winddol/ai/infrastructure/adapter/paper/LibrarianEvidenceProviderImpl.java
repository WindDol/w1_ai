package cn.winddol.ai.infrastructure.adapter.paper;

import cn.winddol.ai.agent.librarian.api.ILibrarianEvidenceProvider;
import cn.winddol.ai.agent.librarian.domain.AuditEvidenceBundle;
import cn.winddol.ai.paper.api.IPaperRetrievalService;
import cn.winddol.ai.paper.domain.retrieval.PaperEvidence;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalQuery;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalResult;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 将论文模块的混合检索结果适配为 librarian 关系审计所需的可追溯证据。
 */
@Repository
public class LibrarianEvidenceProviderImpl implements ILibrarianEvidenceProvider {

    private static final double AUDIT_MIN_SEMANTIC_SCORE = 0.20;

    private final IPaperRetrievalService retrievalService;

    public LibrarianEvidenceProviderImpl(IPaperRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    /**
     * 在指定论文内部检索候选关系的相关片段，保留 retrievalVersion 供后续复现。
     */
    @Override
    public AuditEvidenceBundle retrieveEvidence(Long paperId, String query, int limit) {
        if (paperId == null || query == null || query.isBlank()) {
            return AuditEvidenceBundle.builder().retrievalVersion("unavailable").build();
        }
        PaperRetrievalResult result = retrievalService.retrieve(
                new PaperRetrievalQuery(query, paperId, AUDIT_MIN_SEMANTIC_SCORE, Math.max(1, limit)));
        return AuditEvidenceBundle.builder()
                .retrievalVersion(result.retrievalVersion())
                .evidence(result.evidence().stream().map(this::toToolEvidence).toList())
                .build();
    }

    /**
     * 复用统一 ToolEvidence 模型，保证关系审计与 ResearchAgent 使用同一种证据定位格式。
     */
    private ToolEvidence toToolEvidence(PaperEvidence evidence) {
        return ToolEvidence.builder()
                .evidenceKey(evidence.getEvidenceKey())
                .evidenceType(evidence.getEvidenceType() == null ? null : evidence.getEvidenceType().name())
                .paperId(evidence.getPaperId())
                .paperTitle(evidence.getPaperTitle())
                .sectionId(evidence.getSectionId())
                .chunkId(evidence.getChunkId())
                .heading(evidence.getHeading())
                .headingPath(evidence.getHeadingPath())
                .pageStart(evidence.getPageStart())
                .pageEnd(evidence.getPageEnd())
                .referenceIndex(evidence.getReferenceIndex())
                .quote(evidence.getQuote())
                .accessedVia("librarianAudit")
                .build();
    }
}
