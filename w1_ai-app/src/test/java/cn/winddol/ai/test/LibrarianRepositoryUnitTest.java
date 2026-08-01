package cn.winddol.ai.test;

import cn.winddol.ai.agent.librarian.domain.RelationAuditRecord;
import cn.winddol.ai.agent.librarian.domain.RelationAuditStatus;
import cn.winddol.ai.infrastructure.adapter.paper.LibrarianRepositoryImpl;
import cn.winddol.ai.infrastructure.dao.PaperKnowledgeRelationMapper;
import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.po.PaperKnowledgeRelation;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class LibrarianRepositoryUnitTest {

    @Test
    void replacesOnlyTheSamePaperPairAndAuditVersion() {
        PaperMapper paperMapper = mock(PaperMapper.class);
        PaperKnowledgeRelationMapper relationMapper = mock(PaperKnowledgeRelationMapper.class);
        LibrarianRepositoryImpl repository = new LibrarianRepositoryImpl(paperMapper, relationMapper);
        RelationAuditRecord record = RelationAuditRecord.builder()
                .sourcePaperId(7L)
                .targetPaperId(14L)
                .relationType("EXTENDS")
                .description("The source generalizes the target method.")
                .confidence(0.82)
                .auditStatus(RelationAuditStatus.CONFIRMED)
                .auditVersion("librarian-evidence-v1")
                .modelName("test-model")
                .promptVersion("librarian-relation-v1")
                .retrievalVersion("source=hybrid-rrf-v1;target=hybrid-rrf-v1")
                .supportingEvidence(List.of(ToolEvidence.builder()
                        .evidenceKey("SECTION:source-1")
                        .quote("A source excerpt.")
                        .build()))
                .build();

        repository.replaceRelation(record);

        InOrder order = inOrder(relationMapper);
        order.verify(relationMapper).deleteByPaperPairAndAuditVersion(7L, 14L, "librarian-evidence-v1");
        ArgumentCaptor<PaperKnowledgeRelation> relationCaptor = ArgumentCaptor.forClass(PaperKnowledgeRelation.class);
        order.verify(relationMapper).insert(relationCaptor.capture());
        PaperKnowledgeRelation saved = relationCaptor.getValue();
        assertEquals("CONFIRMED", saved.getAuditStatus());
        assertEquals("librarian-evidence-v1", saved.getAuditVersion());
        assertEquals(0.82, saved.getConfidence());
        assertTrueJsonArray(saved.getSupportingEvidence());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    private void assertTrueJsonArray(String value) {
        org.junit.jupiter.api.Assertions.assertTrue(value.startsWith("["));
        org.junit.jupiter.api.Assertions.assertTrue(value.endsWith("]"));
    }
}
