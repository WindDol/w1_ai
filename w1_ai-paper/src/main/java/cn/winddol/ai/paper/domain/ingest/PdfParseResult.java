package cn.winddol.ai.paper.domain.ingest;

public record PdfParseResult(
        String markdown,
        String parserArtifactPath
) {
}
