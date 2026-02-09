package cn.winddol.ai.domain.agent.adapter.redis;

import java.time.Duration;

public interface ISessionLockService {
    boolean tryLock(String sessionId, Duration timeout);
    void unlock(String sessionId);
}
