package cn.winddol.ai.test;


import cn.winddol.ai.domain.paperTools.adapter.tools.ScientificResearchTools;
import cn.winddol.ai.domain.paperTools.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paperTools.service.AgentReaderService;
import cn.winddol.ai.domain.paperTools.service.HybridRetrieverService;
import com.alibaba.fastjson.JSON;
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

    @Test
    public void test4_ScientificResearch() {
        System.out.println("--- Test Search: 'Möbius transformation' ---");
        String content = scientificResearchTools.lookupReference(7l, String.valueOf(21));
        log.info(content);
    }
    @Test
    public void testLookupReferenceTool() {
        // 假设 Paper ID 是 7, 参考文献索引是 "24"
        String result = scientificResearchTools.lookupReference(7L, "24");

        log.info("--- Tool Output ---");
        log.info(result);

        // 断言检查
        assert result.contains("Pikovsky"); // 应该是 Pikovsky 的论文
        assert result.contains("Abstract"); // 应该包含摘要
    }
}
