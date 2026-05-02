package cn.winddol.ai.infrastructure.adapter.paper;

import cn.winddol.ai.agent.librarian.api.ILibrarianRepository;
import cn.winddol.ai.agent.librarian.domain.AgentPaperEntity;
import cn.winddol.ai.infrastructure.dao.PaperMapper;
import cn.winddol.ai.infrastructure.dao.PaperKnowledgeRelationMapper;
import cn.winddol.ai.infrastructure.dao.po.Paper;
import cn.winddol.ai.infrastructure.dao.po.PaperKnowledgeRelation;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class LibrarianRepositoryImpl implements ILibrarianRepository {

    private final PaperMapper paperMapper;
    private final PaperKnowledgeRelationMapper relationMapper;

    public LibrarianRepositoryImpl(PaperMapper paperMapper, PaperKnowledgeRelationMapper relationMapper) {
        this.paperMapper = paperMapper;
        this.relationMapper = relationMapper;
    }

    @Override
    public AgentPaperEntity getPaperById(Long id) {
        if (id == null) return null;
        Paper paper = paperMapper.selectById(id);
        return AgentPaperEntity.builder()
                .id(id)
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .embedding(paper.getEmbedding())
                .years(paper.getYears())
                .build();
    }

    @Override
    public List<AgentPaperEntity> searchSimilarPapers(float[] vector, int limit, Long excludeId) {
        String vectorStr = Arrays.toString(vector);
        List<Map<String, Object>> results = paperMapper.searchSimilar(vectorStr, limit, excludeId);
        return results.stream().map(map -> AgentPaperEntity.builder()
                .id((Long) map.get("id"))
                .title((String) map.get("title"))
                .abstractText((String) map.get("abstracttext"))
                .years((Integer) map.get("years"))
                .build()).collect(Collectors.toList());
    }

    @Override
    public void saveRelation(Long sourceId, Long targetId, String type, String reason) {
        PaperKnowledgeRelation po = PaperKnowledgeRelation.builder()
                .sourcePaperId(sourceId)
                .targetPaperId(targetId)
                .relationType(type)
                .description(reason)
                .build();
        relationMapper.insert(po);
    }

    @Override
    public void updateStatus(Long paperId, String status) {
        Paper po = new Paper();
        po.setId(paperId);
        po.setStatus(status);
        paperMapper.updateById(po);
    }

    @Override
    public void updateStatusWithError(Long paperId, String status, String errorMessage) {
        Paper po = new Paper();
        po.setId(paperId);
        po.setStatus(status);
        po.setStatusMessage(errorMessage);
        paperMapper.updateById(po);
    }
}
