package cn.winddol.ai.api;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface IResearchController {
    SseEmitter askStream(String sessionId,
                         String question,
                         List<Long> paperIds,
                         String sectionId,
                         String headingPath,
                         String selectedText);
}
