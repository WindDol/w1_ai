package cn.winddol.ai.domain.agent.service.bussiness;

import cn.winddol.ai.domain.agent.adapter.ai.IAiAdapter;
import cn.winddol.ai.domain.agent.adapter.repository.IAgentRepository;
import cn.winddol.ai.domain.agent.model.entity.PaperAuditResult;
import cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class LibrarianAuditService {
    @Resource
    private IAgentRepository agentRepository;
    @Resource
    private IAiAdapter aiAdapter;
    public void auditAgainstLibrary(Long newPaperId) {
        // 1. 获取新论文的摘要 (假设摘要在第一节或专门字段)
        AgentPaperEntity newPaper = agentRepository.getPaperById(newPaperId);
        String newAbstract = newPaper.getAbstractText();

        // 2. 语义雷达：在库中寻找“邻居”
        float[] vector = newPaper.getEmbedding();
        List<AgentPaperEntity> neighbors = agentRepository.searchSimilarPapers(vector, 3, newPaperId);

        if (neighbors.isEmpty()) {
            log.info("🔍 No similar papers found in library. This might be a new sub-field.");
            return;
        }

        // 3. 深度对比：让 LLM 评价
        for (AgentPaperEntity oldPaper : neighbors) {
            PaperAuditResult auditResult = aiAdapter.analyzeRelation(newPaper, oldPaper, newAbstract);
            agentRepository.saveRelation(newPaper.getId(), oldPaper.getId(), auditResult.getType(),auditResult.getReason());
        }
    }

}
