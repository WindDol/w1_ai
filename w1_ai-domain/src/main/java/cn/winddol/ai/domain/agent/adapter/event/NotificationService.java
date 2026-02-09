package cn.winddol.ai.domain.agent.adapter.event;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {
    void send(String sessionId, Object payload);
    void complete(String sessionId);
    void error(String sessionId, Throwable t);

    void register(String sessionId, SseEmitter emitter);
}
