package cn.winddol.ai.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.store.memory.chat.redis.RedisChatMemoryStoreException;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

public class RedisChatMemoryStore implements ChatMemoryStore {
    private final JedisPooled client;

    public RedisChatMemoryStore(String host, Integer port, String user, String password,Integer database) {
        String finalHost = ValidationUtils.ensureNotBlank(host, "host");
        int finalPort = (Integer)ValidationUtils.ensureNotNull(port, "port");
        int finalDatabase = (Integer)ValidationUtils.ensureNotNull(database, "database");
        if (user != null) {
            String finalUser = ValidationUtils.ensureNotBlank(user, "user");
            String finalPassword = ValidationUtils.ensureNotBlank(password, "password");
            this.client = new JedisPooled(new HostAndPort(finalHost, finalPort),
                    (JedisClientConfig) DefaultJedisClientConfig.builder().user(finalUser).password(finalPassword).database(finalDatabase).build());
        } else {
            this.client = new JedisPooled(new HostAndPort(finalHost, finalPort),
                    (JedisClientConfig) DefaultJedisClientConfig.builder().database(finalDatabase).build());
        }

    }

    public List<ChatMessage> getMessages(Object memoryId) {
        String json = this.client.get(toMemoryIdString(memoryId));
        return (List<ChatMessage>)(json == null ? new ArrayList() : ChatMessageDeserializer.messagesFromJson(json));
    }

    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson((List)ValidationUtils.ensureNotEmpty(messages, "messages"));
        String res = this.client.set(toMemoryIdString(memoryId), json);
        if (!"OK".equals(res)) {
            throw new RedisChatMemoryStoreException("Set memory error, msg=" + res);
        }
    }

    public void deleteMessages(Object memoryId) {
        this.client.del(toMemoryIdString(memoryId));
    }

    private static String toMemoryIdString(Object memoryId) {
        boolean isNullOrEmpty = memoryId == null || memoryId.toString().trim().isEmpty();
        if (isNullOrEmpty) {
            throw new IllegalArgumentException("memoryId cannot be null or empty");
        } else {
            return memoryId.toString();
        }
    }

    public static RedisChatMemoryStore.Builder builder() {
        return new RedisChatMemoryStore.Builder();
    }

    public static class Builder {
        private String host;
        private Integer port;
        private String user;
        private String password;
        private Integer database;
        public RedisChatMemoryStore.Builder host(String host) {
            this.host = host;
            return this;
        }

        public RedisChatMemoryStore.Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public RedisChatMemoryStore.Builder user(String user) {
            this.user = user;
            return this;
        }

        public RedisChatMemoryStore.Builder password(String password) {
            this.password = password;
            return this;
        }
        public RedisChatMemoryStore.Builder database(int database) {
            this.database = database;
            return this;
        }

        public RedisChatMemoryStore build() {
            return new RedisChatMemoryStore(this.host, this.port, this.user, this.password,this.database);
        }
    }
}

