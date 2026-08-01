package cn.winddol.ai.agent.librarian.internal;

import cn.winddol.ai.agent.librarian.api.IAiAdapter;
import cn.winddol.ai.agent.librarian.api.ILibrarianAgent;
import cn.winddol.ai.agent.librarian.api.ILibrarianEvidenceProvider;
import cn.winddol.ai.agent.librarian.api.ILibrarianRepository;
import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.agent.librarian.domain.AuditEvidenceBundle;
import cn.winddol.ai.agent.librarian.domain.PaperAuditResult;
import cn.winddol.ai.agent.librarian.domain.RelationAuditRecord;
import cn.winddol.ai.agent.librarian.domain.RelationAuditRequest;
import cn.winddol.ai.agent.librarian.domain.RelationAuditStatus;
import cn.winddol.ai.agent.librarian.domain.RelationType;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class LibrarianAgentImpl implements ILibrarianAgent {

    private final ILibrarianRepository repository;
    private final IAiAdapter aiAdapter;
    private final ILibrarianEvidenceProvider evidenceProvider;
    private final int neighborLimit;
    private final int evidenceLimit;
    private final double reviewThreshold;
    private final String auditVersion;
    private final String promptVersion;
    private final String modelName;

    public LibrarianAgentImpl(ILibrarianRepository repository,
                              IAiAdapter aiAdapter,
                              ILibrarianEvidenceProvider evidenceProvider,
                              @Value("${paper.librarian.audit.neighbor-limit:3}") int neighborLimit,
                              @Value("${paper.librarian.audit.evidence-limit:4}") int evidenceLimit,
                              @Value("${paper.librarian.audit.review-threshold:0.5}") double reviewThreshold,
                              @Value("${paper.librarian.audit.version:librarian-evidence-v1}") String auditVersion,
                              @Value("${paper.librarian.audit.prompt-version:librarian-relation-v1}") String promptVersion,
                              @Value("${ai.llm.model:configured-chat-model}") String modelName) {
        this.repository = repository;
        this.aiAdapter = aiAdapter;
        this.evidenceProvider = evidenceProvider;
        this.neighborLimit = Math.max(1, neighborLimit);
        this.evidenceLimit = Math.max(1, evidenceLimit);
        this.reviewThreshold = Math.max(0.0, Math.min(1.0, reviewThreshold));
        this.auditVersion = auditVersion;
        this.promptVersion = promptVersion;
        this.modelName = modelName;
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
        float[] vector = newPaper.getEmbedding();
        if (vector == null || vector.length == 0) {
            log.warn("Paper [{}] has no embedding, skipping relation audit", paperId);
            return;
        }
        List<AgentPaperEntity> neighbors = repository.searchSimilarPapers(vector, neighborLimit, paperId);

        if (neighbors == null || neighbors.isEmpty()) {
            log.info("No similar papers found for Paper [{}]. Likely a new sub-field.", paperId);
            return;
        }

        for (AgentPaperEntity oldPaper : neighbors) {
            auditCandidate(newPaper, oldPaper);
        }
    }

    /**
     * 对一对候选论文检索双方证据、请求模型判定，并保存可回溯的审计记录。
     */
    private void auditCandidate(AgentPaperEntity sourcePaper, AgentPaperEntity targetPaper) {
        if (targetPaper == null || targetPaper.getId() == null) {
            return;
        }
        String query = buildEvidenceQuery(sourcePaper, targetPaper);
        AuditEvidenceBundle sourceBundle = evidenceProvider.retrieveEvidence(sourcePaper.getId(), query, evidenceLimit);
        AuditEvidenceBundle targetBundle = evidenceProvider.retrieveEvidence(targetPaper.getId(), query, evidenceLimit);
        RelationAuditRequest request = RelationAuditRequest.builder()
                .sourcePaper(sourcePaper)
                .targetPaper(targetPaper)
                .auditVersion(auditVersion)
                .promptVersion(promptVersion)
                .sourceRetrievalVersion(sourceBundle.getRetrievalVersion())
                .targetRetrievalVersion(targetBundle.getRetrievalVersion())
                .sourceEvidence(sourceBundle.getEvidence())
                .targetEvidence(targetBundle.getEvidence())
                .build();
        PaperAuditResult result = aiAdapter.analyzeRelation(request);
        if (result == null) {
            log.warn("Relation audit returned no result for Paper [{}] -> [{}]", sourcePaper.getId(), targetPaper.getId());
            return;
        }

        String relationType = normalizeRelationType(result.getType());
        List<ToolEvidence> supporting = resolveEvidence(result.getSupportingEvidenceKeys(), sourceBundle, targetBundle);
        List<ToolEvidence> conflicting = resolveEvidence(result.getConflictingEvidenceKeys(), sourceBundle, targetBundle);
        double confidence = normalizeConfidence(result.getConfidence());
        RelationAuditStatus status = confidence >= reviewThreshold
                && (!supporting.isEmpty() || !conflicting.isEmpty())
                ? RelationAuditStatus.CONFIRMED
                : RelationAuditStatus.PENDING_REVIEW;
        RelationAuditRecord record = RelationAuditRecord.builder()
                .sourcePaperId(sourcePaper.getId())
                .targetPaperId(targetPaper.getId())
                .relationType(relationType)
                .description(result.getReason())
                .confidence(confidence)
                .auditStatus(status)
                .auditVersion(auditVersion)
                .modelName(modelName)
                .promptVersion(promptVersion)
                .retrievalVersion(joinVersions(sourceBundle.getRetrievalVersion(), targetBundle.getRetrievalVersion()))
                .supportingEvidence(supporting)
                .conflictingEvidence(conflicting)
                .build();
        repository.replaceRelation(record);
        log.info("Relation audit saved: Paper [{}] {} -> Paper [{}], confidence={}, status={}",
                sourcePaper.getId(), relationType, targetPaper.getId(), confidence, status);
    }

    /**
     * 关系模型只能返回当前检索结果的 evidenceKey，过滤未知键以避免伪造来源。
     */
    private List<ToolEvidence> resolveEvidence(List<String> evidenceKeys,
                                               AuditEvidenceBundle sourceBundle,
                                               AuditEvidenceBundle targetBundle) {
        Map<String, ToolEvidence> available = new LinkedHashMap<>();
        sourceBundle.getEvidence().forEach(item -> available.put(item.getEvidenceKey(), item));
        targetBundle.getEvidence().forEach(item -> available.put(item.getEvidenceKey(), item));
        if (evidenceKeys == null || evidenceKeys.isEmpty()) {
            return List.of();
        }
        List<ToolEvidence> resolved = new ArrayList<>();
        for (String key : evidenceKeys) {
            ToolEvidence evidence = available.get(key);
            if (evidence != null && !resolved.contains(evidence)) {
                resolved.add(evidence);
            }
        }
        return resolved;
    }

    /**
     * 构造用于双方正文检索的查询，控制长度避免把整篇摘要直接送入 embedding 服务。
     */
    private String buildEvidenceQuery(AgentPaperEntity sourcePaper, AgentPaperEntity targetPaper) {
        String sourceAbstract = sourcePaper.getAbstractText() == null ? "" : sourcePaper.getAbstractText();
        String query = String.join(" ", List.of(
                safeText(sourcePaper.getTitle()),
                safeText(targetPaper.getTitle()),
                sourceAbstract.length() > 800 ? sourceAbstract.substring(0, 800) : sourceAbstract));
        return query.trim();
    }

    private String normalizeRelationType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return RelationType.UNRELATED.name();
        }
        try {
            return RelationType.valueOf(rawType.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return RelationType.UNRELATED.name();
        }
    }

    private double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private String joinVersions(String sourceVersion, String targetVersion) {
        return "source=" + safeText(sourceVersion) + ";target=" + safeText(targetVersion);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
