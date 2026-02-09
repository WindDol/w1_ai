package cn.winddol.ai.domain.agent.service.bussiness;

import cn.winddol.ai.domain.agent.adapter.repository.IAgentRepository;
import cn.winddol.ai.domain.agent.event.ResearchEvent;
import cn.winddol.ai.domain.agent.model.entity.AgentStep;
import cn.winddol.ai.domain.paperTools.service.IScientificResearchTools;
import cn.winddol.ai.domain.paperTools.service.ScientificResearchTools;
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
    private IScientificResearchTools tools;
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
            - LIBRARY CONTEXT: Before concluding your analysis, ALWAYS use 'checkPaperRelations' to see if the private database contains existing critiques or extensions of the current paper.
            - NOVELTY ASSESSMENT: If a paper 'EXTENDS' another, highlight what was added (e.g., higher dimensions, new parameters).
            - MACRO vs MICRO: For broad questions like "what papers do we have?", use 'findPapers'. For deep reading, use 'readSection'.
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
                - Query the private database for the Librarian's evaluation reports and relationship mappings.
                - This tool reveals how other papers in the local library REVIEW, SUPPORT, CONTRADICT, or EXTEND this paper.
                - Use this to understand the paper's standing, relevance, and critical reception within your private collection.
                - Usage Example: {"paperId": 7}
            6. getTopCitedReferences(limit):
                - Identify the most influential references within the library.
                - Use this to find foundational works (e.g., "What is the most cited paper here?").
                - Input: {"limit": 5} (Optional)
            7. findPapers(query, threshold):
                - Search for papers by title, author, or abstract keywords.
                - Use this for MACRO-level discovery (e.g., "List papers by Seth Marvel").
                - 'query': The search keyword (Required).
                - 'threshold': Similarity threshold (Optional, Double). Default is 0.5.
                   Strategy: If previous search returned 0 results, retry with threshold=0.35. If too many irrelevant results, retry with 0.7.
                - Usage: {"query": "Kuramoto model", "threshold": 0.5}
            
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
    public String doResearch(String sessionId, String taskDescription, ChatMemory memory, ResearchProcessListener listener) {
        List<ChatMessage> context = new ArrayList<>();
        context.add(SystemMessage.from(SYSTEM_PROMPT));
        context.addAll(memory.messages());
        context.add(UserMessage.from(
                "The user's request (resolved and translated): " + taskDescription
        ));
        log.info("🤖 Agent started. Question: {}", taskDescription);
        String finalAnswer = executeReActLoop(sessionId,context,listener);
        memory.add(AiMessage.from(finalAnswer));
        return finalAnswer;
    }


    private String executeReActLoop(String sessionId, List<ChatMessage> history, ResearchProcessListener listener){
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
            listener.onStep(ResearchEvent.builder()
                            .sessionId(sessionId)
                            .type("THOUGHT")
                            .content(step.getThought())
                            .step(i)
                            .build());


            if (step.getFinalAnswer() != null && !StringUtils.isBlank(step.getFinalAnswer())) {
                log.info("🛑 Step {}: [Final Answer Ready] {}", i+1, step.getThought());
                repository.logStep(sessionId, i + 1, step, "FINAL_ANSWER_GENERATED");
                listener.onStep(ResearchEvent.builder()
                        .sessionId(sessionId)
                        .type("ANSWER")
                        .content(step.getFinalAnswer())
                        .step(i)
                        .build());
                return step.getFinalAnswer();
            }

            if (StringUtils.isBlank(step.getAction()) || "null".equalsIgnoreCase(step.getAction())) {
                log.warn("⚠️ Step {}: Missing Action and FinalAnswer. Asking LLM to correct.", i + 1);
                String prompt;
                if (step.getThought().toLowerCase().contains("final answer") ||
                        step.getThought().toLowerCase().contains("answer in chinese")) {
                    // 如果 Thought 里提到了要回答，但没给字段，直接把 JSON 模板甩给它
                    prompt = "System Error: You indicated you are ready to provide the final answer, but the JSON field 'finalAnswer' is MISSING. " +
                            "Please output the JSON again with the answer included. " +
                            "Format: {\"thought\": \"...\", \"finalAnswer\": \"YOUR CHINESE ANSWER HERE\"}";
                } else {
                    // 普通的 Action 缺失
                    prompt = "System Error: You provided a 'thought' but missed the 'action'. " +
                            "If you want to use a tool, specify it in 'action'. " +
                            "If you have the answer, use the 'finalAnswer' field.";
                }

                history.add(UserMessage.from(prompt));
                continue; // 跳过本次 executeTool，直接进入下一轮循环
            }

            listener.onStep(ResearchEvent.builder()
                    .sessionId(sessionId)
                    .type("ACTION")
                    .content(step.getAction())
                    .data(step.getActionInput())
                    .step(i)
                    .build());

            log.info("🔄 Step {}: [Thought] {} -> [Action] {}({})",
                    i + 1, step.getThought(), step.getAction(), step.getActionInput());
            if (i == maxSteps - 1) {
                log.warn("⚠️ Agent failed to provide finalAnswer in last step. Fallback to thought summary.");
                return "【自动汇总】由于搜索步数达到上限，根据已有资料整理如下：" + step.getThought();
            }
            String observation = executeTool(step.getAction(), step.getActionInput());
            String preview = observation;
            if (observation.length() > 1000) {
                // 截取前 1000 字，并加上省略号和统计信息，增加透明度
                preview = observation.substring(0, 1000) + "\n\n...(Total " + observation.length() + " chars, truncated for display)";
            }
            listener.onStep(ResearchEvent.builder()
                    .sessionId(sessionId)
                    .type("OBSERVATION")
                    .content(preview)
                    .step(i)
                    .build());
            history.add(UserMessage.from("Observation: " + observation));
            repository.logStep(sessionId,i+1,step,observation);
        }
        return "❌ Failed to answer within step limit.";
    }


    private String executeTool(String toolName, String inputRaw) {
        if (toolName == null || "null".equalsIgnoreCase(toolName.trim())) {
            return "System Error: No tool name provided (Action was null). Please specify a valid action or use 'finalAnswer'.";
        }
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
                case "getTopCitedReferences":
                    Integer limit = params.getInteger("limit");
                    if (limit == null) limit = 5; // 默认查 Top 5
                    return tools.getTopCitedReferences(limit);
                case "findPapers":
                    String paperQuery = params.getString("query");
                    Double paperThreshold = params.getDouble("threshold");
                    if (paperQuery == null && !params.isEmpty()) paperQuery = inputRaw;
                    if (paperQuery == null) return "Error: Missing 'query' parameter.";
                    return tools.findPapers(paperQuery, paperThreshold);
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
            // 1. 提取 JSON 内容
            String json = extractJson(llmOutput);
            if (json == null) return null;

            // 2. 预处理非法转义 (保留你原本的逻辑，虽然简单粗暴但能防 LaTeX 报错)
            String sanitizedJson = json
                    .replace("\\n", "###NEWLINE###")  // 先把合法的 \n 藏起来
                    .replace("\\\"", "###QUOTE###")   // 先把合法的 \" 藏起来
                    .replace("\\", "\\\\")            // 剩下的反斜杠全是 LaTeX 的，统统转义
                    .replace("###NEWLINE###", "\\n")  // 还原换行
                    .replace("###QUOTE###", "\\\"");  // 还原引号

            // 3. 解析 JSON
            JSONObject jsonObject = JSON.parseObject(sanitizedJson);

            AgentStep step = new AgentStep();
            step.setThought(jsonObject.getString("thought"));
            step.setAction(jsonObject.getString("action"));
            step.setActionInput(jsonObject.getString("actionInput"));
            step.setFinalAnswer(jsonObject.getString("finalAnswer"));

            // 4. 处理 actionInput
            Object inputObj = jsonObject.get("actionInput");
            if (inputObj instanceof String) {
                step.setActionInput((String) inputObj);
            } else if (inputObj != null) {
                step.setActionInput(JSON.toJSONString(inputObj));
            }

            return step;
        } catch (Exception e) {
            log.error("JSON Parse Error. Raw Output: {}", llmOutput, e);
            return null;
        }
    }
    private String extractJson(String output) {
        int start = output.indexOf("{");
        int end = output.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return output.substring(start, end + 1);
        }
        return null;
    }


}
