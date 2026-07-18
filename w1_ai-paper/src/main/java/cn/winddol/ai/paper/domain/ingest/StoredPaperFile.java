package cn.winddol.ai.paper.domain.ingest;

public record StoredPaperFile(
        String path,
        String sha256,
        long size
) {
}
