package cn.winddol.ai.paper.domain.workspace;

/** 允许阅读工作台查看的文本产物类型。 */
public enum ArtifactType {
    RAW_MARKDOWN("MonkeyOCR Markdown"),
    NORMALIZED_MARKDOWN("Normalized Markdown"),
    NORMALIZATION_REPORT("Outline Decision Report"),
    METADATA("Extracted Metadata");

    private final String label;

    ArtifactType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
