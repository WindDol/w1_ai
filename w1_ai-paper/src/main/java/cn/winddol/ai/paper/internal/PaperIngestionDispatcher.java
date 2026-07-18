package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class PaperIngestionDispatcher {

    private final TaskExecutor executor;
    private final PaperIngestionWorkflow workflow;
    private final IPaperIngestJobRepository jobRepository;
    private final Duration leaseDuration;

    public PaperIngestionDispatcher(@Qualifier("paperIngestionExecutor") TaskExecutor executor,
                                    PaperIngestionWorkflow workflow,
                                    IPaperIngestJobRepository jobRepository,
                                    @Value("${paper.ingestion.lease-minutes:120}") long leaseMinutes) {
        this.executor = executor;
        this.workflow = workflow;
        this.jobRepository = jobRepository;
        this.leaseDuration = Duration.ofMinutes(Math.max(10, leaseMinutes));
    }

    public void dispatch(String jobId) {
        String workerToken = UUID.randomUUID().toString();
        if (!jobRepository.tryAcquire(jobId, workerToken,
                LocalDateTime.now().plus(leaseDuration))) {
            log.info("Paper ingestion job [{}] is already queued or owned by another worker", jobId);
            return;
        }
        try {
            executor.execute(() -> workflow.process(jobId, workerToken));
            log.info("Paper ingestion job [{}] dispatched", jobId);
        } catch (RuntimeException dispatchError) {
            jobRepository.release(jobId, workerToken);
            throw dispatchError;
        }
    }
}
