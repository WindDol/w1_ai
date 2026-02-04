package cn.winddol.ai.test;

import cn.winddol.ai.domain.agent.service.ResearchAgentEngine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class TestAgent {
    @Resource
    private ResearchAgentEngine agentEngine;

    @Test
    public void testComplexReasoning() {
        // 提一个这篇论文特有的、如果不查正文绝对回答不上来的问题
        String question = "在推导公式 21 时，Möbius 变换的参数 alpha 和 psi 满足什么微分方程？";

        String answer = agentEngine.run(question);

        log.info("================ FINAL ANSWER ================");
        log.info(answer);
    }

    @Test
    public void testReferenceTrackingReasoning() {
        String question = "在讨论 Möbius 变换的Algebraic method时，作者提到了参考文献 [24]。请问 [24] 这篇论文的标题是什么？它主要解决了什么问题？";

        String answer = agentEngine.run(question);

        log.info("================ FINAL ANSWER ================");
        log.info(answer);
    }
}
