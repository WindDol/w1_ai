package cn.winddol.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperIngestJobDTO {
    private String jobId;
    private Long paperId;
    private String originalFilename;
    private String fileSha256;
    private Long fileSize;
    private String status;
    private String currentStage;
    private String failedStage;
    private String errorCode;
    private String errorMessage;
    private Integer attemptCount;
    private String parserType;
    private String parserVersion;
    private String normalizerVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
