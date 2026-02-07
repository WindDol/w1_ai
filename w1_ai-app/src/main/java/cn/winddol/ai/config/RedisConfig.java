package cn.winddol.ai.config;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {
    @Value("${redis.sdk.config.host}")
    private String redisHost;
    @Value("${redis.sdk.config.port}")
    private int redisPort;
    @Value("${redis.sdk.config.database}")
    private int redisDatabase;
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(new HostAndPort(redisHost, redisPort), DefaultJedisClientConfig.builder().database(redisDatabase).build());
    }
    @Bean
    public ChatMemoryStore chatMemoryStore(JedisPooled jedisPooled){
        return  new RedisChatMemoryStore(jedisPooled);
    }

}
