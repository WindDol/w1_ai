package cn.winddol.ai.agent.librarian.internal;

import cn.winddol.ai.agent.librarian.api.ILibrarianAgent;
import cn.winddol.ai.agent.librarian.api.ILibrarianRepository;
import cn.winddol.ai.domain.agent.adapter.embedding.IEmbeddingProcessor;
import cn.winddol.ai.paper.api.IEmbeddingService;
import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.api.PaperIngestedEvent;
import cn.winddol.ai.paper.domain.PaperEntity;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStatus;
import cn.winddol.ai.shared.api.ICitationEnrichmentService;
import cn.winddol.ai.shared.api.IPaperEnrichmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class LibrarianPaperEventListener {

    private final IPaperEnrichmentService symbolService;
    private final ICitationEnrichmentService citationService;
    private final IEmbeddingProcessor embeddingProcessor;
    private final IEmbeddingService embeddingService;
    private final IPaperRepository paperRepository;
    private final IPaperIngestJobRepository jobRepository;
    private final ILibrarianAgent librarianAgent;

    public LibrarianPaperEventListener(IPaperEnrichmentService symbolService,
                                       ICitationEnrichmentService citationService,
                                       IEmbeddingProcessor embeddingProcessor,
                                       IEmbeddingService embeddingService,
                                       IPaperRepository paperRepository,
                                       IPaperIngestJobRepository jobRepository,
                                       ILibrarianAgent librarianAgent) {
        this.symbolService = symbolService;
        this.citationService = citationService;
        this.embeddingProcessor = embeddingProcessor;
        this.embeddingService = embeddingService;
        this.paperRepository = paperRepository;
        this.jobRepository = jobRepository;
        this.librarianAgent = librarianAgent;
    }

    @EventListener
    public void onPaperIngested(PaperIngestedEvent event) {
        long start = System.currentTimeMillis();
        String jobId = event.getJobId();
        Long paperId = event.getPaperId();
        int attempt = event.getAttempt();
        PaperIngestStage currentStage = event.getStartStage();
        log.info("Received PaperIngestedEvent for Paper [{}], Job [{}], starting at {}",
                paperId, jobId, currentStage);

        paperRepository.updateStatus(paperId, PaperIngestStatus.AUDITING.name());
        try {
            if (shouldRun(currentStage, PaperIngestStage.SYMBOL_ENRICHMENT)) {
                currentStage = PaperIngestStage.SYMBOL_ENRICHMENT;
                runStage(jobId, currentStage, attempt, () -> symbolService.symbolExtractionAndStorage(paperId));
            }

            if (shouldRun(event.getStartStage(), PaperIngestStage.CITATION_ENRICHMENT)) {
                currentStage = PaperIngestStage.CITATION_ENRICHMENT;
                runStage(jobId, currentStage, attempt, () -> citationService.enrichPaperReferences(paperId));
            }

            if (shouldRun(event.getStartStage(), PaperIngestStage.EMBEDDING)) {
                currentStage = PaperIngestStage.EMBEDDING;
                runStage(jobId, currentStage, attempt, () -> {
                    PaperEntity paper = paperRepository.getPaperDetailsById(paperId);
                    if (paper == null) {
                        throw new IllegalStateException("Paper not found: " + paperId);
                    }
                    String embeddingText = paper.getTitle() + "\n"
                            + (paper.getAbstractText() == null ? "" : paper.getAbstractText());
                    paperRepository.updatePaperEmbedding(paperId, embeddingService.embed(embeddingText));
                    embeddingProcessor.embedReferences(paperId);
                    embeddingProcessor.embedSections(paperId);
                });
            }

            if (shouldRun(event.getStartStage(), PaperIngestStage.LIBRARIAN_AUDIT)) {
                currentStage = PaperIngestStage.LIBRARIAN_AUDIT;
                runStage(jobId, currentStage, attempt, () -> librarianAgent.auditAgainstLibrary(paperId));
            }

            paperRepository.updateStatus(paperId, PaperIngestStatus.READY.name());
            jobRepository.markReady(jobId);
            log.info("Paper [{}], Job [{}] processing pipeline completed successfully.", paperId, jobId);

        } catch (Exception e) {
            String message = rootMessage(e);
            log.error("Error processing Paper [{}], Job [{}] at {}", paperId, jobId, currentStage, e);
            jobRepository.failStage(jobId, currentStage, attempt,
                    currentStage.name() + "_FAILED", message);
            paperRepository.updateStatusWithError(paperId, PaperIngestStatus.FAILED.name(), message);
        }

        log.info("Paper [{}], Job [{}] total processing time: {}ms",
                paperId, jobId, System.currentTimeMillis() - start);
    }

    private void runStage(String jobId, PaperIngestStage stage, int attempt, StageAction action) throws Exception {
        jobRepository.startStage(jobId, stage, PaperIngestStatus.AUDITING,
                attempt, LocalDateTime.now().plusHours(2));
        action.run();
        jobRepository.completeStage(jobId, stage, attempt, null);
    }

    private boolean shouldRun(PaperIngestStage start, PaperIngestStage candidate) {
        return start.ordinal() <= candidate.ordinal();
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @FunctionalInterface
    private interface StageAction {
        void run() throws Exception;
    }
}
