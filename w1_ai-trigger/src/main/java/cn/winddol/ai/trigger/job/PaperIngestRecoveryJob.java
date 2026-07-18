package cn.winddol.ai.trigger.job;

import cn.winddol.ai.paper.api.IPaperApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "paper.ingestion.recovery", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PaperIngestRecoveryJob {

    private final IPaperApplication paperApplication;

    public PaperIngestRecoveryJob(IPaperApplication paperApplication) {
        this.paperApplication = paperApplication;
    }

    @Scheduled(fixedDelayString = "${paper.ingestion.recovery.fixed-delay-ms:60000}")
    public void recoverInterruptedJobs() {
        int recovered = paperApplication.recoverInterruptedJobs();
        if (recovered > 0) {
            log.info("Redispatched {} recoverable paper ingestion job(s)", recovered);
        }
    }
}
