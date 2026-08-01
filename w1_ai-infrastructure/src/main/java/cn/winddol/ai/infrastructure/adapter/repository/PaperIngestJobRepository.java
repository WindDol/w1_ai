package cn.winddol.ai.infrastructure.adapter.repository;

import cn.winddol.ai.infrastructure.dao.PaperIngestJobMapper;
import cn.winddol.ai.infrastructure.dao.PaperIngestStageRunMapper;
import cn.winddol.ai.infrastructure.dao.po.PaperIngestJobPO;
import cn.winddol.ai.infrastructure.dao.po.PaperIngestStageRunPO;
import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import cn.winddol.ai.paper.domain.ingest.IngestStageRunStatus;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStageRun;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStateMachine;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PaperIngestJobRepository implements IPaperIngestJobRepository {

    private final PaperIngestJobMapper jobMapper;
    private final PaperIngestStageRunMapper stageRunMapper;

    public PaperIngestJobRepository(PaperIngestJobMapper jobMapper,
                                    PaperIngestStageRunMapper stageRunMapper) {
        this.jobMapper = jobMapper;
        this.stageRunMapper = stageRunMapper;
    }

    @Override
    public PaperIngestJob createOrGetReusable(PaperIngestJob job) {
        PaperIngestJobPO reusable = findReusable(job.getFileSha256());
        if (reusable != null) {
            return toDomain(reusable);
        }
        try {
            jobMapper.insert(toPo(job));
            return job;
        } catch (DuplicateKeyException duplicate) {
            PaperIngestJobPO concurrent = findReusable(job.getFileSha256());
            if (concurrent == null) {
                throw duplicate;
            }
            return toDomain(concurrent);
        }
    }

    @Override
    public Optional<PaperIngestJob> findById(String jobId) {
        return Optional.ofNullable(jobMapper.selectById(jobId)).map(this::toDomain);
    }

    @Override
    public Optional<PaperIngestJob> findLatestByPaperId(Long paperId) {
        if (paperId == null) {
            return Optional.empty();
        }
        PaperIngestJobPO job = jobMapper.selectOne(new LambdaQueryWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getPaperId, paperId)
                .orderByDesc(PaperIngestJobPO::getCreatedAt)
                .last("LIMIT 1"));
        return Optional.ofNullable(job).map(this::toDomain);
    }

    @Override
    public List<PaperIngestStageRun> findStageRuns(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return List.of();
        }
        return stageRunMapper.selectList(new LambdaQueryWrapper<PaperIngestStageRunPO>()
                        .eq(PaperIngestStageRunPO::getJobId, jobId)
                        .orderByAsc(PaperIngestStageRunPO::getStartedAt)
                        .orderByAsc(PaperIngestStageRunPO::getId))
                .stream().map(this::toStageRun).toList();
    }

    @Override
    public List<PaperIngestJob> findRecoverable(LocalDateTime staleBefore, int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime queuedBefore = LocalDateTime.now().minusMinutes(1);
        return jobMapper.selectList(new LambdaQueryWrapper<PaperIngestJobPO>()
                        .and(wrapper -> wrapper
                                .nested(queued -> queued
                                        .eq(PaperIngestJobPO::getStatus, PaperIngestStatus.UPLOADED.name())
                                        .lt(PaperIngestJobPO::getUpdatedAt, queuedBefore))
                                .or(nested -> nested
                                        .in(PaperIngestJobPO::getStatus,
                                                PaperIngestStatus.PARSING.name(),
                                                PaperIngestStatus.PARSED.name(),
                                                PaperIngestStatus.AUDITING.name())
                                        .lt(PaperIngestJobPO::getUpdatedAt, staleBefore)))
                        .and(lease -> lease.isNull(PaperIngestJobPO::getWorkerToken)
                                .or().isNull(PaperIngestJobPO::getLeaseUntil)
                                .or().lt(PaperIngestJobPO::getLeaseUntil, now))
                        .orderByAsc(PaperIngestJobPO::getUpdatedAt)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean tryAcquire(String jobId, String workerToken, LocalDateTime leaseUntil) {
        LocalDateTime now = LocalDateTime.now();
        return jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .notIn(PaperIngestJobPO::getStatus,
                        PaperIngestStatus.READY.name(), PaperIngestStatus.FAILED.name())
                .and(wrapper -> wrapper.isNull(PaperIngestJobPO::getWorkerToken)
                        .or().isNull(PaperIngestJobPO::getLeaseUntil)
                        .or().lt(PaperIngestJobPO::getLeaseUntil, now))
                .set(PaperIngestJobPO::getWorkerToken, workerToken)
                .set(PaperIngestJobPO::getLeaseUntil, leaseUntil)
                .set(PaperIngestJobPO::getUpdatedAt, now)
                .setSql("version = version + 1")) == 1;
    }

    @Override
    public boolean renewLease(String jobId, String workerToken, LocalDateTime leaseUntil) {
        LocalDateTime now = LocalDateTime.now();
        return jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .eq(PaperIngestJobPO::getWorkerToken, workerToken)
                .notIn(PaperIngestJobPO::getStatus,
                        PaperIngestStatus.READY.name(), PaperIngestStatus.FAILED.name())
                .set(PaperIngestJobPO::getLeaseUntil, leaseUntil)
                .set(PaperIngestJobPO::getUpdatedAt, now)
                .setSql("version = version + 1")) == 1;
    }

    @Override
    public void release(String jobId, String workerToken) {
        jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .eq(PaperIngestJobPO::getWorkerToken, workerToken)
                .set(PaperIngestJobPO::getWorkerToken, null)
                .set(PaperIngestJobPO::getLeaseUntil, null)
                .set(PaperIngestJobPO::getUpdatedAt, LocalDateTime.now())
                .setSql("version = version + 1"));
    }

    @Override
    @Transactional
    public void startStage(String jobId, PaperIngestStage stage, PaperIngestStatus status,
                           int attempt, LocalDateTime leaseUntil) {
        LocalDateTime now = LocalDateTime.now();
        requireTransition(jobId, status);
        jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .set(PaperIngestJobPO::getStatus, status.name())
                .set(PaperIngestJobPO::getCurrentStage, stage.name())
                .set(PaperIngestJobPO::getFailedStage, null)
                .set(PaperIngestJobPO::getErrorCode, null)
                .set(PaperIngestJobPO::getErrorMessage, null)
                .set(PaperIngestJobPO::getUpdatedAt, now)
                .set(PaperIngestJobPO::getLeaseUntil, leaseUntil)
                .setSql("started_at = COALESCE(started_at, CURRENT_TIMESTAMP), version = version + 1"));
        stageRunMapper.update(null, new LambdaUpdateWrapper<PaperIngestStageRunPO>()
                .eq(PaperIngestStageRunPO::getJobId, jobId)
                .eq(PaperIngestStageRunPO::getStatus, IngestStageRunStatus.RUNNING.name())
                .set(PaperIngestStageRunPO::getStatus, IngestStageRunStatus.FAILED.name())
                .set(PaperIngestStageRunPO::getErrorCode, "WORKER_LEASE_EXPIRED")
                .set(PaperIngestStageRunPO::getErrorMessage,
                        "A new worker resumed the job before this stage run completed")
                .set(PaperIngestStageRunPO::getFinishedAt, now));
        stageRunMapper.insert(PaperIngestStageRunPO.builder()
                .jobId(jobId)
                .stage(stage.name())
                .attempt(attempt)
                .status(IngestStageRunStatus.RUNNING.name())
                .startedAt(now)
                .build());
    }

    @Override
    @Transactional
    public void completeStage(String jobId, PaperIngestStage stage, int attempt, String artifactPath) {
        LocalDateTime now = LocalDateTime.now();
        PaperIngestStageRunPO run = findLatestRun(jobId, stage, attempt);
        if (run != null) {
            run.setStatus(IngestStageRunStatus.SUCCEEDED.name());
            run.setArtifactPath(artifactPath);
            run.setFinishedAt(now);
            stageRunMapper.updateById(run);
        }
        touch(jobId, now);
    }

    @Override
    @Transactional
    public void failStage(String jobId, PaperIngestStage stage, int attempt,
                          String errorCode, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .set(PaperIngestJobPO::getStatus, PaperIngestStatus.FAILED.name())
                .set(PaperIngestJobPO::getCurrentStage, stage.name())
                .set(PaperIngestJobPO::getFailedStage, stage.name())
                .set(PaperIngestJobPO::getErrorCode, errorCode)
                .set(PaperIngestJobPO::getErrorMessage, abbreviate(errorMessage, 4000))
                .set(PaperIngestJobPO::getUpdatedAt, now)
                .set(PaperIngestJobPO::getLeaseUntil, null)
                .setSql("version = version + 1"));
        PaperIngestStageRunPO run = findLatestRun(jobId, stage, attempt);
        if (run != null) {
            run.setStatus(IngestStageRunStatus.FAILED.name());
            run.setErrorCode(errorCode);
            run.setErrorMessage(abbreviate(errorMessage, 4000));
            run.setFinishedAt(now);
            stageRunMapper.updateById(run);
        }
    }

    @Override
    public void updateParserArtifacts(String jobId, String rawMarkdownPath, String parserArtifactPath) {
        update(jobId, wrapper -> wrapper
                .set(PaperIngestJobPO::getRawMarkdownPath, rawMarkdownPath)
                .set(PaperIngestJobPO::getParserArtifactPath, parserArtifactPath));
    }

    @Override
    public void updateNormalizedArtifacts(String jobId, String markdownPath, String reportPath) {
        update(jobId, wrapper -> wrapper
                .set(PaperIngestJobPO::getNormalizedMarkdownPath, markdownPath)
                .set(PaperIngestJobPO::getNormalizationReportPath, reportPath));
    }

    @Override
    public void updateMetadata(String jobId, String metadataPath, String title, String abstractText,
                               String fingerprint, Integer publicationYear) {
        update(jobId, wrapper -> wrapper
                .set(PaperIngestJobPO::getMetadataPath, metadataPath)
                .set(PaperIngestJobPO::getExtractedTitle, title)
                .set(PaperIngestJobPO::getExtractedAbstract, abstractText)
                .set(PaperIngestJobPO::getPaperFingerprint, fingerprint)
                .set(PaperIngestJobPO::getPublicationYear, publicationYear));
    }

    @Override
    public void attachPaper(String jobId, Long paperId) {
        update(jobId, wrapper -> wrapper.set(PaperIngestJobPO::getPaperId, paperId));
    }

    @Override
    public void markParsed(String jobId) {
        requireTransition(jobId, PaperIngestStatus.PARSED);
        update(jobId, wrapper -> wrapper
                .set(PaperIngestJobPO::getStatus, PaperIngestStatus.PARSED.name())
                .set(PaperIngestJobPO::getCurrentStage, PaperIngestStage.SYMBOL_ENRICHMENT.name()));
    }

    @Override
    public void markReady(String jobId) {
        LocalDateTime now = LocalDateTime.now();
        requireTransition(jobId, PaperIngestStatus.READY);
        jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .set(PaperIngestJobPO::getStatus, PaperIngestStatus.READY.name())
                .set(PaperIngestJobPO::getCurrentStage, PaperIngestStage.LIBRARIAN_AUDIT.name())
                .set(PaperIngestJobPO::getFailedStage, null)
                .set(PaperIngestJobPO::getErrorCode, null)
                .set(PaperIngestJobPO::getErrorMessage, null)
                .set(PaperIngestJobPO::getCompletedAt, now)
                .set(PaperIngestJobPO::getUpdatedAt, now)
                .setSql("version = version + 1"));
    }

    @Override
    public void updateProcessingVersions(String jobId, String parserType,
                                         String parserVersion, String normalizerVersion) {
        update(jobId, wrapper -> wrapper
                .set(PaperIngestJobPO::getParserType, parserType)
                .set(PaperIngestJobPO::getParserVersion, parserVersion)
                .set(PaperIngestJobPO::getNormalizerVersion, normalizerVersion));
    }

    @Override
    public boolean prepareRetry(String jobId) {
        return jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .eq(PaperIngestJobPO::getStatus, PaperIngestStatus.FAILED.name())
                .isNotNull(PaperIngestJobPO::getFailedStage)
                .setSql("status = CASE WHEN failed_stage IN "
                        + "('SYMBOL_ENRICHMENT','CITATION_ENRICHMENT','EMBEDDING','LIBRARIAN_AUDIT') "
                        + "THEN 'PARSED' ELSE 'UPLOADED' END, current_stage = failed_stage")
                .set(PaperIngestJobPO::getErrorCode, null)
                .set(PaperIngestJobPO::getErrorMessage, null)
                .set(PaperIngestJobPO::getWorkerToken, null)
                .set(PaperIngestJobPO::getLeaseUntil, null)
                .set(PaperIngestJobPO::getCompletedAt, null)
                .setSql("attempt_count = attempt_count + 1, version = version + 1")
                .set(PaperIngestJobPO::getUpdatedAt, LocalDateTime.now())) == 1;
    }

    @Override
    public boolean prepareRerun(String jobId, PaperIngestStage stage) {
        LambdaUpdateWrapper<PaperIngestJobPO> update = new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .in(PaperIngestJobPO::getStatus,
                        PaperIngestStatus.READY.name(), PaperIngestStatus.FAILED.name())
                .set(PaperIngestJobPO::getStatus, stage.isPostProcessingStage()
                        ? PaperIngestStatus.PARSED.name()
                        : PaperIngestStatus.UPLOADED.name())
                .set(PaperIngestJobPO::getCurrentStage, stage.name())
                .set(PaperIngestJobPO::getFailedStage, null)
                .set(PaperIngestJobPO::getErrorCode, null)
                .set(PaperIngestJobPO::getErrorMessage, null)
                .set(PaperIngestJobPO::getWorkerToken, null)
                .set(PaperIngestJobPO::getLeaseUntil, null)
                .set(PaperIngestJobPO::getCompletedAt, null)
                .set(PaperIngestJobPO::getUpdatedAt, LocalDateTime.now())
                .setSql("attempt_count = attempt_count + 1, version = version + 1");

        if (stage.isBeforeOrEqual(PaperIngestStage.MONKEY_OCR)) {
            update.set(PaperIngestJobPO::getRawMarkdownPath, null)
                    .set(PaperIngestJobPO::getParserArtifactPath, null);
        }
        if (stage.isBeforeOrEqual(PaperIngestStage.STRUCTURE_NORMALIZATION)) {
            update.set(PaperIngestJobPO::getNormalizedMarkdownPath, null)
                    .set(PaperIngestJobPO::getNormalizationReportPath, null);
        }
        if (stage.isBeforeOrEqual(PaperIngestStage.METADATA_EXTRACTION)) {
            update.set(PaperIngestJobPO::getMetadataPath, null)
                    .set(PaperIngestJobPO::getExtractedTitle, null)
                    .set(PaperIngestJobPO::getExtractedAbstract, null)
                    .set(PaperIngestJobPO::getPaperFingerprint, null)
                    .set(PaperIngestJobPO::getPublicationYear, null);
        }
        return jobMapper.update(null, update) == 1;
    }

    private PaperIngestJobPO findReusable(String sha256) {
        return jobMapper.selectOne(new LambdaQueryWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getFileSha256, sha256)
                .orderByDesc(PaperIngestJobPO::getCreatedAt)
                .last("LIMIT 1"));
    }

    private PaperIngestStageRunPO findLatestRun(String jobId, PaperIngestStage stage, int attempt) {
        return stageRunMapper.selectOne(new LambdaQueryWrapper<PaperIngestStageRunPO>()
                .eq(PaperIngestStageRunPO::getJobId, jobId)
                .eq(PaperIngestStageRunPO::getStage, stage.name())
                .eq(PaperIngestStageRunPO::getAttempt, attempt)
                .eq(PaperIngestStageRunPO::getStatus, IngestStageRunStatus.RUNNING.name())
                .orderByDesc(PaperIngestStageRunPO::getId)
                .last("LIMIT 1"));
    }

    private void touch(String jobId, LocalDateTime now) {
        jobMapper.update(null, new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId)
                .set(PaperIngestJobPO::getUpdatedAt, now)
                .setSql("version = version + 1"));
    }

    private void update(String jobId,
                        java.util.function.UnaryOperator<LambdaUpdateWrapper<PaperIngestJobPO>> customizer) {
        LambdaUpdateWrapper<PaperIngestJobPO> wrapper = new LambdaUpdateWrapper<PaperIngestJobPO>()
                .eq(PaperIngestJobPO::getId, jobId);
        wrapper = customizer.apply(wrapper)
                .set(PaperIngestJobPO::getUpdatedAt, LocalDateTime.now())
                .setSql("version = version + 1");
        jobMapper.update(null, wrapper);
    }

    private PaperIngestJobPO toPo(PaperIngestJob job) {
        return PaperIngestJobPO.builder()
                .id(job.getId()).paperId(job.getPaperId())
                .originalFilename(job.getOriginalFilename()).fileSha256(job.getFileSha256())
                .fileSize(job.getFileSize()).sourceFilePath(job.getSourceFilePath())
                .status(name(job.getStatus())).currentStage(name(job.getCurrentStage()))
                .failedStage(name(job.getFailedStage())).errorCode(job.getErrorCode())
                .errorMessage(job.getErrorMessage()).attemptCount(job.getAttemptCount())
                .parserType(job.getParserType()).parserVersion(job.getParserVersion())
                .normalizerVersion(job.getNormalizerVersion())
                .parserArtifactPath(job.getParserArtifactPath())
                .rawMarkdownPath(job.getRawMarkdownPath())
                .normalizedMarkdownPath(job.getNormalizedMarkdownPath())
                .normalizationReportPath(job.getNormalizationReportPath())
                .metadataPath(job.getMetadataPath()).extractedTitle(job.getExtractedTitle())
                .extractedAbstract(job.getExtractedAbstract()).paperFingerprint(job.getPaperFingerprint())
                .publicationYear(job.getPublicationYear()).workerToken(job.getWorkerToken())
                .leaseUntil(job.getLeaseUntil()).version(job.getVersion())
                .createdAt(job.getCreatedAt()).updatedAt(job.getUpdatedAt())
                .startedAt(job.getStartedAt()).completedAt(job.getCompletedAt()).build();
    }

    private PaperIngestJob toDomain(PaperIngestJobPO po) {
        return PaperIngestJob.builder()
                .id(po.getId()).paperId(po.getPaperId())
                .originalFilename(po.getOriginalFilename()).fileSha256(po.getFileSha256())
                .fileSize(po.getFileSize()).sourceFilePath(po.getSourceFilePath())
                .status(enumValue(PaperIngestStatus.class, po.getStatus()))
                .currentStage(enumValue(PaperIngestStage.class, po.getCurrentStage()))
                .failedStage(enumValue(PaperIngestStage.class, po.getFailedStage()))
                .errorCode(po.getErrorCode()).errorMessage(po.getErrorMessage())
                .attemptCount(po.getAttemptCount()).parserType(po.getParserType())
                .parserVersion(po.getParserVersion()).normalizerVersion(po.getNormalizerVersion())
                .parserArtifactPath(po.getParserArtifactPath())
                .rawMarkdownPath(po.getRawMarkdownPath())
                .normalizedMarkdownPath(po.getNormalizedMarkdownPath())
                .normalizationReportPath(po.getNormalizationReportPath())
                .metadataPath(po.getMetadataPath()).extractedTitle(po.getExtractedTitle())
                .extractedAbstract(po.getExtractedAbstract()).paperFingerprint(po.getPaperFingerprint())
                .publicationYear(po.getPublicationYear()).workerToken(po.getWorkerToken())
                .leaseUntil(po.getLeaseUntil()).version(po.getVersion())
                .createdAt(po.getCreatedAt()).updatedAt(po.getUpdatedAt())
                .startedAt(po.getStartedAt()).completedAt(po.getCompletedAt()).build();
    }

    private PaperIngestStageRun toStageRun(PaperIngestStageRunPO po) {
        return PaperIngestStageRun.builder()
                .id(po.getId())
                .jobId(po.getJobId())
                .stage(enumValue(PaperIngestStage.class, po.getStage()))
                .attempt(po.getAttempt())
                .status(enumValue(IngestStageRunStatus.class, po.getStatus()))
                .artifactPath(po.getArtifactPath())
                .errorCode(po.getErrorCode())
                .errorMessage(po.getErrorMessage())
                .startedAt(po.getStartedAt())
                .finishedAt(po.getFinishedAt())
                .build();
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void requireTransition(String jobId, PaperIngestStatus target) {
        PaperIngestJobPO current = jobMapper.selectById(jobId);
        if (current == null) {
            throw new IllegalArgumentException("Paper ingestion job not found: " + jobId);
        }
        PaperIngestStateMachine.requireTransition(
                enumValue(PaperIngestStatus.class, current.getStatus()), target);
    }
}
