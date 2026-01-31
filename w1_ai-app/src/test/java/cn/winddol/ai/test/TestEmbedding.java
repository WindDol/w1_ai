package cn.winddol.ai.test;

import cn.winddol.ai.infrastructure.embedding.EmbeddingProcessor;

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



    @Test
    public void test_embedding(){
        embeddingProcessor.embedSymbols();
    }


    @Test
    public void test_embedding1(){
        embeddingProcessor.embedSections();
    }
}
