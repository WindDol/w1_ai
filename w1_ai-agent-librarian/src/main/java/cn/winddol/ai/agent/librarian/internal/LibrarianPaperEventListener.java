package cn.winddol.ai.agent.librarian.internal;

import cn.winddol.ai.agent.librarian.api.ILibrarianAgent;
import cn.winddol.ai.agent.librarian.api.ILibrarianRepository;
import cn.winddol.ai.domain.agent.adapter.embedding.IEmbeddingProcessor;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.service.librarianTools.CitationEnrichmentService;
import cn.winddol.ai.domain.paperTools.service.librarianTools.PaperEnrichmentService;
import cn.winddol.ai.paper.event.PaperIngestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LibrarianPaperEventListener {

    private final PaperEnrichmentService symbolService;
    private final CitationEnrichmentService citationService;
    private final IEmbeddingProcessor embeddingProcessor;
    private final IPaperRepository paperRepository;
    private final ILibrarianAgent librarianAgent;

    public LibrarianPaperEventListener(PaperEnrichmentService symbolService,
                                       CitationEnrichmentService citationService,
                                       IEmbeddingProcessor embeddingProcessor,
                                       IPaperRepository paperRepository,
                                       ILibrarianAgent librarianAgent) {
        this.symbolService = symbolService;
        this.citationService = citationService;
        this.embeddingProcessor = embeddingProcessor;
        this.paperRepository = paperRepository;
        this.librarianAgent = librarianAgent;
    }

    @Async
    @EventListener
    public void onPaperIngested(PaperIngestedEvent event) {
        long start = System.currentTimeMillis();
        Long paperId = event.getPaperId();
        log.info("Received PaperIngestedEvent for Paper [{}], starting async pipeline...", paperId);

        paperRepository.updateStatus(paperId, "AUDITING");
        try {
            log.info("Step 1/4: Extracting symbols for Paper [{}]...", paperId);
            symbolService.symbolExtractionAndStorage(paperId);

            log.info("Step 2/4: Enriching citations for Paper [{}]...", paperId);
            citationService.enrichPaperReferences(paperId);

            log.info("Step 3/4: Generating embeddings for Paper [{}]...", paperId);
            embeddingProcessor.embedReferences(paperId);
            embeddingProcessor.embedSections(paperId);

            log.info("Step 4/4: Auditing Paper [{}] against library...", paperId);
            librarianAgent.auditAgainstLibrary(paperId);

            paperRepository.updateStatus(paperId, "COMPLETED");
            log.info("Paper [{}] processing pipeline completed successfully.", paperId);

        } catch (Exception e) {
            log.error("Error processing Paper [{}]", paperId, e);
            paperRepository.updateStatusWithError(paperId, "ERROR", e.getMessage());
        }

        log.info("Paper [{}] total processing time: {}ms", paperId, System.currentTimeMillis() - start);
    }
}
