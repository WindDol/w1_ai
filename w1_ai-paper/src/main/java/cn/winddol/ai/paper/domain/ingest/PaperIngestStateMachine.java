package cn.winddol.ai.paper.domain.ingest;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PaperIngestStateMachine {

    private static final Map<PaperIngestStatus, Set<PaperIngestStatus>> TRANSITIONS =
            new EnumMap<>(PaperIngestStatus.class);

    static {
        TRANSITIONS.put(PaperIngestStatus.UPLOADED,
                EnumSet.of(PaperIngestStatus.PARSING, PaperIngestStatus.FAILED));
        TRANSITIONS.put(PaperIngestStatus.PARSING,
                EnumSet.of(PaperIngestStatus.PARSED, PaperIngestStatus.FAILED));
        TRANSITIONS.put(PaperIngestStatus.PARSED,
                EnumSet.of(PaperIngestStatus.AUDITING, PaperIngestStatus.FAILED));
        TRANSITIONS.put(PaperIngestStatus.AUDITING,
                EnumSet.of(PaperIngestStatus.READY, PaperIngestStatus.FAILED));
        TRANSITIONS.put(PaperIngestStatus.READY,
                EnumSet.of(PaperIngestStatus.UPLOADED, PaperIngestStatus.PARSED));
        TRANSITIONS.put(PaperIngestStatus.FAILED,
                EnumSet.of(PaperIngestStatus.UPLOADED, PaperIngestStatus.PARSED));
    }

    private PaperIngestStateMachine() {
    }

    public static boolean canTransition(PaperIngestStatus from, PaperIngestStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return from == to || TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(PaperIngestStatus from, PaperIngestStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Illegal paper ingestion transition: " + from + " -> " + to);
        }
    }
}
