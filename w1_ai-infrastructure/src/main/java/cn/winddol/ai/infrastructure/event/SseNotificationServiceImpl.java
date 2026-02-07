package cn.winddol.ai.infrastructure.event;

import cn.winddol.ai.domain.agent.event.NotificationService;
import com.alibaba.fastjson.JSON;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationServiceImpl implements NotificationService {
    // 维护 sessionId 和 SseEmitter 的映射
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    @Override
    public void register(String sessionId, SseEmitter emitter) {
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError((e) -> emitters.remove(sessionId));
    }

    @Override
    public void send(String sessionId, Object payload) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                String jsonPayload = JSON.toJSONString(payload);
                emitter.send(SseEmitter.event()
                        .data(jsonPayload, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitters.remove(sessionId);
            }
        }
    }

    @Override
    public void complete(String sessionId) {

    }

    @Override
    public void error(String sessionId, Throwable t) {

    }
}
