package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.embedding.EmbeddingProcessor;
import cn.winddol.ai.infrastructure.embedding.KnowledgeRetriever;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Repeat;

@Slf4j
@SpringBootTest
public class TestEmbedding {

    @Resource
    private EmbeddingProcessor embeddingProcessor;

    @Resource
    private KnowledgeRetriever knowledgeRetriever;

    @Test
    public void test_embedding(){
        embeddingProcessor.embedSymbols();
    }

    @Test
    public void test_embedding_query(){
        knowledgeRetriever.searchSymbols(7l,"mapping w in the unit disk to z ");
    }
    @Test
    public void test_embedding1(){
        embeddingProcessor.embedSections();
    }
}
