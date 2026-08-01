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
    @Value("${ai.embedding.model:text-embedding-v4}")
    private String embeddingModelName;
    @Value("${ai.embedding.dimensions:1536}")
    private int embeddingDimensions;
    @Value("${ai.llm.api-key}")
    private String limApiKey;

    @Value("${ai.llm.base-url:https://api.deepseek.com}")
    private String llmBaseUrl;

    @Value("${ai.llm.model:deepseek-v4-flash}")
    private String llmModelName;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .baseUrl(embeddingBaseUrl)
                .modelName(embeddingModelName)
                .dimensions(embeddingDimensions)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel(){
        return  OpenAiChatModel.builder()
                .apiKey(limApiKey)
                .baseUrl(llmBaseUrl)
                .modelName(llmModelName)
                .temperature(0.0)                   
                .timeout(java.time.Duration.ofSeconds(500))
                .maxRetries(1)
                .logRequests(true)                   
                .logResponses(true)
                .build();
    }


}
