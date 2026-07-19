package cn.winddol.ai.test;

import cn.winddol.ai.paper.api.IEmbeddingService;
import cn.winddol.ai.paper.api.IRetrievalRepository;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.retrieval.EvidenceType;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalQuery;
import cn.winddol.ai.paper.domain.retrieval.RetrievalCandidate;
import cn.winddol.ai.paper.domain.retrieval.RetrievalChannel;
import cn.winddol.ai.paper.domain.retrieval.SectionChunk;
import cn.winddol.ai.paper.internal.retrieval.EvidenceQuoteExtractor;
import cn.winddol.ai.paper.internal.retrieval.HeadingPathResolver;
import cn.winddol.ai.paper.internal.retrieval.HybridRetrieverServiceImpl;
import cn.winddol.ai.paper.internal.retrieval.RankedCandidates;
import cn.winddol.ai.paper.internal.retrieval.ReciprocalRankFusion;
import cn.winddol.ai.paper.internal.retrieval.StructuredSectionChunker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StructuredRetrievalUnitTest {

    @Test
    void splitsASectionIntoDeterministicBoundedChunks() {
        StructuredSectionChunker chunker = new StructuredSectionChunker(800, 100);
        SectionEntity section = SectionEntity.builder()
                .id("section-1")
                .paperId(7L)
                .header("3.2 Stability")
                .content(paragraph("first", 80) + "\n\n"
                        + paragraph("second", 80) + "\n\n"
                        + paragraph("third", 80))
                .build();

        List<SectionChunk> first = chunker.chunk(section, "3 Results > 3.2 Stability");
        List<SectionChunk> second = chunker.chunk(section, "3 Results > 3.2 Stability");

        assertTrue(first.size() > 1);
        assertTrue(first.stream().allMatch(chunk -> chunk.getContent().length() <= 800));
        assertEquals(first.stream().map(SectionChunk::getId).toList(),
                second.stream().map(SectionChunk::getId).toList());
        assertTrue(first.stream().allMatch(chunk -> chunk.getSectionId().equals("section-1")));
    }

    @Test
    void resolvesHeadingPathFromParentLinks() {
        HeadingPathResolver resolver = new HeadingPathResolver();
        SectionEntity parent = SectionEntity.builder().id("p").header("III. RESULTS").build();
        SectionEntity child = SectionEntity.builder()
                .id("c").parentId("p").header("A. Stability").build();

        Map<String, String> paths = resolver.resolve(List.of(parent, child));

        assertEquals("III. RESULTS > A. Stability", paths.get("c"));
    }

    @Test
    void rrfRewardsEvidenceFoundByMultipleChannels() {
        ReciprocalRankFusion fusion = new ReciprocalRankFusion();
        RetrievalCandidate sharedVector = candidate("SECTION:shared", 0.81, null);
        RetrievalCandidate vectorOnly = candidate("SECTION:vector", 0.80, null);
        RetrievalCandidate sharedText = candidate("SECTION:shared", null, 0.50);
        RetrievalCandidate textOnly = candidate("SECTION:text", null, 0.60);

        List<RetrievalCandidate> result = fusion.fuse(List.of(
                new RankedCandidates(RetrievalChannel.VECTOR, 1.0,
                        List.of(vectorOnly, sharedVector)),
                new RankedCandidates(RetrievalChannel.FULL_TEXT, 1.0,
                        List.of(textOnly, sharedText))
        ), 3);

        assertEquals("SECTION:shared", result.get(0).getEvidenceKey());
        assertEquals(2, result.get(0).getMatchedChannels().size());
    }

    @Test
    void appliesThresholdToVectorQueriesAndKeepsLexicalFallback() {
        IRetrievalRepository repository = mock(IRetrievalRepository.class);
        IEmbeddingService embeddingService = mock(IEmbeddingService.class);
        RetrievalCandidate lexical = RetrievalCandidate.builder()
                .evidenceKey("SECTION:lexical")
                .evidenceType(EvidenceType.SECTION)
                .paperId(9L)
                .heading("4 Stability")
                .content("The stability criterion follows from the linearized operator.")
                .lexicalScore(0.7)
                .build();
        when(repository.searchSectionChunksByFullText(9L, "stability criterion", 30))
                .thenReturn(List.of(lexical));
        when(repository.searchSymbolsByFullText(9L, "stability criterion", 30)).thenReturn(List.of());
        when(repository.searchReferencesByFullText(9L, "stability criterion", 30)).thenReturn(List.of());
        when(repository.searchSectionChunksByVector(eq(9L), anyString(), eq(0.7), eq(30)))
                .thenReturn(List.of());
        when(repository.searchSymbolsByVector(eq(9L), anyString(), eq(0.7), eq(30)))
                .thenReturn(List.of());
        when(repository.searchReferencesByVector(eq(9L), anyString(), eq(0.7), eq(30)))
                .thenReturn(List.of());
        when(embeddingService.embed("stability criterion")).thenReturn(new float[]{0.1f, 0.2f});
        HybridRetrieverServiceImpl service = new HybridRetrieverServiceImpl(
                repository, embeddingService, new ReciprocalRankFusion(),
                new EvidenceQuoteExtractor(), 30, 8, "hybrid");

        var result = service.retrieve(new PaperRetrievalQuery("stability criterion", 9L, 0.7, 5));

        verify(repository).searchSectionChunksByVector(eq(9L), anyString(), eq(0.7), eq(30));
        assertFalse(result.evidence().isEmpty());
        assertTrue(result.evidence().get(0).getMatchedChannels().contains(RetrievalChannel.FULL_TEXT));
    }

    private RetrievalCandidate candidate(String key, Double semantic, Double lexical) {
        return RetrievalCandidate.builder()
                .evidenceKey(key)
                .evidenceType(EvidenceType.SECTION)
                .semanticScore(semantic)
                .lexicalScore(lexical)
                .build();
    }

    private String paragraph(String word, int count) {
        return (word + " ").repeat(count).trim() + ".";
    }
}
