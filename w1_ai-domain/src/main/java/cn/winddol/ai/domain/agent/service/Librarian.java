package cn.winddol.ai.domain.agent.service;

import cn.winddol.ai.domain.agent.adapter.ai.IAiAdapter;
import cn.winddol.ai.domain.paperTools.adapter.parser.IPaperParser;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.event.PaperIngestedEvent;
import cn.winddol.ai.domain.paperTools.model.entity.PaperEntity;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class Librarian {

    @Resource
    private ApplicationEventPublisher eventPublisher;

    public void initiateAudit(Long paperId, String title) {
        System.out.println("🦉 Domain: Librarian 决定对 Paper [" + paperId + "] 进行审查");
        eventPublisher.publishEvent(new PaperIngestedEvent(this, paperId, title));
    }
}