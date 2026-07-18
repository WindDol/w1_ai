package cn.winddol.ai.test;

import cn.winddol.ai.paper.api.IFingerprintUtils;
import cn.winddol.ai.paper.api.IFileStorageService;
import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import cn.winddol.ai.paper.api.IPaperParser;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.api.ISymbolExtractor;
import cn.winddol.ai.paper.api.PaperIngestedEvent;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStatus;
import cn.winddol.ai.paper.internal.PaperIngestionWorkflow;
import cn.winddol.ai.paper.internal.structure.PaperStructureNormalizer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperIngestionWorkflowUnitTest {

    @Test
    void workerWithoutDispatchReservationDoesNotStartPipeline() {
        Fixture fixture = new Fixture(job(PaperIngestStatus.PARSING, PaperIngestStage.MONKEY_OCR, null));
        when(fixture.jobRepository.renewLease(eq(Fixture.JOB_ID), eq(Fixture.WORKER_TOKEN), any()))
                .thenReturn(false);

        fixture.workflow.process(Fixture.JOB_ID, Fixture.WORKER_TOKEN);

        verify(fixture.heartbeatScheduler, never()).scheduleAtFixedRate(
                any(Runnable.class), anyLong(), anyLong(), eq(TimeUnit.SECONDS));
        verify(fixture.parser, never()).parsePdf(anyString());
        verify(fixture.eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void missingSourceIsAuditedAndLeaseIsReleased() {
        Fixture fixture = new Fixture(job(PaperIngestStatus.UPLOADED, PaperIngestStage.MONKEY_OCR, null));
        when(fixture.fileStorageService.resolveFile(anyString()))
                .thenReturn(new File("missing-paper-ingestion-source.pdf"));

        fixture.workflow.process(Fixture.JOB_ID, Fixture.WORKER_TOKEN);

        verify(fixture.jobRepository, atLeastOnce()).failStage(
                eq(Fixture.JOB_ID), eq(PaperIngestStage.MONKEY_OCR), eq(1),
                eq("SOURCE_FILE_MISSING"), anyString());
        verify(fixture.heartbeatFuture).cancel(false);
        verify(fixture.jobRepository).release(eq(Fixture.JOB_ID), anyString());
        verify(fixture.eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void embeddingRetrySkipsOcrAndPublishesPostProcessingStartStage() {
        Fixture fixture = new Fixture(job(
                PaperIngestStatus.PARSED, PaperIngestStage.EMBEDDING, 42L));
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        fixture.workflow.process(Fixture.JOB_ID, Fixture.WORKER_TOKEN);

        verify(fixture.parser, never()).parsePdf(anyString());
        verify(fixture.paperRepository).resetDerivedDataFrom(42L, PaperIngestStage.EMBEDDING);
        verify(fixture.eventPublisher).publishEvent(eventCaptor.capture());
        PaperIngestedEvent event = (PaperIngestedEvent) eventCaptor.getValue();
        assertEquals(PaperIngestStage.EMBEDDING, event.getStartStage());
        assertEquals(42L, event.getPaperId());
    }

    @Test
    void heartbeatRenewsTheOwnedLease() {
        Fixture fixture = new Fixture(job(PaperIngestStatus.UPLOADED, PaperIngestStage.MONKEY_OCR, null));
        when(fixture.fileStorageService.resolveFile(anyString()))
                .thenReturn(new File("missing-paper-ingestion-source.pdf"));
        ArgumentCaptor<Runnable> heartbeatCaptor = ArgumentCaptor.forClass(Runnable.class);

        fixture.workflow.process(Fixture.JOB_ID, Fixture.WORKER_TOKEN);
        verify(fixture.heartbeatScheduler).scheduleAtFixedRate(
                heartbeatCaptor.capture(), anyLong(), anyLong(), eq(TimeUnit.SECONDS));
        heartbeatCaptor.getValue().run();

        verify(fixture.jobRepository, atLeast(2)).renewLease(
                eq(Fixture.JOB_ID), anyString(), any(LocalDateTime.class));
    }

    @Test
    void heartbeatSchedulerFailureIsAuditedAtResumeStage() {
        Fixture fixture = new Fixture(job(
                PaperIngestStatus.PARSED, PaperIngestStage.EMBEDDING, 42L));
        when(fixture.heartbeatScheduler.scheduleAtFixedRate(
                any(Runnable.class), anyLong(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new IllegalStateException("scheduler unavailable"));

        fixture.workflow.process(Fixture.JOB_ID, Fixture.WORKER_TOKEN);

        verify(fixture.jobRepository).failStage(
                Fixture.JOB_ID,
                PaperIngestStage.EMBEDDING,
                1,
                "INGESTION_HEARTBEAT_UNAVAILABLE",
                "scheduler unavailable");
        verify(fixture.paperRepository).updateStatusWithError(
                42L, PaperIngestStatus.FAILED.name(), "scheduler unavailable");
    }

    private static PaperIngestJob job(PaperIngestStatus status,
                                      PaperIngestStage stage,
                                      Long paperId) {
        return PaperIngestJob.builder()
                .id(Fixture.JOB_ID)
                .paperId(paperId)
                .sourceFilePath("stored/source.pdf")
                .status(status)
                .currentStage(stage)
                .attemptCount(1)
                .extractedTitle("Test paper")
                .build();
    }

    private static class Fixture {
        private static final String JOB_ID = "11111111-1111-1111-1111-111111111111";
        private static final String WORKER_TOKEN = "22222222-2222-2222-2222-222222222222";

        private final IFileStorageService fileStorageService = mock(IFileStorageService.class);
        private final IPaperParser parser = mock(IPaperParser.class);
        private final ISymbolExtractor symbolExtractor = mock(ISymbolExtractor.class);
        private final IPaperRepository paperRepository = mock(IPaperRepository.class);
        private final IPaperIngestJobRepository jobRepository = mock(IPaperIngestJobRepository.class);
        private final IFingerprintUtils fingerprintUtils = mock(IFingerprintUtils.class);
        private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        private final ScheduledExecutorService heartbeatScheduler = mock(ScheduledExecutorService.class);
        private final ScheduledFuture<?> heartbeatFuture = mock(ScheduledFuture.class);
        private final PaperIngestionWorkflow workflow;

        private Fixture(PaperIngestJob job) {
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(jobRepository.renewLease(eq(JOB_ID), eq(WORKER_TOKEN), any(LocalDateTime.class)))
                    .thenReturn(true);
            doReturn(heartbeatFuture).when(heartbeatScheduler).scheduleAtFixedRate(
                    any(Runnable.class), anyLong(), anyLong(), eq(TimeUnit.SECONDS));
            workflow = new PaperIngestionWorkflow(
                    fileStorageService,
                    parser,
                    symbolExtractor,
                    paperRepository,
                    jobRepository,
                    fingerprintUtils,
                    eventPublisher,
                    new PaperStructureNormalizer(),
                    heartbeatScheduler,
                    120,
                    30
            );
        }
    }
}
