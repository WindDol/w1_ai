package cn.winddol.ai.api;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IResearchController {
    SseEmitter askStream(String sessionId, String question);
}
