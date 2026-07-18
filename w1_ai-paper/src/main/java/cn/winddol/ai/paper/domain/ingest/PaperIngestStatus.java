package cn.winddol.ai.paper.domain.ingest;

public enum PaperIngestStatus {
    UPLOADED,
    PARSING,
    PARSED,
    AUDITING,
    READY,
    FAILED;

    public boolean isTerminal() {
        return this == READY || this == FAILED;
    }
}
