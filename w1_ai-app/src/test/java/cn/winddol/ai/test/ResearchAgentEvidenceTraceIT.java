package cn.winddol.ai.test;

import cn.winddol.ai.agent.research.api.IResearchAgent;
import cn.winddol.ai.framework.event.AgentEvent;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段 3A 的手动端到端测试。
 *
 * <p>从 ResearchAgent 的真实 LLM 对话进入，要求其调用阶段 2 的检索和章节阅读工具，
 * 最后校验 EVIDENCE 事件中携带的 sectionId、chunkId、Outline 路径和原文片段。
 * 该测试会调用 LLM 和 embedding 服务，故以 IT 结尾且标记为 manual，默认不参与常规单测。</p>
 */
@Tag("manual")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class ResearchAgentEvidenceTraceIT {

    @Autowired
    private IResearchAgent researchAgent;

    @Autowired
    private IPaperRepository paperRepository;

    @Value("${paper.research.evidence-test.paper-id:7}")
    private Long paperId;

    @Value("${paper.research.evidence-test.query:Mobius group action}")
    private String query;

    /**
     * 验证 Agent 不是由模型虚构出处，而是将阶段 2 召回的真实 chunk 定位信息透传到 EVIDENCE 事件。
     */
    @Test
    void publishesTraceableEvidenceFromActualStageTwoRetrieval() {
        assertNotNull(paperRepository.selectPaperById(paperId), "Paper does not exist: " + paperId);

        List<AgentEvent> events = new CopyOnWriteArrayList<>();
        String sessionId = "manual-evidence-" + UUID.randomUUID();
        String answer = researchAgent.doResearch(
                sessionId,
                buildTaskDescription(),
                MessageWindowChatMemory.builder().maxMessages(20).build(),
                events::add
        );

        assertFalse(answer == null || answer.isBlank(), "ResearchAgent returned an empty answer");
        assertTrue(events.stream().anyMatch(event -> event.getType() == AgentEvent.Type.ACTION
                        && "searchLibrary".equals(event.getContent())),
                "The agent did not call searchLibrary; inspect the live model output before treating this as a retrieval regression.");
        assertTrue(events.stream().anyMatch(event -> event.getType() == AgentEvent.Type.ACTION
                        && "readSection".equals(event.getContent())),
                "The agent did not read a concrete section after retrieval.");

        AgentEvent evidenceEvent = events.stream()
                .filter(event -> event.getType() == AgentEvent.Type.EVIDENCE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ResearchAgent did not publish an EVIDENCE event"));
        List<ToolEvidence> evidence = JSON.parseArray(evidenceEvent.getData(), ToolEvidence.class);

        assertFalse(evidence == null || evidence.isEmpty(), "The final EVIDENCE event is empty");
        assertTrue(evidence.stream().anyMatch(item -> paperId.equals(item.getPaperId())
                        && "SECTION".equals(item.getEvidenceType())
                        && hasText(item.getSectionId())
                        && hasText(item.getChunkId())
                        && hasText(item.getHeadingPath())
                        && hasText(item.getQuote())
                        && "searchLibrary".equals(item.getAccessedVia())),
                "No stage-2 section chunk with location metadata reached the final evidence event");
        assertTrue(evidence.stream().anyMatch(item -> paperId.equals(item.getPaperId())
                        && "SECTION".equals(item.getEvidenceType())
                        && hasText(item.getSectionId())
                        && hasText(item.getHeadingPath())
                        && "readSection".equals(item.getAccessedVia())),
                "No directly read section reached the final evidence event");

        System.out.printf("[RESEARCH-EVIDENCE] paperId=%d events=%d evidence=%d%n",
                paperId, events.size(), evidence.size());
        evidence.forEach(item -> System.out.printf(
                "  -> key=%s type=%s section=%s chunk=%s path=%s via=%s%n",
                item.getEvidenceKey(), item.getEvidenceType(), item.getSectionId(),
                item.getChunkId(), item.getHeadingPath(), item.getAccessedVia()));
    }

    /**
     * 明确约束模型必须先检索再阅读，避免真实 LLM 直接用常识回答而跳过 3A 证据链。
     */
    private String buildTaskDescription() {
        return """
                Answer in Simplified Chinese using only the private paper library.
                The target paperId is %d and the topic is \"%s\".
                You MUST first call searchLibrary with that English query and paperId=%d.
                Then choose a returned SECTION evidence item and call readSection with its Section ID.
                Give a concise answer based on that section and do not answer from general knowledge.
                """.formatted(paperId, query, paperId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
