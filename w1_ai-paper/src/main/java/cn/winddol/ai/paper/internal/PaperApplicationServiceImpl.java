package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.domain.*;
import cn.winddol.ai.paper.domain.PaperDetailVO;
import cn.winddol.ai.paper.domain.PaperVO;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.api.IFileStorageService;
import cn.winddol.ai.paper.api.IPaperApplication;
import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import cn.winddol.ai.paper.api.IPaperParser;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStatus;
import cn.winddol.ai.paper.domain.ingest.StoredPaperFile;
import cn.winddol.ai.paper.internal.structure.PaperStructureNormalizer;
import cn.winddol.ai.shared.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaperApplicationServiceImpl implements IPaperApplication {

    private final IFileStorageService fileStorageService;
    private final IPaperParser parser;
    private final IPaperRepository paperRepository;
    private final IPaperIngestJobRepository jobRepository;
    private final PaperIngestionDispatcher dispatcher;
    private final PaperStructureNormalizer structureNormalizer;
    private final int recoveryBatchSize;
    private final long staleMinutes;

    public PaperApplicationServiceImpl(IFileStorageService fileStorageService,
                                       IPaperParser parser,
                                       IPaperRepository paperRepository,
                                       IPaperIngestJobRepository jobRepository,
                                       PaperIngestionDispatcher dispatcher,
                                       PaperStructureNormalizer structureNormalizer,
                                       @Value("${paper.ingestion.recovery-batch-size:10}") int recoveryBatchSize,
                                       @Value("${paper.ingestion.stale-minutes:130}") long staleMinutes) {
        this.fileStorageService = fileStorageService;
        this.parser = parser;
        this.paperRepository = paperRepository;
        this.jobRepository = jobRepository;
        this.dispatcher = dispatcher;
        this.structureNormalizer = structureNormalizer;
        this.recoveryBatchSize = Math.max(1, recoveryBatchSize);
        this.staleMinutes = Math.max(10, staleMinutes);
    }

    @Override
    public PaperIngestJob submit(MultipartFile file) throws IOException {
        validateUpload(file);
        String jobId = UUID.randomUUID().toString();
        StoredPaperFile stored = fileStorageService.storeUploadedFile(jobId, file);
        LocalDateTime now = LocalDateTime.now();
        PaperIngestJob candidate = PaperIngestJob.builder()
                .id(jobId)
                .originalFilename(file.getOriginalFilename())
                .fileSha256(stored.sha256())
                .fileSize(stored.size())
                .sourceFilePath(stored.path())
                .status(PaperIngestStatus.UPLOADED)
                .currentStage(PaperIngestStage.FILE_VALIDATION)
                .attemptCount(1)
                .parserType(parser.parserType())
                .parserVersion(parser.parserVersion())
                .normalizerVersion(structureNormalizer.ruleVersion())
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaperIngestJob persisted = jobRepository.createOrGetReusable(candidate);
        if (!jobId.equals(persisted.getId())) {
            fileStorageService.deleteJobFiles(jobId);
            log.info("Duplicate PDF reused ingestion job [{}] for SHA-256 [{}]",
                    persisted.getId(), stored.sha256());
            return persisted;
        }

        auditSubmissionStages(jobId, stored.path());
        dispatchOrFail(jobId, 1);
        return getIngestJob(jobId);
    }

    @Override
    public PaperIngestJob getIngestJob(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException("INGEST_JOB_NOT_FOUND", "Ingestion job not found: " + jobId));
    }

    @Override
    public PaperIngestJob retry(String jobId) {
        PaperIngestJob job = getIngestJob(jobId);
        if (!job.canRetry() || !jobRepository.prepareRetry(jobId)) {
            throw new AppException("INGEST_JOB_NOT_RETRYABLE", "Only a failed ingestion job can be retried");
        }
        int attempt = job.getAttemptCount() == null ? 2 : job.getAttemptCount() + 1;
        dispatchOrFail(jobId, attempt);
        return getIngestJob(jobId);
    }

    @Override
    public PaperIngestJob rerunFrom(String jobId, PaperIngestStage stage) {
        if (stage == null || stage == PaperIngestStage.FILE_VALIDATION
                || stage == PaperIngestStage.DEDUPLICATION) {
            throw new AppException("INGEST_STAGE_INVALID",
                    "Rerun stage must be MONKEY_OCR or a later processing stage");
        }
        PaperIngestJob job = getIngestJob(jobId);
        if (!jobRepository.prepareRerun(jobId, stage)) {
            throw new AppException("INGEST_JOB_NOT_RERUNNABLE",
                    "Only READY or FAILED jobs can be rerun");
        }
        jobRepository.updateProcessingVersions(jobId, parser.parserType(),
                parser.parserVersion(), structureNormalizer.ruleVersion());
        int attempt = job.getAttemptCount() == null ? 2 : job.getAttemptCount() + 1;
        dispatchOrFail(jobId, attempt);
        return getIngestJob(jobId);
    }

    @Override
    public int recoverInterruptedJobs() {
        List<PaperIngestJob> jobs = jobRepository.findRecoverable(
                LocalDateTime.now().minusMinutes(staleMinutes), recoveryBatchSize);
        jobs.forEach(job -> dispatchOrFail(
                job.getId(), job.getAttemptCount() == null ? 1 : job.getAttemptCount()));
        return jobs.size();
    }

    @Override
    public List<PaperVO> listAllPapers() {
        return paperRepository.listAllPapers();
    }

    @Override
    public PaperDetailVO getPaperDetails(Long paperId) {
        PaperEntity paper = paperRepository.getPaperDetailsById(paperId);
        List<SymbolEntity> symbols = paperRepository.findByPaperId(paperId);
        List<KnowledgeRelationEntity> relations = paperRepository.findRelationsByPaperId(paperId);
        List<ReferenceItem> referenceItemList = paperRepository.selectReferencesByPaperId(paperId);

        StringBuilder novelty = new StringBuilder();

        if (!relations.isEmpty()) {
            novelty.append("**Inter-paper relations from the Librarian's audit:**\n\n");

            for (KnowledgeRelationEntity rel : relations) {
                String type = rel.getType();
                String otherPaperInfo = String.format("Paper [%d] (%s)", rel.getRelatedId(), rel.getRelatedTitle());

                if ("OUTGOING".equals(rel.getDirection())) {
                    String actionPhrase = getActivePhrasing(type);
                    novelty.append(String.format("- This paper **%s** %s.\n  *Reason: %s*\n",
                            actionPhrase, otherPaperInfo, rel.getDescription()));
                } else {
                    String passivePhrase = getPassivePhrasing(type);
                    novelty.append(String.format("- This paper **%s** %s.\n  *Note: %s*\n",
                            passivePhrase, otherPaperInfo, rel.getDescription()));
                }
            }
        } else {
            novelty.append("The Librarian found no direct or specific relations with other papers in the library yet.");
        }

        PaperDetailVO vo = PaperDetailVO.builder()
                .id(paper.getId())
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .noveltyAssessment(novelty.toString())
                .symbolCount(symbols.size())
                .referenceCount(referenceItemList.size())
                .keySymbols(symbols.stream().limit(5).collect(Collectors.toList()))
                .build();

        return vo;
    }

    private String getActivePhrasing(String type) {
        if (type == null) return "relates to";
        return switch (type.toUpperCase()) {
            case "FOUNDATIONAL" -> "serves as a FOUNDATIONAL BASIS for";
            case "EXTENDS"      -> "EXTENDS the work of";
            case "CONTRADICTS"  -> "CONTRADICTS or REFUTES";
            case "SUPPORT"      -> "SUPPORTS the findings of";
            case "ALTERNATIVE"  -> "presents an ALTERNATIVE approach to";
            default             -> "has a relation (" + type + ") with";
        };
    }

    private String getPassivePhrasing(String type) {
        if (type == null) return "is related to";
        return switch (type.toUpperCase()) {
            case "FOUNDATIONAL" -> "is BUILT UPON the foundation of";
            case "EXTENDS"      -> "is EXTENDED by";
            case "CONTRADICTS"  -> "is CONTRADICTED by";
            case "SUPPORT"      -> "is SUPPORTED by";
            case "ALTERNATIVE"  -> "is considered an ALTERNATIVE to";
            default             -> "is referenced (" + type + ") by";
        };
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("UPLOAD_EMPTY", "PDF file must not be empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new AppException("UPLOAD_NOT_PDF", "Only PDF files are supported");
        }
    }

    private void auditSubmissionStages(String jobId, String sourcePath) {
        jobRepository.startStage(jobId, PaperIngestStage.FILE_VALIDATION,
                PaperIngestStatus.UPLOADED, 1, null);
        jobRepository.completeStage(jobId, PaperIngestStage.FILE_VALIDATION, 1, sourcePath);
        jobRepository.startStage(jobId, PaperIngestStage.DEDUPLICATION,
                PaperIngestStatus.UPLOADED, 1, null);
        jobRepository.completeStage(jobId, PaperIngestStage.DEDUPLICATION, 1, null);
    }

    private void dispatchOrFail(String jobId, int attempt) {
        try {
            dispatcher.dispatch(jobId);
        } catch (RuntimeException rejected) {
            jobRepository.failStage(jobId, PaperIngestStage.MONKEY_OCR, attempt,
                    "INGESTION_DISPATCH_REJECTED", rejected.getMessage());
            log.error("Unable to dispatch paper ingestion job [{}]", jobId, rejected);
        }
    }
}
