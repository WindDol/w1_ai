package cn.winddol.ai.test;

import cn.winddol.ai.agent.research.domain.ResearchContext;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.PaperEntity;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.SymbolEntity;
import cn.winddol.ai.paper.internal.retrieval.AgentReaderServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentEvidenceUnitTest {

    @Test
    void researchContextKeepsQuestionSeparateFromReadingScope() {
        ResearchContext context = new ResearchContext(
                7L, "section-1", "II. BACKGROUND > C. Mobius group",
                "M(z) maps the unit disk onto itself.");

        String mission = context.scopeTask("Explain the group action");

        assertTrue(mission.startsWith("Explain the group action"));
        assertTrue(mission.contains("Current paper ID: 7"));
        assertTrue(mission.contains("Current section ID: section-1"));
        assertTrue(mission.contains("<BEGIN_SELECTED_SOURCE>"));
    }

    @Test
    void readingSectionReturnsTraceableSectionAndSymbolEvidence() {
        IPaperRepository repository = mock(IPaperRepository.class);
        SectionEntity parent = SectionEntity.builder()
                .id("parent")
                .paperId(7L)
                .header("III. RESULTS")
                .build();
        SectionEntity child = SectionEntity.builder()
                .id("child")
                .paperId(7L)
                .parentId("parent")
                .header("A. Stability")
                .content("The stability criterion follows from the linearized operator.")
                .build();
        SymbolEntity symbol = SymbolEntity.builder()
                .id(11L)
                .symbol("K")
                .description("Coupling strength")
                .definitionFormula("K > K_c")
                .build();
        when(repository.selectSectionById("child")).thenReturn(child);
        when(repository.selectSectionById("parent")).thenReturn(parent);
        when(repository.selectPaperById(7L)).thenReturn(PaperEntity.builder()
                .id(7L)
                .title("Test Paper")
                .build());
        when(repository.selectSymbolsByUuids("child")).thenReturn(List.of(symbol));

        var result = new AgentReaderServiceImpl(repository).readSectionWithEvidence("child");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getEvidence().size());
        assertEquals("SECTION:child", result.getEvidence().get(0).getEvidenceKey());
        assertEquals("III. RESULTS > A. Stability", result.getEvidence().get(0).getHeadingPath());
        assertEquals("SYMBOL:11", result.getEvidence().get(1).getEvidenceKey());
        assertEquals("readSection", result.getEvidence().get(1).getAccessedVia());
    }

    @Test
    void referenceLookupReturnsTraceableReferenceEvidence() {
        IPaperRepository repository = mock(IPaperRepository.class);
        ReferenceItem reference = ReferenceItem.builder()
                .paperId(7L)
                .refId("12")
                .title("Foundational stability paper")
                .paperAbstract("Studies the stability criterion for coupled oscillators.")
                .build();
        when(repository.lookupReference(7L, "12")).thenReturn(reference);
        when(repository.selectPaperById(7L)).thenReturn(PaperEntity.builder()
                .id(7L)
                .title("Test Paper")
                .build());

        var result = new AgentReaderServiceImpl(repository).lookupReferenceWithEvidence(7L, "12");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getEvidence().size());
        assertEquals("REFERENCE:7:12", result.getEvidence().get(0).getEvidenceKey());
        assertEquals("lookupReference", result.getEvidence().get(0).getAccessedVia());
    }
}
