package cn.winddol.ai.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("paper_ingest_jobs")
public class PaperIngestJobPO {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long paperId;
    private String originalFilename;
    private String fileSha256;
    private Long fileSize;
    private String sourceFilePath;
    private String status;
    private String currentStage;
    private String failedStage;
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
}
