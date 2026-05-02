package cn.winddol.ai.agent.research.api;

import java.time.Duration;

public interface ISessionLockService {
    boolean tryLock(String sessionId, Duration timeout);
    void unlock(String sessionId);
}
