package cn.winddol.ai.test;

import cn.winddol.ai.domain.agent.service.ResearchAgent;
import cn.winddol.ai.domain.agent.service.ResearchOrchestrator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class TestAgent {
    @Resource
    private ResearchOrchestrator agent;

//    @Test
//    public void testComplexReasoning() {
//        // 提一个这篇论文特有的、如果不查正文绝对回答不上来的问题
//        String question = "在推导公式 21 时，Möbius 变换的参数 alpha 和 psi 满足什么微分方程？";
//
////        String answer = agent.run(question);
//
//        log.info("================ FINAL ANSWER ================");
//        log.info(answer);
//    }
//
//    @Test
//    public void testReferenceTrackingReasoning() {
//        String question = "What are the characteristics of the traveling wave status";
//
////        String answer = agentEngine.run(question);
//
//        log.info("================ FINAL ANSWER ================");
//        log.info(answer);
//    }

    @Test
    public void testSession() {

        String answer = agent.ask("session_1", "谁在 2009 年研究了 Möbius 变换在振子系统中的应用？");

        log.info("================ FINAL ANSWER ================");
        log.info(answer);
    }
    @Test
    public void testSession1() {

        String answer = agent.ask("session_3", "“根据 Seth Marvel (2009) 的论文，他的方法与 Watanabe (1994) [Ref 21] 提出的方法在处理 N 个振子时的维度缩减结果有什么具体不同？”");

        log.info("================ FINAL ANSWER ================");
        log.info(answer);
    }

}
