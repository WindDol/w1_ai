package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import lombok.Getter;

@Getter
class PaperIngestStageException extends RuntimeException {
    private final PaperIngestStage stage;
    private final String errorCode;

    PaperIngestStageException(PaperIngestStage stage, String errorCode, String message) {
        super(message);
        this.stage = stage;
        this.errorCode = errorCode;
    }

    PaperIngestStageException(PaperIngestStage stage, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
        this.errorCode = errorCode;
    }
}
