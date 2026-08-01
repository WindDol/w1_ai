package cn.winddol.ai.infrastructure.adapter.paper;

import cn.winddol.ai.agent.librarian.api.ILibrarianRepository;
import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.agent.librarian.domain.RelationAuditRecord;
import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.PaperKnowledgeRelationMapper;
import cn.winddol.ai.infrastructure.dao.po.Paper;
import cn.winddol.ai.infrastructure.dao.po.PaperKnowledgeRelation;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import com.alibaba.fastjson2.JSON;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class LibrarianRepositoryImpl implements ILibrarianRepository {

    private final PaperMapper paperMapper;
    private final PaperKnowledgeRelationMapper relationMapper;

    public LibrarianRepositoryImpl(PaperMapper paperMapper, PaperKnowledgeRelationMapper relationMapper) {
        this.paperMapper = paperMapper;
        this.relationMapper = relationMapper;
    }

    @Override
    public AgentPaperEntity getPaperById(Long id) {
        if (id == null) return null;
        Paper paper = paperMapper.selectById(id);
        if (paper == null) return null;
        return AgentPaperEntity.builder()
                .id(id)
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .embedding(paper.getEmbedding())
                .years(paper.getYears())
                .build();
    }

    @Override
    public List<AgentPaperEntity> searchSimilarPapers(float[] vector, int limit, Long excludeId) {
        String vectorStr = Arrays.toString(vector);
        List<Map<String, Object>> results = paperMapper.searchSimilar(vectorStr, limit, excludeId);
        return results.stream().map(map -> AgentPaperEntity.builder()
                .id(numberValue(map.get("id")))
                .title((String) map.get("title"))
                .abstractText((String) map.get("abstracttext"))
                .years(integerValue(map.get("years")))
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void replaceRelation(RelationAuditRecord record) {
        if (record == null || record.getSourcePaperId() == null || record.getTargetPaperId() == null
                || record.getAuditVersion() == null || record.getAuditVersion().isBlank()) {
            throw new IllegalArgumentException("Relation audit record requires source, target and auditVersion");
        }
        relationMapper.deleteByPaperPairAndAuditVersion(
                record.getSourcePaperId(), record.getTargetPaperId(), record.getAuditVersion());
        Date now = new Date();
        PaperKnowledgeRelation po = PaperKnowledgeRelation.builder()
                .sourcePaperId(record.getSourcePaperId())
                .targetPaperId(record.getTargetPaperId())
                .relationType(record.getRelationType())
                .description(record.getDescription())
                .confidence(record.getConfidence())
                .auditStatus(record.getAuditStatus() == null ? null : record.getAuditStatus().name())
                .supportingEvidence(JSON.toJSONString(nullSafeEvidence(record.getSupportingEvidence())))
                .conflictingEvidence(JSON.toJSONString(nullSafeEvidence(record.getConflictingEvidence())))
                .auditVersion(record.getAuditVersion())
                .modelName(record.getModelName())
                .promptVersion(record.getPromptVersion())
                .retrievalVersion(record.getRetrievalVersion())
                .createdAt(now)
                .updatedAt(now)
                .build();
        relationMapper.insert(po);
    }

    private List<ToolEvidence> nullSafeEvidence(List<ToolEvidence> evidence) {
        return evidence == null ? List.of() : evidence;
    }

    private Long numberValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    @Override
    public void updateStatus(Long paperId, String status) {
        Paper po = new Paper();
        po.setId(paperId);
        po.setStatus(status);
        paperMapper.updateById(po);
    }

    @Override
    public void updateStatusWithError(Long paperId, String status, String errorMessage) {
        Paper po = new Paper();
        po.setId(paperId);
        po.setStatus(status);
        po.setStatusMessage(errorMessage);
        paperMapper.updateById(po);
    }
}
