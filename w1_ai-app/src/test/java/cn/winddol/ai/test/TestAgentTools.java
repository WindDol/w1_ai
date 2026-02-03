package cn.winddol.ai.test;


import cn.winddol.ai.domain.paper.adapter.tools.ScientificResearchTools;
import cn.winddol.ai.domain.paper.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paper.service.AgentReaderService;
import cn.winddol.ai.domain.paper.service.HybridRetrieverService;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.internal.Json;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class TestAgentTools {

    @Resource
    private HybridRetrieverService retriever;
    @Resource
    private AgentReaderService reader;
    @Resource
    private ScientificResearchTools scientificResearchTools;

    @Test
    public void test1_SearchLibrary() {
        // 测试雷达
        System.out.println("--- Test Search: 'Möbius transformation' ---");
        SearchResultDTO result = retriever.searchLibrary("Möbius transformation");

        // 断言：应该能找到 Header 包含 Möbius 的章节
       log.info(JSON.toJSONString(result));
    }

    @Test
    public void test2_SmartRead() {
        // 测试阅读器 (你需要先去数据库里找一个存在的 sectionId)
        String sectionId = "c704a54e-06c8-422e-a5a4-718e3d24e881";

        System.out.println("--- Test Read: " + sectionId + " ---");
        String content = reader.readSectionWithContext(sectionId);

        log.info(content);
    }
    @Test
    public void test3_ScientificResearch() {
        System.out.println("--- Test Search: 'Möbius transformation' ---");
        String content = scientificResearchTools.searchLibrary("Möbius transformation",null);
        log.info(content);
    }
}
