package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStageRun;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IPaperIngestJobRepository {

    PaperIngestJob createOrGetReusable(PaperIngestJob job);

    Optional<PaperIngestJob> findById(String jobId);

    Optional<PaperIngestJob> findLatestByPaperId(Long paperId);

    List<PaperIngestStageRun> findStageRuns(String jobId);

    List<PaperIngestJob> findRecoverable(LocalDateTime staleBefore, int limit);

    boolean tryAcquire(String jobId, String workerToken, LocalDateTime leaseUntil);

    boolean renewLease(String jobId, String workerToken, LocalDateTime leaseUntil);

    void release(String jobId, String workerToken);

    void startStage(String jobId, PaperIngestStage stage, PaperIngestStatus status,
                    int attempt, LocalDateTime leaseUntil);

    void completeStage(String jobId, PaperIngestStage stage, int attempt, String artifactPath);

    void failStage(String jobId, PaperIngestStage stage, int attempt,
                   String errorCode, String errorMessage);

    void updateParserArtifacts(String jobId, String rawMarkdownPath, String parserArtifactPath);

    void updateNormalizedArtifacts(String jobId, String markdownPath, String reportPath);

    void updateMetadata(String jobId, String metadataPath, String title, String abstractText,
                        String fingerprint, Integer publicationYear);

    void attachPaper(String jobId, Long paperId);

    void markParsed(String jobId);

    void markReady(String jobId);

    void updateProcessingVersions(String jobId, String parserType,
                                  String parserVersion, String normalizerVersion);

    boolean prepareRetry(String jobId);

    boolean prepareRerun(String jobId, PaperIngestStage stage);
}
