package cn.winddol.ai.test;

import cn.winddol.ai.paper.api.IFileStorageService;
import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import cn.winddol.ai.paper.api.IPaperParser;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.PaperEntity;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStateMachine;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStatus;
import cn.winddol.ai.paper.domain.ingest.StoredPaperFile;
import cn.winddol.ai.paper.domain.workspace.ArtifactType;
import cn.winddol.ai.paper.internal.PaperApplicationServiceImpl;
import cn.winddol.ai.paper.internal.PaperIngestionDispatcher;
import cn.winddol.ai.paper.internal.structure.PaperStructureNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperIngestionApplicationUnitTest {

    @Test
    void stageOrderingSeparatesCoreAndPostProcessing() {
        assertTrue(PaperIngestStage.MONKEY_OCR.isCoreStage());
        assertTrue(PaperIngestStage.PAPER_PERSISTENCE.isCoreStage());
        assertFalse(PaperIngestStage.EMBEDDING.isCoreStage());
        assertTrue(PaperIngestStage.SYMBOL_ENRICHMENT.isPostProcessingStage());
        assertEquals(PaperIngestStatus.PARSING, PaperIngestStage.METADATA_EXTRACTION.activeStatus());
        assertEquals(PaperIngestStatus.AUDITING, PaperIngestStage.LIBRARIAN_AUDIT.activeStatus());
        assertTrue(PaperIngestStateMachine.canTransition(
                PaperIngestStatus.PARSING, PaperIngestStatus.PARSED));
        assertFalse(PaperIngestStateMachine.canTransition(
                PaperIngestStatus.UPLOADED, PaperIngestStatus.READY));
    }

    @Test
    void submitPersistsAndDispatchesDurableJob() throws Exception {
        Fixture fixture = new Fixture();
        AtomicReference<PaperIngestJob> saved = new AtomicReference<>();
        when(fixture.jobRepository.createOrGetReusable(any())).thenAnswer(invocation -> {
            PaperIngestJob job = invocation.getArgument(0);
            saved.set(job);
            return job;
        });
        when(fixture.jobRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(saved.get()));

        PaperIngestJob result = fixture.service.submit(pdf());

        assertEquals(PaperIngestStatus.UPLOADED, result.getStatus());
        assertEquals("sha256", result.getFileSha256());
        verify(fixture.dispatcher).dispatch(result.getId());
        verify(fixture.jobRepository).completeStage(
                result.getId(), PaperIngestStage.FILE_VALIDATION, 1, "stored/source.pdf");
        verify(fixture.jobRepository).completeStage(
                result.getId(), PaperIngestStage.DEDUPLICATION, 1, null);
    }

    @Test
    void duplicateUploadReusesExistingJobWithoutDispatchingAgain() throws Exception {
        Fixture fixture = new Fixture();
        PaperIngestJob existing = PaperIngestJob.builder()
                .id("11111111-1111-1111-1111-111111111111")
                .fileSha256("sha256")
                .status(PaperIngestStatus.READY)
                .currentStage(PaperIngestStage.LIBRARIAN_AUDIT)
                .build();
        when(fixture.jobRepository.createOrGetReusable(any())).thenReturn(existing);

        PaperIngestJob result = fixture.service.submit(pdf());

        assertEquals(existing.getId(), result.getId());
        verify(fixture.fileStorageService).deleteJobFiles(anyString());
        verify(fixture.dispatcher, never()).dispatch(anyString());
    }

    @Test
    void failedJobCanBeRedispatchedFromItsFailedStage() throws Exception {
        Fixture fixture = new Fixture();
        PaperIngestJob failed = PaperIngestJob.builder()
                .id("11111111-1111-1111-1111-111111111111")
                .status(PaperIngestStatus.FAILED)
                .currentStage(PaperIngestStage.EMBEDDING)
                .failedStage(PaperIngestStage.EMBEDDING)
                .attemptCount(1)
                .build();
        when(fixture.jobRepository.findById(failed.getId())).thenReturn(Optional.of(failed));
        when(fixture.jobRepository.prepareRetry(failed.getId())).thenReturn(true);

        fixture.service.retry(failed.getId());

        verify(fixture.jobRepository).prepareRetry(failed.getId());
        verify(fixture.dispatcher).dispatch(failed.getId());
    }

    @Test
    void recoveryScannerRedispatchesPersistedInterruptedJobs() throws Exception {
        Fixture fixture = new Fixture();
        PaperIngestJob interrupted = PaperIngestJob.builder()
                .id("22222222-2222-2222-2222-222222222222")
                .status(PaperIngestStatus.PARSING)
                .currentStage(PaperIngestStage.STRUCTURE_NORMALIZATION)
                .attemptCount(1)
                .build();
        when(fixture.jobRepository.findRecoverable(any(), eq(10)))
                .thenReturn(List.of(interrupted));

        int recovered = fixture.service.recoverInterruptedJobs();

        assertEquals(1, recovered);
        verify(fixture.dispatcher).dispatch(interrupted.getId());
    }

    @Test
    void paperReadingAggregatesExistingPaperData() throws Exception {
        Fixture fixture = new Fixture();
        PaperEntity paper = PaperEntity.builder().id(7L).title("Mobius action").status("COMPLETED").build();
        PaperIngestJob job = PaperIngestJob.builder().id("job-7").paperId(7L).status(PaperIngestStatus.READY).build();
        when(fixture.paperRepository.selectPaperById(7L)).thenReturn(paper);
        when(fixture.jobRepository.findLatestByPaperId(7L)).thenReturn(Optional.of(job));
        when(fixture.paperRepository.findByPaperId(7L)).thenReturn(List.of());
        when(fixture.paperRepository.selectReferencesByPaperId(7L)).thenReturn(List.of());
        when(fixture.paperRepository.findRelationsByPaperId(7L)).thenReturn(List.of());

        var reading = fixture.service.getPaperReading(7L);

        assertEquals(7L, reading.getPaper().getId());
        assertEquals("job-7", reading.getLatestJobId());
    }

    @Test
    void sectionReadingUsesRealParentPath() throws Exception {
        Fixture fixture = new Fixture();
        SectionEntity parent = SectionEntity.builder().id("parent").paperId(7L).header("II. BACKGROUND").build();
        SectionEntity child = SectionEntity.builder().id("child").paperId(7L)
                .parentId("parent").header("C. Mobius group").build();
        when(fixture.paperRepository.selectSectionById("child")).thenReturn(child);
        when(fixture.paperRepository.selectSectionById("parent")).thenReturn(parent);
        when(fixture.paperRepository.selectPaperById(7L))
                .thenReturn(PaperEntity.builder().id(7L).title("Paper").build());
        when(fixture.paperRepository.selectLinksBySectionId("child")).thenReturn(List.of());
        when(fixture.paperRepository.selectReferencesByPaperId(7L)).thenReturn(List.of());
        when(fixture.paperRepository.selectSymbolsByUuids("child")).thenReturn(List.of());

        assertEquals("II. BACKGROUND > C. Mobius group",
                fixture.service.getSectionReading("child").getHeadingPath());
    }

    @Test
    void ingestionDetailsExposeOnlyExistingArtifacts() throws Exception {
        Fixture fixture = new Fixture();
        PaperIngestJob job = PaperIngestJob.builder().id("job-7")
                .rawMarkdownPath("raw.md").normalizedMarkdownPath("normalized.md").build();
        File raw = mock(File.class);
        File normalized = mock(File.class);
        when(fixture.jobRepository.findById("job-7")).thenReturn(Optional.of(job));
        when(fixture.jobRepository.findStageRuns("job-7")).thenReturn(List.of());
        when(fixture.fileStorageService.resolveFile("raw.md")).thenReturn(raw);
        when(fixture.fileStorageService.resolveFile("normalized.md")).thenReturn(normalized);
        when(raw.isFile()).thenReturn(true);
        when(normalized.isFile()).thenReturn(false);

        var details = fixture.service.getIngestionDetails("job-7");

        assertTrue(details.getArtifacts().stream()
                .anyMatch(item -> item.type() == ArtifactType.RAW_MARKDOWN && item.available()));
        assertFalse(details.getArtifacts().stream()
                .anyMatch(item -> item.type() == ArtifactType.NORMALIZED_MARKDOWN && item.available()));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "paper.pdf", "application/pdf", "%PDF-test".getBytes());
    }

    private static class Fixture {
        private final IFileStorageService fileStorageService = mock(IFileStorageService.class);
        private final IPaperParser parser = mock(IPaperParser.class);
        private final IPaperRepository paperRepository = mock(IPaperRepository.class);
        private final IPaperIngestJobRepository jobRepository = mock(IPaperIngestJobRepository.class);
        private final PaperIngestionDispatcher dispatcher = mock(PaperIngestionDispatcher.class);
        private final PaperApplicationServiceImpl service;

        private Fixture() throws Exception {
            when(fileStorageService.storeUploadedFile(anyString(), any()))
                    .thenReturn(new StoredPaperFile("stored/source.pdf", "sha256", 9));
            when(parser.parserType()).thenReturn("monkeyocr-http");
            when(parser.parserVersion()).thenReturn("test");
            service = new PaperApplicationServiceImpl(
                    fileStorageService,
                    parser,
                    paperRepository,
                    jobRepository,
                    dispatcher,
                    new PaperStructureNormalizer(),
                    10,
                    130
            );
        }
    }
}
