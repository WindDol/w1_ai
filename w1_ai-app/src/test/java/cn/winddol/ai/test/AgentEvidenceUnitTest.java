package cn.winddol.ai.test;

import cn.winddol.ai.agent.research.domain.ResearchContext;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.PaperEntity;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.SymbolEntity;
import cn.winddol.ai.paper.internal.retrieval.AgentReaderServiceImpl;
import cn.winddol.ai.paper.domain.retrieval.EvidenceType;
import cn.winddol.ai.paper.domain.retrieval.PaperEvidence;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalResult;
import cn.winddol.ai.paper.internal.ScientificResearchToolsImpl;
import cn.winddol.ai.paper.internal.retrieval.AgentCommonToolsImpl;
import cn.winddol.ai.paper.internal.retrieval.HybridRetrieverServiceImpl;
import cn.winddol.ai.shared.model.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentEvidenceUnitTest {

    @Test
    void researchContextKeepsQuestionSeparateFromReadingScope() {
        ResearchContext context = new ResearchContext(
                7L, "section-1", "II. BACKGROUND > C. Mobius group",
                "M(z) maps the unit disk onto itself.");

        String mission = context.scopeTask("Explain the group action");

        assertTrue(mission.startsWith("Explain the group action"));
        assertTrue(mission.contains("Selected paper IDs: [7]"));
        assertTrue(mission.contains("Current section ID: section-1"));
        assertTrue(mission.contains("<BEGIN_SELECTED_SOURCE>"));
    }

    @Test
    void researchContextSupportsMultiplePaperScope() {
        ResearchContext context = new ResearchContext(List.of(7L, 14L, 7L), null, null, null);

        String mission = context.scopeTask("Compare the methods");

        assertTrue(mission.contains("Selected paper IDs: [7, 14]"));
        assertTrue(mission.contains("searchLibrary separately"));
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
                .idx(3)
                .build();
        SectionEntity previous = SectionEntity.builder().id("previous").header("II. MODEL").build();
        SectionEntity next = SectionEntity.builder().id("next").header("B. Bifurcation").build();
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
        when(repository.getSectionSibling(7L, 3, -1)).thenReturn(previous);
        when(repository.getSectionSibling(7L, 3, 1)).thenReturn(next);

        var result = new AgentReaderServiceImpl(repository).readSectionWithEvidence("child");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getEvidence().size());
        assertEquals("SECTION:child", result.getEvidence().get(0).getEvidenceKey());
        assertEquals("III. RESULTS > A. Stability", result.getEvidence().get(0).getHeadingPath());
        assertEquals("SYMBOL:11", result.getEvidence().get(1).getEvidenceKey());
        assertEquals("readSection", result.getEvidence().get(1).getAccessedVia());
        assertTrue(result.getContent().contains("### Parent Section Background"));
        assertTrue(result.getContent().contains("### Current Section Content"));
        assertTrue(result.getContent().contains("[sectionId=previous] II. MODEL"));
        assertTrue(result.getContent().contains("[sectionId=next] B. Bifurcation"));
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

    @Test
    void searchExpandsEachMatchedSectionOnlyOnce() {
        HybridRetrieverServiceImpl retriever = mock(HybridRetrieverServiceImpl.class);
        AgentReaderServiceImpl reader = mock(AgentReaderServiceImpl.class);
        AgentCommonToolsImpl commonTools = mock(AgentCommonToolsImpl.class);
        PaperEvidence firstChunk = PaperEvidence.builder()
                .evidenceKey("SECTION_CHUNK:first")
                .evidenceType(EvidenceType.SECTION)
                .paperId(7L)
                .paperTitle("Test Paper")
                .sectionId("section-1")
                .chunkId("first")
                .heading("A. Stability")
                .headingPath("III. RESULTS > A. Stability")
                .quote("first matching chunk")
                .fusionScore(0.8)
                .build();
        PaperEvidence secondChunk = PaperEvidence.builder()
                .evidenceKey("SECTION_CHUNK:second")
                .evidenceType(EvidenceType.SECTION)
                .paperId(7L)
                .paperTitle("Test Paper")
                .sectionId("section-1")
                .chunkId("second")
                .heading("A. Stability")
                .headingPath("III. RESULTS > A. Stability")
                .quote("second matching chunk")
                .fusionScore(0.7)
                .build();
        when(retriever.retrieve(any())).thenReturn(new PaperRetrievalResult(
                "stability", 7L, "test", 1L, List.of(firstChunk, secondChunk)));
        when(reader.readSectionWithEvidence("section-1"))
                .thenReturn(ToolResult.ok("COMPLETE SECTION CONTEXT"));

        var tools = new ScientificResearchToolsImpl(retriever, reader, commonTools, 2, 1.35);
        var result = tools.searchLibraryWithEvidence("stability", 7L, 0.4);

        assertTrue(result.getContent().contains("COMPLETE SECTION CONTEXT"));
        assertEquals(1, countOccurrences(result.getContent(), "#### Expanded sectionId=section-1"));
        verify(reader, times(1)).readSectionWithEvidence("section-1");
    }

    private int countOccurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }
}
