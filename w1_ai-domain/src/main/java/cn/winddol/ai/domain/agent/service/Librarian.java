package cn.winddol.ai.domain.agent.service;

import cn.winddol.ai.domain.paperTools.event.PaperIngestedEvent;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class Librarian {

    @Resource
    private ApplicationEventPublisher eventPublisher;


    public void initiateAudit(Long paperId, String title) {
        System.out.println("🦉 Domain: Librarian 决定对 Paper [" + paperId + "] 进行审查");
        eventPublisher.publishEvent(new PaperIngestedEvent(this, paperId, title));
    }
}