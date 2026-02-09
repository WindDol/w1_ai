package cn.winddol.ai.infrastructure.scheduler;


import cn.winddol.ai.domain.paperTools.service.librarianTools.CitationEnrichmentService;
import cn.winddol.ai.infrastructure.embedding.EmbeddingProcessor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class GlobalScheduler {
    @Resource
    private CitationEnrichmentService citationService;
    @Resource
    private EmbeddingProcessor embeddingProcessor;

    // 每天凌晨 2 点执行兜底
    @Scheduled(cron = "0 0 * * * ?")
    public void runCitationCleanup() {
        log.info("⏰ Starting nightly citation cleanup...");
        citationService.enrichReferences();
    }

    @Scheduled(cron = "0 10 * * * ?")
    public void runSymbolsCleanup() {
        log.info("⏰ Starting nightly symbols cleanup...");
        embeddingProcessor.embedSymbols();
    }
    @Scheduled(cron = "0 20 * * * ?")
    public void runSectionsCleanup() {
        log.info("⏰ Starting nightly sections cleanup...");
        embeddingProcessor.embedSections();
    }
}
