package cn.winddol.ai.infrastructure.redis;



import cn.winddol.ai.domain.agent.adapter.redis.ISessionLockService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;

@Service
public class SessionLockService implements ISessionLockService,
        cn.winddol.ai.agent.research.api.ISessionLockService {

    @Resource
    private JedisPooled jedisPooled; // 自动注入 RedisConfig 中定义的那个对象

    private static final String LOCK_PREFIX = "lock:agent:";

    public boolean tryLock(String sessionId, Duration timeout) {
        String key = LOCK_PREFIX + sessionId;
        SetParams params = SetParams.setParams().nx().ex(timeout.toSeconds());

        // 使用同一个连接池执行命令
        String result = jedisPooled.set(key, "BUSY", params);
        return "OK".equals(result);
    }

    public void unlock(String sessionId) {
        // 安全解锁：使用 Lua 脚本确保只有加锁者能解锁
        String key = LOCK_PREFIX + sessionId;
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        jedisPooled.eval(script, 1, key, "BUSY");
    }
}