package cn.winddol.ai.domain.agent.service;

import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.domain.paperTools.adapter.tools.ScientificResearchTools;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
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

    private static final String SYSTEM_PROMPT = """
            You are 'ScholarBrain', an advanced autonomous research assistant.
            You have access to a private library of parsed scientific papers.
            
            [YOUR GOAL]
            Answer the user's question accurately using the provided tools. 
            Do NOT hallucinate. If you don't know, search. If you found a location, read it.
            
            [AVAILABLE TOOLS]
            1. searchLibrary(query, paperId):\s
                - 'query': The search keyword (Required).
                - 'paperId': Specific paper ID (Optional, Long). Use null to search everywhere.
                - Usage Example: {"query": "soliton", "paperId": 123} OR just "soliton" for global search.
            2. getPaperOutline(paperId): ...
            3. readSection(sectionUuid): ...
            
            [PROTOCOL]
            Output ONLY a JSON object.
            If a tool requires multiple parameters (like searchLibrary), put them inside 'actionInput' as a JSON structure.
            
            Example:
            {
              "thought": "I need to search for 'Möbius' inside paper 7.",
              "action": "searchLibrary",\s
              "actionInput": "{\\"query\\": \\"Möbius\\", \\"paperId\\": 7}"\s
            }
            
            OR, if you have gathered enough information to answer:
            
            {
              "thought": "I have read the necessary sections and can now answer.",
              "finalAnswer": "Your comprehensive answer here..."
            }
            """;

    public String run(String userQuestion) {
        List<ChatMessage> history = new ArrayList<>();
        history.add(SystemMessage.from(SYSTEM_PROMPT));
        history.add(UserMessage.from("Question:" + userQuestion));

        int maxSteps = 10;
        log.info("🤖 Agent started. Question: {}", userQuestion);
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
                    } catch (Exception e) {
                        // 如果解析 JSON 失败，回退到将整个 input 当作 query
                        log.warn("Failed to parse searchLibrary params as JSON, using raw string. Error: {}", e.getMessage());
                        query = trimmedInput;
                    }
                } else {
                    // 如果不是 JSON，说明 Agent 只是想做全局搜索
                    query = trimmedInput;
                }

                return tools.searchLibrary(query, paperId);
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
            // 清洗 Markdown 代码块标记
            String json = llmOutput.replaceAll("```json", "").replaceAll("```", "").trim();
            return JSON.parseObject(json, AgentStep.class);
        } catch (Exception e) {
            log.error("JSON Parse Error. Output: {}", llmOutput);
            return null;
        }
    }
}
