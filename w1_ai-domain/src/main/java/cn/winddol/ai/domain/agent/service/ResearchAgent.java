package cn.winddol.ai.domain.agent.service;

import cn.winddol.ai.domain.agent.adapter.repository.IAgentRepository;
import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.domain.paperTools.adapter.tools.ScientificResearchTools;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ResearchAgent {

    @Resource
    private ChatLanguageModel chatLanguageModel;
    @Resource
    private ScientificResearchTools tools;
    @Resource
    private IAgentRepository repository;

    private static final String SYSTEM_PROMPT = """
            You are 'ScholarBrain', an advanced autonomous research assistant.
            You have access to a private library of parsed scientific papers.
            
            [YOUR GOAL]
            Answer the user's question accurately using the provided tools.
            Do NOT hallucinate. If you don't know, search. If you found a location, read it.
            Think like a scientist: analyze the structure, find symbols, read sections, and follow references.
            
            [SCIENTIFIC REASONING GUIDELINES]
            - CRITICAL THINKING: Scientific progress is built on consensus and conflict.
            - PROACTIVE CHECK: When you identify a paper ID, check if there are any relations or conflicts with other papers using 'checkPaperRelations'.
            - NOVELTY ASSESSMENT: If a paper 'EXTENDS' another, highlight what was added (e.g., higher dimensions, new parameters).
           
            [LANGUAGE PROTOCOL]
            1. INTERNAL REASONING: All your 'thought' fields MUST be written in ENGLISH.
            2. TOOL CALLS: All search queries and tool parameters MUST be in ENGLISH.
            3. FINAL OUTPUT: Your 'finalAnswer' MUST be written in CHINESE (Simplified).
            
            [AVAILABLE TOOLS]
            1. searchLibrary(query, paperId, threshold):
                - Search for relevant sections, symbols, and references.
                - 'query': The search keyword (Required).
                - 'paperId': Specific paper ID (Optional, Long). Use null to search everywhere.
                - 'threshold': Similarity threshold (Optional, Double). Range [0.35, 0.7]. Default is 0.5.
                   Hint: Increase to 0.6 if results are irrelevant; decrease to 0.35 if no results found.
                - Usage Example: {"query": "soliton", "paperId": 123, "threshold": 0.5} OR just "soliton" for global search.
            2. getPaperOutline(paperId):
                - Get the hierarchical table of contents for a paper.
                - Usage: {"paperId": 7}
            3. readSection(sectionUuid):
                - Read full content of a section with context and symbols.
                - Usage: {"sectionUuid": "uuid-string"}
            4. lookupReference(paperId, refIndex):
                - Get specific title and abstract for a citation index found in text (e.g., "[12]", "Ref 24").
                - Use this when text mentions a citation and you need to know what that external work is about.
                - Usage: {"paperId": 7, "refIndex": "24"}
            5. checkPaperRelations(paperId):
                - Check for inter-paper relationships (conflicts, supports, or extensions) found by the Librarian.
                - Use this when the user asks about controversies, contradictions, or how this paper relates to other works in the library.
                - Usage Example: {"paperId": 7}
            
            [PROTOCOL]
            Output ONLY a JSON object.
            If a tool requires multiple parameters (like searchLibrary or lookupReference), put them inside 'actionInput' as a JSON structure.
            
            Example:
            {
              "thought": "I need to search for 'Möbius' inside paper 7.",
              "action": "searchLibrary",\s
              "actionInput": "{\\"query\\": \\"Möbius\\", \\"paperId\\": 7, \\"threshold\\": 0.5}"\s
            }
            Or
            {
              "thought": "The text mentions reference [24] in the derivation. I need to check its background.",
              "action": "lookupReference",
              "actionInput": "{\\\\"paperId\\\\": 7, \\\\"refIndex\\\\": \\\\"24\\\\"}"
            }
            
            OR, if you have gathered enough information to answer:
            
            {
              "thought": "I have read the necessary sections and can now answer.",
              "finalAnswer": "Your comprehensive answer here..."
            }
            """;
    public String doResearch(String sessionId, String taskDescription, ChatMemory memory) {
        List<ChatMessage> context = new ArrayList<>();
        context.add(SystemMessage.from(SYSTEM_PROMPT));
        context.addAll(memory.messages());
        context.add(UserMessage.from(
                "The user's request (resolved and translated): " + taskDescription
        ));
        log.info("🤖 Agent started. Question: {}", taskDescription);
        String finalAnswer = executeReActLoop(sessionId,context);
        memory.add(AiMessage.from(finalAnswer));
        return finalAnswer;
    }


    private String executeReActLoop(String sessionId, List<ChatMessage> history){
        int maxSteps = 20;

        for (int i = 0; i < maxSteps; i++) {
            if (i == maxSteps - 2) {
                history.add(SystemMessage.from(
                        "WARNING: You have almost reached the step limit. " +
                                "Stop searching. Synthesize what you have gathered so far and generate the 'finalAnswer' immediately."
                ));
            }
            Response<AiMessage> response = chatLanguageModel.generate(history);
            String llmOutput = response.content().text();
            history.add(AiMessage.from(llmOutput));

            AgentStep step = parseOutput(llmOutput);
            if (step == null) {
                history.add(UserMessage.from("System Error: Your output was not valid JSON. Please fix the format and output ONLY JSON."));
                i--;
                continue;
            }
            if (step.getFinalAnswer() != null && !StringUtils.isBlank(step.getFinalAnswer())) {
                log.info("🛑 Step {}: [Final Answer Ready] {}", i+1, step.getThought());
                repository.logStep(sessionId, i + 1, step, "FINAL_ANSWER_GENERATED");
                return step.getFinalAnswer();
            }
            log.info("🔄 Step {}: [Thought] {} -> [Action] {}({})",
                    i + 1, step.getThought(), step.getAction(), step.getActionInput());
            if (i == maxSteps - 1) {
                log.warn("⚠️ Agent failed to provide finalAnswer in last step. Fallback to thought summary.");
                return "【自动汇总】由于搜索步数达到上限，根据已有资料整理如下：" + step.getThought();
            }
            String observation = executeTool(step.getAction(), step.getActionInput());
            history.add(UserMessage.from("Observation: " + observation));
            repository.logStep(sessionId,i+1,step,observation);
        }
        return "❌ Failed to answer within step limit.";
    }

    private String executeTool(String toolName, String inputRaw) {
        try {
            // 1. 统一预处理：确保 input 是个合法的 JSON 对象
            // 如果 LLM 偷懒直接传了字符串 "soliton"，我们帮它包装成 {"query": "soliton"}
            JSONObject params = smartParseInput(inputRaw);

            switch (toolName) {
                case "searchLibrary":
                    String query = params.getString("query");
                    // 默认值处理
                    Long pId = params.getLong("paperId"); // fastjson 若无key返回 null
                    Double threshold = params.getDouble("threshold");
                    // 智能兜底：如果没传 query 但传了 raw string
                    if (query == null && !params.isEmpty()) query = inputRaw;

                    return tools.searchLibrary(query, pId, threshold);

                case "getPaperOutline":
                    return tools.getPaperOutline(params.getLong("paperId"));

                case "readSection":
                    return tools.readSection(params.getString("sectionUuid"));

                case "lookupReference":
                    Long refPaperId = params.getLong("paperId");
                    String refIndex = params.getString("refIndex");
                    if (refPaperId == null || refIndex == null) {
                        return "Error: Missing parameters. Required: paperId, refIndex.";
                    }
                    return tools.lookupReference(refPaperId, refIndex);
                case "checkPaperRelations":
                    Long paperId = params.getLong("paperId");
                    if (paperId == null) {
                        return "Error: Missing parameters. Required: paperId.";
                    }
                    return tools.checkPaperRelations(paperId);
                default:
                    return "Error: Unknown tool '" + toolName + "'. Check tool definitions.";
            }
        } catch (Exception e) {
            log.error("Tool Execution Error: {} | Input: {}", toolName, inputRaw, e);
            return "System Error executing tool: " + e.getMessage();
        }
    }
    private JSONObject smartParseInput(String input) {
        if (input == null || input.isBlank()) return new JSONObject();
        String trimmed = input.trim();

        // 如果看起来像 JSON
        if (trimmed.startsWith("{")) {
            try {
                return JSON.parseObject(trimmed);
            } catch (Exception e) {
                // 解析失败，降级处理
            }
        }

        // 如果不是 JSON，或者是解析失败的 JSON，尝试作为单参数处理
        // 这里假设这就 query 或者 uuid
        JSONObject fallback = new JSONObject();
        fallback.put("query", trimmed);       // 适配 search
        fallback.put("sectionUuid", trimmed); // 适配 read
        fallback.put("paperId", trimmed.replaceAll("\\D", "")); // 适配 outline (只取数字)
        return fallback;
    }


    private String extractSimpleValue(String input, String keyName) {
        String trimmed = input.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JSONObject json = JSON.parseObject(trimmed);
                return json.getString(keyName);
            } catch (Exception e) {
                return trimmed; // 解析失败，直接返回原值试试
            }
        }
        return trimmed;
    }

    private AgentStep parseOutput(String llmOutput) {
        try {
            // 1. 清除 Markdown 格式块
            String json = llmOutput.replaceAll("```json", "").replaceAll("```", "").trim();
            AgentStep step = JSON.parseObject(json, AgentStep.class);

            // 2. 处理 actionInput 中可能的二次转义问题
            // 有时模型会输出 "actionInput": "{\"query\": \"Möbius\"}"
            // FastJSON 有时会将其识别为双重转义字符串，这里确保其为纯 JSON 串
            if (step.getActionInput() != null) {
                String input = step.getActionInput().trim();
                if (input.startsWith("\"") && input.endsWith("\"") && input.length() > 2) {
                    input = input.substring(1, input.length() - 1).replace("\\\"", "\"");
                    step.setActionInput(input);
                }
            }
            return step;
        } catch (Exception e) {
            log.error("JSON Parse Error. Output: {}", llmOutput);
            return null;
        }
    }


}
