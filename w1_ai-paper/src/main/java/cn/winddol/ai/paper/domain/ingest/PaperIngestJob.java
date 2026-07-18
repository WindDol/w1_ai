package cn.winddol.ai.paper.domain.ingest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperIngestJob {
    private String id;
    private Long paperId;
    private String originalFilename;
    private String fileSha256;
    private Long fileSize;
    private String sourceFilePath;
    private PaperIngestStatus status;
    private PaperIngestStage currentStage;
    private PaperIngestStage failedStage;
    private String errorCode;
    private String errorMessage;
    private Integer attemptCount;
    private String parserType;
    private String parserVersion;
    private String normalizerVersion;
    private String parserArtifactPath;
    private String rawMarkdownPath;
    private String normalizedMarkdownPath;
    private String normalizationReportPath;
    private String metadataPath;
    private String extractedTitle;
    private String extractedAbstract;
    private String paperFingerprint;
    private Integer publicationYear;
    private String workerToken;
    private LocalDateTime leaseUntil;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public boolean canRetry() {
        return status == PaperIngestStatus.FAILED && failedStage != null;
    }
}
