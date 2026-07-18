package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IFingerprintUtils;
import cn.winddol.ai.paper.api.IFileStorageService;
import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import cn.winddol.ai.paper.api.IPaperParser;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.api.ISymbolExtractor;
import cn.winddol.ai.paper.api.PaperIngestedEvent;
import cn.winddol.ai.paper.domain.RefMetadata;
import cn.winddol.ai.paper.domain.SectionPO;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStatus;
import cn.winddol.ai.paper.domain.ingest.PdfParseResult;
import cn.winddol.ai.paper.internal.structure.HeadingDecision;
import cn.winddol.ai.paper.internal.structure.PaperStructureNormalizationResult;
import cn.winddol.ai.paper.internal.structure.PaperStructureNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PaperIngestionWorkflow {

    private final IFileStorageService fileStorageService;
    private final IPaperParser parser;
    private final ISymbolExtractor symbolExtractor;
    private final IPaperRepository paperRepository;
    private final IPaperIngestJobRepository jobRepository;
    private final IFingerprintUtils fingerprintUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final PaperStructureNormalizer structureNormalizer;
    private final ScheduledExecutorService heartbeatScheduler;
    private final Duration leaseDuration;
    private final Duration heartbeatInterval;

    public PaperIngestionWorkflow(IFileStorageService fileStorageService,
                                  IPaperParser parser,
                                  ISymbolExtractor symbolExtractor,
                                  IPaperRepository paperRepository,
                                  IPaperIngestJobRepository jobRepository,
                                  IFingerprintUtils fingerprintUtils,
                                  ApplicationEventPublisher eventPublisher,
                                  PaperStructureNormalizer structureNormalizer,
                                  @Qualifier("paperIngestionHeartbeatScheduler")
                                  ScheduledExecutorService heartbeatScheduler,
                                  @Value("${paper.ingestion.lease-minutes:120}") long leaseMinutes,
                                  @Value("${paper.ingestion.heartbeat-seconds:30}") long heartbeatSeconds) {
        this.fileStorageService = fileStorageService;
        this.parser = parser;
        this.symbolExtractor = symbolExtractor;
        this.paperRepository = paperRepository;
        this.jobRepository = jobRepository;
        this.fingerprintUtils = fingerprintUtils;
        this.eventPublisher = eventPublisher;
        this.structureNormalizer = structureNormalizer;
        this.heartbeatScheduler = heartbeatScheduler;
        this.leaseDuration = Duration.ofMinutes(Math.max(10, leaseMinutes));
        long maxHeartbeatSeconds = Math.max(5, this.leaseDuration.toSeconds() / 3);
        this.heartbeatInterval = Duration.ofSeconds(
                Math.max(5, Math.min(heartbeatSeconds, maxHeartbeatSeconds)));
    }

    public void process(String jobId, String workerToken) {
        if (!jobRepository.renewLease(jobId, workerToken, leaseUntil())) {
            log.info("Paper ingestion job [{}] dispatch reservation is no longer owned by this worker", jobId);
            return;
        }

        ScheduledFuture<?> heartbeat;
        try {
            heartbeat = startHeartbeat(jobId, workerToken);
        } catch (RuntimeException schedulingError) {
            PaperIngestJob job = jobRepository.findById(jobId).orElse(null);
            PaperIngestStage stage = job == null || job.getCurrentStage() == null
                    ? PaperIngestStage.MONKEY_OCR
                    : job.getCurrentStage();
            int attempt = job == null || job.getAttemptCount() == null ? 1 : job.getAttemptCount();
            fail(jobId, stage, attempt,
                    "INGESTION_HEARTBEAT_UNAVAILABLE", rootMessage(schedulingError));
            log.error("Unable to start paper ingestion job [{}] lease heartbeat", jobId, schedulingError);
            return;
        }
        try {
            PaperIngestJob job = requireJob(jobId);
            int attempt = job.getAttemptCount() == null ? 1 : job.getAttemptCount();
            PaperIngestStage startStage = job.getCurrentStage() == null
                    ? PaperIngestStage.MONKEY_OCR
                    : job.getCurrentStage();

            CoreResult core = executeCoreStages(job, startStage, attempt);
            PaperIngestStage postStart = startStage.isPostProcessingStage()
                    ? startStage
                    : PaperIngestStage.SYMBOL_ENRICHMENT;

            paperRepository.resetDerivedDataFrom(core.paperId(), postStart);
            if (!startStage.isPostProcessingStage()) {
                jobRepository.markParsed(jobId);
                paperRepository.updateStatus(core.paperId(), PaperIngestStatus.PARSED.name());
            }

            eventPublisher.publishEvent(new PaperIngestedEvent(
                    jobId,
                    core.paperId(),
                    core.title(),
                    postStart,
                    attempt
            ));
        } catch (PaperIngestStageException e) {
            PaperIngestJob failedJob = jobRepository.findById(jobId).orElse(null);
            if (failedJob != null && failedJob.getStatus() != PaperIngestStatus.FAILED) {
                int attempt = failedJob.getAttemptCount() == null ? 1 : failedJob.getAttemptCount();
                fail(jobId, e.getStage(), attempt, e.getErrorCode(), e.getMessage());
            }
            log.error("Paper ingestion job [{}] failed at {}: {}", jobId, e.getStage(), e.getMessage(), e);
        } catch (Exception e) {
            PaperIngestJob job = jobRepository.findById(jobId).orElse(null);
            PaperIngestStage stage = job == null || job.getCurrentStage() == null
                    ? PaperIngestStage.MONKEY_OCR
                    : job.getCurrentStage();
            int attempt = job == null || job.getAttemptCount() == null ? 1 : job.getAttemptCount();
            jobRepository.failStage(jobId, stage, attempt, "INGESTION_UNEXPECTED", rootMessage(e));
            if (job != null && job.getPaperId() != null) {
                paperRepository.updateStatusWithError(job.getPaperId(), PaperIngestStatus.FAILED.name(), rootMessage(e));
            }
            log.error("Unexpected paper ingestion error for job [{}]", jobId, e);
        } finally {
            heartbeat.cancel(false);
            jobRepository.release(jobId, workerToken);
        }
    }

    private ScheduledFuture<?> startHeartbeat(String jobId, String workerToken) {
        try {
            ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
                    () -> renewLease(jobId, workerToken),
                    heartbeatInterval.toSeconds(),
                    heartbeatInterval.toSeconds(),
                    TimeUnit.SECONDS);
            if (heartbeat != null) {
                return heartbeat;
            }
        } catch (RuntimeException schedulingError) {
            jobRepository.release(jobId, workerToken);
            throw schedulingError;
        }
        jobRepository.release(jobId, workerToken);
        throw new IllegalStateException("Unable to schedule ingestion lease heartbeat");
    }

    private void renewLease(String jobId, String workerToken) {
        try {
            if (!jobRepository.renewLease(jobId, workerToken, leaseUntil())) {
                log.warn("Paper ingestion job [{}] lease heartbeat was rejected", jobId);
            }
        } catch (RuntimeException error) {
            log.error("Unable to renew paper ingestion job [{}] lease", jobId, error);
        }
    }

    private CoreResult executeCoreStages(PaperIngestJob initialJob, PaperIngestStage startStage, int attempt) {
        String jobId = initialJob.getId();
        if (startStage.isPostProcessingStage()) {
            if (initialJob.getPaperId() == null) {
                throw new PaperIngestStageException(startStage, "PAPER_ID_MISSING",
                        "Post-processing cannot run before a paper has been persisted");
            }
            return new CoreResult(initialJob.getPaperId(), initialJob.getExtractedTitle());
        }
        String rawPath = initialJob.getRawMarkdownPath();
        String normalizedPath = initialJob.getNormalizedMarkdownPath();

        if (shouldRun(startStage, PaperIngestStage.MONKEY_OCR) || isBlank(rawPath)) {
            rawPath = executeStage(jobId, PaperIngestStage.MONKEY_OCR, attempt, () -> {
                File source = fileStorageService.resolveFile(initialJob.getSourceFilePath());
                if (!source.isFile()) {
                    throw new PaperIngestStageException(PaperIngestStage.MONKEY_OCR,
                            "SOURCE_FILE_MISSING", "Stored PDF does not exist: " + source);
                }
                PdfParseResult parseResult = parser.parsePdf(source.getAbsolutePath());
                String markdown = parseResult.markdown();
                if (isBlank(markdown)) {
                    throw new PaperIngestStageException(PaperIngestStage.MONKEY_OCR,
                            "OCR_EMPTY_RESULT", "PDF parser returned empty Markdown");
                }
                String path = fileStorageService.writeTextArtifact(jobId, "raw-monkeyocr.md", markdown);
                jobRepository.updateParserArtifacts(jobId, path, parseResult.parserArtifactPath());
                return new StageValue<>(path,
                        parseResult.parserArtifactPath() == null ? path : parseResult.parserArtifactPath());
            });
        }

        if (shouldRun(startStage, PaperIngestStage.STRUCTURE_NORMALIZATION) || isBlank(normalizedPath)) {
            String finalRawPath = rawPath;
            normalizedPath = executeStage(jobId, PaperIngestStage.STRUCTURE_NORMALIZATION, attempt, () -> {
                String rawMarkdown = fileStorageService.readTextArtifact(finalRawPath);
                PaperStructureNormalizationResult result = structureNormalizer.normalizeWithReport(rawMarkdown);
                if (isBlank(result.normalizedMarkdown())) {
                    throw new PaperIngestStageException(PaperIngestStage.STRUCTURE_NORMALIZATION,
                            "NORMALIZATION_EMPTY_RESULT", "Outline normalization returned empty Markdown");
                }
                String markdownPath = fileStorageService.writeTextArtifact(
                        jobId, "normalized.md", result.normalizedMarkdown());
                String reportPath = fileStorageService.writeTextArtifact(
                        jobId, "normalization-report.txt", formatNormalizationReport(result));
                jobRepository.updateNormalizedArtifacts(jobId, markdownPath, reportPath);
                return new StageValue<>(markdownPath, reportPath);
            });
        }

        PaperIngestJob current = requireJob(jobId);
        if (shouldRun(startStage, PaperIngestStage.METADATA_EXTRACTION)
                || isBlank(current.getPaperFingerprint())) {
            String finalNormalizedPath = normalizedPath;
            executeStage(jobId, PaperIngestStage.METADATA_EXTRACTION, attempt, () -> {
                String markdown = fileStorageService.readTextArtifact(finalNormalizedPath);
                List<SectionPO> parsedSections = parser.parse(markdown);
                if (parsedSections == null || parsedSections.isEmpty()) {
                    throw new PaperIngestStageException(PaperIngestStage.METADATA_EXTRACTION,
                            "NO_SECTIONS", "No sections could be extracted from normalized Markdown");
                }
                String title = parser.extractTitle(markdown);
                String abstractText = parser.extractAbstract(parsedSections);
                RefMetadata metadata = symbolExtractor.extractRefMetadata(
                        buildMetadataContext(parsedSections, title));
                validateMetadata(metadata);
                String fingerprint = fingerprintUtils.generateRefFingerprint(
                        metadata.getAuthorSurnames(), metadata.getYear());
                String metadataJson = metadataJson(title, abstractText, fingerprint, metadata);
                String metadataPath = fileStorageService.writeTextArtifact(
                        jobId, "metadata.json", metadataJson);
                jobRepository.updateMetadata(jobId, metadataPath, title, abstractText,
                        fingerprint, metadata.getYear());
                return new StageValue<>(Boolean.TRUE, metadataPath);
            });
            current = requireJob(jobId);
        }

        if (isBlank(current.getExtractedTitle()) || isBlank(current.getPaperFingerprint())) {
            throw new PaperIngestStageException(PaperIngestStage.METADATA_EXTRACTION,
                    "METADATA_ARTIFACT_MISSING", "Stored metadata is incomplete");
        }

        PaperIngestJob metadataJob = current;
        String finalNormalizedPath = normalizedPath;
        Long paperId = current.getPaperId();
        if (shouldRun(startStage, PaperIngestStage.PAPER_PERSISTENCE) || paperId == null) {
            paperId = executeStage(jobId, PaperIngestStage.PAPER_PERSISTENCE, attempt, () -> {
                List<SectionPO> finalSections = parseSections(finalNormalizedPath);
                Long persistedId = metadataJob.getPaperId();
                if (persistedId == null) {
                    persistedId = paperRepository.saveFullPaper(
                            metadataJob.getExtractedTitle(), finalSections,
                            metadataJob.getPaperFingerprint(), metadataJob.getExtractedAbstract(),
                            metadataJob.getPublicationYear());
                } else {
                    paperRepository.replaceFullPaper(
                            persistedId, metadataJob.getExtractedTitle(), finalSections,
                            metadataJob.getPaperFingerprint(), metadataJob.getExtractedAbstract(),
                            metadataJob.getPublicationYear());
                }
                jobRepository.attachPaper(jobId, persistedId);
                return new StageValue<>(persistedId, null);
            });
        }

        if (paperId == null) {
            throw new PaperIngestStageException(PaperIngestStage.PAPER_PERSISTENCE,
                    "PAPER_ID_MISSING", "Paper persistence did not produce a paper id");
        }
        return new CoreResult(paperId, current.getExtractedTitle());
    }

    private List<SectionPO> parseSections(String normalizedPath) {
        try {
            List<SectionPO> sections = parser.parse(fileStorageService.readTextArtifact(normalizedPath));
            if (sections == null || sections.isEmpty()) {
                throw new PaperIngestStageException(PaperIngestStage.PAPER_PERSISTENCE,
                        "NO_SECTIONS", "Normalized Markdown contains no persistable sections");
            }
            return sections;
        } catch (PaperIngestStageException e) {
            throw e;
        } catch (Exception e) {
            throw new PaperIngestStageException(PaperIngestStage.PAPER_PERSISTENCE,
                    "NORMALIZED_ARTIFACT_READ_FAILED", rootMessage(e), e);
        }
    }

    private <T> T executeStage(String jobId, PaperIngestStage stage, int attempt,
                               StageOperation<T> operation) {
        jobRepository.startStage(jobId, stage, stage.activeStatus(), attempt, leaseUntil());
        try {
            StageValue<T> result = operation.execute();
            jobRepository.completeStage(jobId, stage, attempt, result.artifactPath());
            return result.value();
        } catch (PaperIngestStageException e) {
            fail(jobId, stage, attempt, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            String errorCode = stage.name() + "_FAILED";
            fail(jobId, stage, attempt, errorCode, rootMessage(e));
            throw new PaperIngestStageException(stage, errorCode, rootMessage(e), e);
        }
    }

    private void fail(String jobId, PaperIngestStage stage, int attempt,
                      String errorCode, String message) {
        jobRepository.failStage(jobId, stage, attempt, errorCode, message);
        jobRepository.findById(jobId).map(PaperIngestJob::getPaperId).ifPresent(
                paperId -> paperRepository.updateStatusWithError(
                        paperId, PaperIngestStatus.FAILED.name(), message));
    }

    private PaperIngestJob requireJob(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Paper ingestion job not found: " + jobId));
    }

    private boolean shouldRun(PaperIngestStage start, PaperIngestStage stage) {
        return start.isBeforeOrEqual(stage);
    }

    private LocalDateTime leaseUntil() {
        return LocalDateTime.now().plus(leaseDuration);
    }

    private String buildMetadataContext(List<SectionPO> sections, String title) {
        StringBuilder context = new StringBuilder();
        int limit = Math.min(sections.size(), 3);
        for (int i = 0; i < limit; i++) {
            SectionPO section = sections.get(i);
            if (section.getHeader() != null) {
                context.append(section.getHeader()).append('\n');
            }
            if (section.getContent() != null) {
                context.append(section.getContent()).append('\n');
            }
            if (title != null && title.equals(section.getHeader()) && i + 1 < sections.size()) {
                context.append(sections.get(i + 1).getContent()).append('\n');
            }
        }
        return context.toString();
    }

    private void validateMetadata(RefMetadata metadata) {
        if (metadata == null || metadata.getAuthorSurnames() == null
                || metadata.getAuthorSurnames().isEmpty() || metadata.getYear() == 0) {
            throw new PaperIngestStageException(PaperIngestStage.METADATA_EXTRACTION,
                    "METADATA_INCOMPLETE", "Metadata must contain at least one author surname and a publication year");
        }
    }

    private String metadataJson(String title, String abstractText, String fingerprint, RefMetadata metadata) {
        return "{\n"
                + "  \"title\": \"" + jsonEscape(title) + "\",\n"
                + "  \"abstract\": \"" + jsonEscape(abstractText) + "\",\n"
                + "  \"fingerprint\": \"" + jsonEscape(fingerprint) + "\",\n"
                + "  \"year\": " + metadata.getYear() + ",\n"
                + "  \"authors\": \"" + jsonEscape(String.join(",", metadata.getAuthorSurnames())) + "\"\n"
                + "}\n";
    }

    private String formatNormalizationReport(PaperStructureNormalizationResult result) {
        StringBuilder report = new StringBuilder();
        report.append("normalizerVersion=").append(structureNormalizer.ruleVersion()).append('\n');
        report.append("outlineConfidence=").append(result.outlineConfidence()).append('\n');
        for (String warning : result.warnings()) {
            report.append("WARNING: ").append(warning).append('\n');
        }
        for (HeadingDecision decision : result.decisions()) {
            report.append(decision.candidate().lineNumber()).append('\t')
                    .append(decision.kind()).append('\t')
                    .append(decision.action()).append('\t')
                    .append(decision.targetLevel()).append('\t')
                    .append(decision.confidence()).append('\t')
                    .append(String.join(" | ", decision.reasons())).append('\t')
                    .append(decision.candidate().rawLine()).append('\n');
        }
        return report.toString();
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface StageOperation<T> {
        StageValue<T> execute() throws Exception;
    }

    private record StageValue<T>(T value, String artifactPath) {
    }

    private record CoreResult(Long paperId, String title) {
    }
}
