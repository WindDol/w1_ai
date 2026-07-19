package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.domain.retrieval.RetrievalCandidate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReciprocalRankFusion {

    // RRF 平滑常数，避免单一通道中的头部结果完全压制其他召回通道。
    private static final double RANK_CONSTANT = 60.0;

    /**
     * 按 evidenceKey 合并多通道候选，并使用加权 RRF 计算最终排序。
     */
    public List<RetrievalCandidate> fuse(List<RankedCandidates> rankedLists, int limit) {
        Map<String, RetrievalCandidate> merged = new LinkedHashMap<>();
        for (RankedCandidates ranked : rankedLists) {
            if (ranked.candidates() == null) {
                continue;
            }
            for (int index = 0; index < ranked.candidates().size(); index++) {
                RetrievalCandidate source = ranked.candidates().get(index);
                if (source.getEvidenceKey() == null || source.getEvidenceKey().isBlank()) {
                    continue;
                }
                RetrievalCandidate target = merged.computeIfAbsent(
                        source.getEvidenceKey(), ignored -> copy(source));
                // 同一证据可能被多个通道召回，需要保留来源并累计各通道的排名贡献。
                target.getMatchedChannels().add(ranked.channel());
                target.setSemanticScore(max(target.getSemanticScore(), source.getSemanticScore()));
                target.setLexicalScore(max(target.getLexicalScore(), source.getLexicalScore()));
                double contribution = ranked.weight() / (RANK_CONSTANT + index + 1);
                target.setFusionScore(defaultScore(target.getFusionScore()) + contribution);
            }
        }

        return merged.values().stream()
                .sorted(Comparator
                        .comparingDouble((RetrievalCandidate item) -> defaultScore(item.getFusionScore())).reversed()
                        .thenComparing(Comparator.comparingDouble(
                                (RetrievalCandidate item) -> max(item.getSemanticScore(), item.getLexicalScore()))
                                .reversed()))
                .limit(Math.max(1, limit))
                .toList();
    }

    /**
     * 复制召回候选，避免融合过程修改 Repository 返回的原始对象。
     */
    private RetrievalCandidate copy(RetrievalCandidate source) {
        return RetrievalCandidate.builder()
                .evidenceKey(source.getEvidenceKey())
                .evidenceType(source.getEvidenceType())
                .paperId(source.getPaperId())
                .paperTitle(source.getPaperTitle())
                .sectionId(source.getSectionId())
                .chunkId(source.getChunkId())
                .heading(source.getHeading())
                .headingPath(source.getHeadingPath())
                .content(source.getContent())
                .pageStart(source.getPageStart())
                .pageEnd(source.getPageEnd())
                .referenceIndex(source.getReferenceIndex())
                .semanticScore(source.getSemanticScore())
                .lexicalScore(source.getLexicalScore())
                .fusionScore(0.0)
                .build();
    }

    /**
     * 合并同一证据在不同通道中的最佳原始分数。
     */
    private double max(Double left, Double right) {
        return Math.max(defaultScore(left), defaultScore(right));
    }

    /**
     * 将缺失分数归一为零，简化融合与排序计算。
     */
    private double defaultScore(Double value) {
        return value == null ? 0.0 : value;
    }
}
