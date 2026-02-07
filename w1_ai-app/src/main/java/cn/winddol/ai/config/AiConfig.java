package cn.winddol.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Value("${ai.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${ai.embedding.base-url}")
    private String embeddingBaseUrl;
    @Value("${ai.llm.api-key}")
    private String limApiKey;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .baseUrl(embeddingBaseUrl)
                .modelName("text-embedding-v4")
                .dimensions(1536)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel(){
        return  OpenAiChatModel.builder()
                .apiKey(limApiKey)
                .baseUrl("https://api.deepseek.com") // 关键点！
                .modelName("deepseek-chat")          // DeepSeek V3 模型名
                .temperature(0.0)                    // 设为 0 让提取更稳定
                .timeout(java.time.Duration.ofSeconds(500))
                .maxRetries(1)
                .logRequests(true)                   // 调试时打印请求
                .logResponses(true)
                .build();
    }


}
