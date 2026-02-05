package cn.winddol.ai.config;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Value("${redis.sdk.config.host}")
    private String redisHost;
    @Value("${redis.sdk.config.port}")
    private int redisPort;
    @Value("${redis.sdk.config.database}")
    private int redisDatabase;

    @Bean
    public ChatMemoryStore chatMemoryStore(){
        return RedisChatMemoryStore.builder().host(redisHost).port(redisPort).database(redisDatabase).build();
    }

}
