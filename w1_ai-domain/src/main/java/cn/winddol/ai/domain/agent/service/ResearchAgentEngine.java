package cn.winddol.ai.domain.agent.service;

import cn.winddol.ai.domain.agent.adapter.repository.IAiAdapter;
import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.domain.paperTools.adapter.tools.ScientificResearchTools;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ResearchAgentEngine {

    @Resource
    private ChatLanguageModel chatLanguageModel;
    @Resource
    private ScientificResearchTools tools;
    @Resource
    private ChatMemoryStore chatMemoryStore;
    @Resource
    private IAiAdapter aiAdapter;
    private static final String SYSTEM_PROMPT = """
            You are 'ScholarBrain', an advanced autonomous research assistant.
            You have access to a private library of parsed scientific papers.
            
            [YOUR GOAL]
            Answer the user's question accurately using the provided tools.
            Do NOT hallucinate. If you don't know, search. If you found a location, read it.
            Think like a scientist: analyze the structure, find symbols, read sections, and follow references.
            
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
    private ChatMemory getMemory(String sessionId) {
        return MessageWindowChatMemory.builder()
                .id(sessionId)              // 关键：ID 对应 Redis 中的 Key
                .maxMessages(20)            // 保留最近 20 条消息
                .chatMemoryStore(chatMemoryStore) // 关键：数据持久化到 Redis
                .build();
    }

    public String run(String sessionId, String userQuestion){
        ChatMemory memory = getMemory(sessionId);
        String refinedQuestion = aiAdapter.rewriteQueryIfNecessary(userQuestion, memory.messages());
        log.info("📝 Original: '{}' -> Rewritten: '{}'", userQuestion, refinedQuestion);

        memory.add(UserMessage.from(userQuestion));
        List<ChatMessage> context = new ArrayList<>();
        context.add(SystemMessage.from(SYSTEM_PROMPT));
        context.addAll(memory.messages());
        context.add(UserMessage.from(
                "The user's request (resolved and translated): " + refinedQuestion
        ));
        log.info("🤖 Agent started. Question: {}", userQuestion);
        String finalAnswer = executeReActLoop(context);
        memory.add(AiMessage.from(finalAnswer));
        return finalAnswer;
    }


    public String run(String userQuestion) {
        List<ChatMessage> history = new ArrayList<>();
        history.add(SystemMessage.from(SYSTEM_PROMPT));
        history.add(UserMessage.from("Question:" + userQuestion));
        log.info("🤖 Agent started. Question: {}", userQuestion);
        return executeReActLoop(history);
    }

    private String executeReActLoop(List<ChatMessage> history){
        int maxSteps = 20;

        for (int i = 0; i < maxSteps; i++) {
            Response<AiMessage> response = chatLanguageModel.generate(history);
            String llmOutput = response.content().text();
            history.add(AiMessage.from(llmOutput));

            AgentStep step = parseOutput(llmOutput);
            if (step == null) {
                history.add(UserMessage.from("System Error: Invalid JSON format. Please output strictly JSON."));
                continue;
            }
            log.info("🔄 Step {}: [Thought] {} -> [Action] {}({})",
                    i + 1, step.getThought(), step.getAction(), step.getActionInput());
            if (step.getFinalAnswer() != null && !StringUtils.isBlank(step.getFinalAnswer())) {
                log.info("🛑 Step {}: [Final Answer Ready] {}", i+1, step.getThought());
                return step.getFinalAnswer();
            }
            String observation = executeTool(step.getAction(), step.getActionInput());

            history.add(UserMessage.from("Observation: " + observation));

        }
        return "❌ Failed to answer within step limit.";
    }

    private String executeTool(String toolName, String input) {
        try {
            if ("searchLibrary".equalsIgnoreCase(toolName)) {
                String query;
                Long paperId = null;
                Double threshold = null;
                // 尝试检测是否为 JSON 格式 (简单的启发式判断)
                String trimmedInput = input.trim();
                if (trimmedInput.startsWith("{") && trimmedInput.endsWith("}")) {
                    try {
                        // 解析 JSON 参数
                        JSONObject params = JSON.parseObject(trimmedInput);
                        query = params.getString("query");
                        // 处理 paperId (可能是 Integer 或 Long)
                        if (params.containsKey("paperId")) {
                            paperId = params.getLong("paperId");
                        }
                        if (params.containsKey("threshold")) {
                            threshold = params.getDouble("threshold");
                        }
                    } catch (Exception e) {
                        // 如果解析 JSON 失败，回退到将整个 input 当作 query
                        log.warn("Failed to parse searchLibrary params as JSON, using raw string. Error: {}", e.getMessage());
                        query = trimmedInput;
                    }
                } else {
                    // 如果不是 JSON，说明 Agent 只是想做全局搜索
                    query = trimmedInput;
                }

                return tools.searchLibrary(query, paperId,threshold);
            }

            // -------------------------------------------------------
            // Tool 2: getPaperOutline (通常是单参数 ID)
            // -------------------------------------------------------
            if ("getPaperOutline".equalsIgnoreCase(toolName)) {
                // 这里的 input 应该是个数字字符串，但防止 Agent 传了 JSON {"paperId": 7}
                String idStr = extractSimpleValue(input, "paperId");
                return tools.getPaperOutline(Long.parseLong(idStr));
            }

            // -------------------------------------------------------
            // Tool 3: readSection (单参数 UUID)
            // -------------------------------------------------------
            if ("readSection".equalsIgnoreCase(toolName)) {
                // 防止 Agent 传了 {"sectionUuid": "..."}
                String uuid = extractSimpleValue(input, "sectionUuid");
                return tools.readSection(uuid);
            }
            // -------------------------------------------------------
            // Tool 4: lookupReference (多参数)
            // -------------------------------------------------------
            if ("lookupReference".equalsIgnoreCase(toolName)) {
                try {
                    // 确保输入被当作 JSON 解析
                    JSONObject params = JSON.parseObject(input);
                    Long pId = params.getLong("paperId");
                    String rIdx = params.getString("refIndex");

                    if (pId == null || rIdx == null) {
                        return "Error: lookupReference requires both 'paperId' and 'refIndex'.";
                    }
                    return tools.lookupReference(pId, rIdx);
                } catch (Exception e) {
                    // 如果 LLM 没按 JSON 格式传参的兜底处理
                    log.warn("lookupReference params parsing failed: {}", input);
                    return "Error: lookupReference requires a JSON input like {\"paperId\": 7, \"refIndex\": \"24\"}";
                }
            }

            return "Error: Unknown tool '" + toolName + "'";
        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return "Error executing tool: " + e.getMessage();
        }
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
