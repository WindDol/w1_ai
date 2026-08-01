package cn.winddol.ai.test;

import cn.winddol.ai.agent.librarian.api.IAiAdapter;
import cn.winddol.ai.agent.librarian.api.ILibrarianEvidenceProvider;
import cn.winddol.ai.agent.librarian.api.ILibrarianRepository;
import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.agent.librarian.domain.AuditEvidenceBundle;
import cn.winddol.ai.agent.librarian.domain.PaperAuditResult;
import cn.winddol.ai.agent.librarian.domain.RelationAuditRecord;
import cn.winddol.ai.agent.librarian.domain.RelationAuditStatus;
import cn.winddol.ai.agent.librarian.internal.LibrarianAgentImpl;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibrarianRelationAuditUnitTest {

    @Test
    void savesConfirmedRelationWithOnlyRetrievedEvidence() {
        ILibrarianRepository repository = mock(ILibrarianRepository.class);
        ILibrarianEvidenceProvider evidenceProvider = mock(ILibrarianEvidenceProvider.class);
        IAiAdapter aiAdapter = mock(IAiAdapter.class);
        AgentPaperEntity source = paper(7L, "New Möbius reduction", 2025, new float[]{0.1F, 0.2F});
        AgentPaperEntity target = paper(14L, "Foundational Möbius dynamics", 2020, null);
        ToolEvidence sourceEvidence = evidence("SECTION:source-1", 7L, "III. Reduction");
        ToolEvidence targetEvidence = evidence("SECTION:target-1", 14L, "II. Background");
        when(repository.getPaperById(7L)).thenReturn(source);
        when(repository.searchSimilarPapers(any(float[].class), anyInt(), anyLong())).thenReturn(List.of(target));
        when(evidenceProvider.retrieveEvidence(7L, org.mockito.ArgumentMatchers.anyString(), anyInt()))
                .thenReturn(AuditEvidenceBundle.builder().retrievalVersion("hybrid-rrf-v1")
                        .evidence(List.of(sourceEvidence)).build());
        when(evidenceProvider.retrieveEvidence(14L, org.mockito.ArgumentMatchers.anyString(), anyInt()))
                .thenReturn(AuditEvidenceBundle.builder().retrievalVersion("hybrid-rrf-v1")
                        .evidence(List.of(targetEvidence)).build());
        when(aiAdapter.analyzeRelation(any())).thenReturn(PaperAuditResult.builder()
                .type("EXTENDS")
                .reason("The source generalizes the target method.")
                .confidence(0.82)
                .supportingEvidenceKeys(List.of("SECTION:source-1", "SECTION:target-1", "FABRICATED"))
                .build());

        agent(repository, evidenceProvider, aiAdapter).auditAgainstLibrary(7L);

        ArgumentCaptor<RelationAuditRecord> recordCaptor = ArgumentCaptor.forClass(RelationAuditRecord.class);
        verify(repository).replaceRelation(recordCaptor.capture());
        RelationAuditRecord record = recordCaptor.getValue();
        assertEquals("EXTENDS", record.getRelationType());
        assertEquals(RelationAuditStatus.CONFIRMED, record.getAuditStatus());
        assertEquals(2, record.getSupportingEvidence().size());
        assertTrue(record.getSupportingEvidence().stream()
                .noneMatch(item -> "FABRICATED".equals(item.getEvidenceKey())));
        assertEquals("librarian-evidence-v1", record.getAuditVersion());
        assertEquals("source=hybrid-rrf-v1;target=hybrid-rrf-v1", record.getRetrievalVersion());
    }

    @Test
    void marksLowConfidenceOrUntraceableRelationForReview() {
        ILibrarianRepository repository = mock(ILibrarianRepository.class);
        ILibrarianEvidenceProvider evidenceProvider = mock(ILibrarianEvidenceProvider.class);
        IAiAdapter aiAdapter = mock(IAiAdapter.class);
        AgentPaperEntity source = paper(7L, "New paper", 2025, new float[]{0.1F});
        AgentPaperEntity target = paper(14L, "Existing paper", 2020, null);
        when(repository.getPaperById(7L)).thenReturn(source);
        when(repository.searchSimilarPapers(any(float[].class), anyInt(), anyLong())).thenReturn(List.of(target));
        when(evidenceProvider.retrieveEvidence(anyLong(), org.mockito.ArgumentMatchers.anyString(), anyInt()))
                .thenReturn(AuditEvidenceBundle.builder().retrievalVersion("hybrid-rrf-v1").build());
        when(aiAdapter.analyzeRelation(any())).thenReturn(PaperAuditResult.builder()
                .type("SUPPORT")
                .reason("The abstracts seem related.")
                .confidence(0.94)
                .supportingEvidenceKeys(List.of("NOT_IN_RETRIEVAL"))
                .build());

        agent(repository, evidenceProvider, aiAdapter).auditAgainstLibrary(7L);

        ArgumentCaptor<RelationAuditRecord> recordCaptor = ArgumentCaptor.forClass(RelationAuditRecord.class);
        verify(repository).replaceRelation(recordCaptor.capture());
        RelationAuditRecord record = recordCaptor.getValue();
        assertEquals(RelationAuditStatus.PENDING_REVIEW, record.getAuditStatus());
        assertTrue(record.getSupportingEvidence().isEmpty());
    }

    private LibrarianAgentImpl agent(ILibrarianRepository repository,
                                     ILibrarianEvidenceProvider evidenceProvider,
                                     IAiAdapter aiAdapter) {
        return new LibrarianAgentImpl(repository, aiAdapter, evidenceProvider,
                3, 4, 0.65, "librarian-evidence-v1", "librarian-relation-v1", "test-model");
    }

    private AgentPaperEntity paper(Long id, String title, int year, float[] embedding) {
        return AgentPaperEntity.builder()
                .id(id)
                .title(title)
                .abstractText("This paper studies Möbius reductions for coupled oscillators.")
                .years(year)
                .embedding(embedding)
                .build();
    }

    private ToolEvidence evidence(String key, Long paperId, String heading) {
        return ToolEvidence.builder()
                .evidenceKey(key)
                .paperId(paperId)
                .evidenceType("SECTION")
                .headingPath(heading)
                .quote("The result is stated explicitly in this section.")
                .build();
    }
}
