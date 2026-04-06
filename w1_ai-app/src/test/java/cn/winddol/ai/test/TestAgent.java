package cn.winddol.ai.test;

import cn.winddol.ai.domain.agent.service.bussiness.Librarian;
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
    @Resource
    private Librarian librarian;
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

        String answer = agent.startResearch("session_1", "谁在 2009 年研究了 Möbius 变换在振子系统中的应用？");

        log.info("================ FINAL ANSWER ================");
        log.info(answer);
    }
    @Test
    public void testSession1() {

        String answer = agent.startResearch("session_10", "论文在推导连续极限（Continuum limit，第五章）时，其核心是方程 (8) \n" +
                "Z\n" +
                "(\n" +
                "z\n" +
                ")\n" +
                "=\n" +
                "K\n" +
                "∫\n" +
                "S\n" +
                "d\n" +
                "−\n" +
                "1\n" +
                "M\n" +
                "−\n" +
                "z\n" +
                "(\n" +
                "x\n" +
                ")\n" +
                "d\n" +
                "σ\n" +
                "(\n" +
                "x\n" +
                ")\n" +
                "Z(z)=K∫ \n" +
                "S \n" +
                "d−1\n" +
                " \n" +
                "\u200B\n" +
                " M \n" +
                "−z\n" +
                "\u200B\n" +
                " (x)dσ(x)\n" +
                "。请追溯到第二章（Preliminaries），解释公式 (8) 中的 \n" +
                "M\n" +
                "−\n" +
                "z\n" +
                "(\n" +
                "x\n" +
                ")\n" +
                "M \n" +
                "−z\n" +
                "\u200B\n" +
                " (x)\n" +
                " 在几何上代表什么变换？为什么在 \n" +
                "d\n" +
                "≥\n" +
                "3\n" +
                "d≥3\n" +
                " 的高维情况下，这个积分的结果不再像 \n" +
                "d\n" +
                "=\n" +
                "2\n" +
                "d=2\n" +
                " 那样简单等于 \n" +
                "K\n" +
                "z\n" +
                "Kz\n" +
                "，而是需要引入双曲 Poisson 核（hyperbolic Poisson kernel）和超几何函数（hypergeometric function）");

        log.info("================ FINAL ANSWER ================");
        log.info(answer);
    }
    @Test
    public void testSession2() {

        String answer = agent.startResearch("session_5", "Identical phase oscillators with global sinusoidal coupling evolve by Möbius group action (2009) 的论文后续有什么发展");

        log.info("================ FINAL ANSWER ================");
        log.info(answer);
    }


}
