package cn.winddol.ai.paper.domain.ingest;

import java.util.EnumSet;
import java.util.Set;

public enum PaperIngestStage {
    FILE_VALIDATION,
    DEDUPLICATION,
    MONKEY_OCR,
    STRUCTURE_NORMALIZATION,
    METADATA_EXTRACTION,
    PAPER_PERSISTENCE,
    SYMBOL_ENRICHMENT,
    CITATION_ENRICHMENT,
    EMBEDDING,
    LIBRARIAN_AUDIT;

    private static final Set<PaperIngestStage> CORE_STAGES = EnumSet.range(
            MONKEY_OCR,
            PAPER_PERSISTENCE
    );

    public boolean isCoreStage() {
        return CORE_STAGES.contains(this);
    }

    public boolean isPostProcessingStage() {
        return ordinal() >= SYMBOL_ENRICHMENT.ordinal();
    }

    public boolean isBeforeOrEqual(PaperIngestStage other) {
        return ordinal() <= other.ordinal();
    }

    public PaperIngestStatus activeStatus() {
        if (isPostProcessingStage()) {
            return PaperIngestStatus.AUDITING;
        }
        return PaperIngestStatus.PARSING;
    }
}
