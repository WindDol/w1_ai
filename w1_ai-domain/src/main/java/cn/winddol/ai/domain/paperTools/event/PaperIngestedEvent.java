package cn.winddol.ai.domain.paperTools.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaperIngestedEvent extends ApplicationEvent {
    private final Long paperId;
    private final String title;

    public PaperIngestedEvent(Object source, Long paperId, String title) {
        super(source);
        this.paperId = paperId;
        this.title = title;
    }
}