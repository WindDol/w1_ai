package cn.winddol.ai.test;

import cn.winddol.ai.domain.paperTools.service.CitationEnrichmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class TestCitation {
    @Resource
    private CitationEnrichmentService service;
    @Test
    public void testCitation(){

        service.enrichReferences();
    }
}
