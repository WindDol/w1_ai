package cn.winddol.ai.trigger.http;

import cn.winddol.ai.domain.agent.event.NotificationService;
import cn.winddol.ai.trigger.application.service.ResearchOrchestrator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequestMapping("/api/v1/agent")
@RestController
public class ResearchController {
    @Resource
    private NotificationService notificationService;
    @Resource
    private ResearchOrchestrator orchestrator;

    @GetMapping(value = "/ask-stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter askStream(@RequestParam String sessionId, @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(600_000L);

        // 1. 在基础设施层注册这个连接
        notificationService.register(sessionId, emitter);

        // 2. 异步启动领域逻辑，不需要在 Controller 里写 Lambda 推送逻辑
        CompletableFuture.runAsync(() -> {
            try {
                orchestrator.startResearch(sessionId, question);
            } catch (Exception e) {
                notificationService.send(sessionId, "Error: " + e.getMessage());
            }
        });

        return emitter;
    }
}
