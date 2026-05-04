package cn.winddol.ai.agent.research.internal;

import cn.winddol.ai.agent.research.api.IResearchAgent;
import cn.winddol.ai.agent.research.api.ResearchEventListener;
import cn.winddol.ai.agent.research.domain.AgentStep;
import cn.winddol.ai.framework.event.AgentEvent;
import cn.winddol.ai.framework.tool.ToolProvider;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResearchAgentImpl implements IResearchAgent {

    private final ChatLanguageModel chatLanguageModel;
    private final ToolProvider toolProvider;

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

    public ResearchAgentImpl(ChatLanguageModel chatLanguageModel, ToolProvider toolProvider) {
        this.chatLanguageModel = chatLanguageModel;
        this.toolProvider = toolProvider;
    }

    @Override
    public String doResearch(String sessionId, String taskDescription, ChatMemory memory, ResearchEventListener listener) {
        String dynamicSystemPrompt = SYSTEM_PROMPT + "\n\n" +
                "=========================================\n" +
                "[YOUR CURRENT MISSION]\n" +
                "You must solve the following specific task:\n" +
                taskDescription + "\n" +
                "=========================================\n";

        List<ChatMessage> history = new java.util.ArrayList<>();
        history.add(SystemMessage.from(dynamicSystemPrompt));
        history.addAll(memory.messages());
        history.add(UserMessage.from("Please begin your research process to solve the mission defined in the system prompt."));
        log.info("🤖 Agent started. Question: {}", taskDescription);
        String finalAnswer = executeReActLoop(sessionId, history, listener);
        memory.add(AiMessage.from(finalAnswer));
        return finalAnswer;
    }

    private String executeReActLoop(String sessionId, List<ChatMessage> history, ResearchEventListener listener) {
        int maxSteps = 20;
        int maxSafeMessages = 30;
        for (int i = 0; i < maxSteps; i++) {
            if (history.size() > maxSafeMessages) {
                history.remove(3);
                history.remove(3);
                log.warn("🧹 Context window getting too large, evicted oldest intermediate steps.");
            }
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
            listener.onStep(AgentEvent.builder()
                    .sessionId(sessionId)
                    .type(AgentEvent.Type.THOUGHT)
                    .content(step.getThought())
                    .step(i)
                    .build());

            if (step.getFinalAnswer() != null && !StringUtils.isBlank(step.getFinalAnswer())) {
                log.info("🛑 Step {}: [Final Answer Ready] {}", i + 1, step.getThought());
                listener.onStep(AgentEvent.builder()
                        .sessionId(sessionId)
                        .type(AgentEvent.Type.ANSWER)
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
                    prompt = "System Error: You indicated you are ready to provide the final answer, but the JSON field 'finalAnswer' is MISSING. " +
                            "Please output the JSON again with the answer included. " +
                            "Format: {\"thought\": \"...\", \"finalAnswer\": \"YOUR CHINESE ANSWER HERE\"}";
                } else {
                    prompt = "System Error: You provided a 'thought' but missed the 'action'. " +
                            "If you want to use a tool, specify it in 'action'. " +
                            "If you have the answer, use the 'finalAnswer' field.";
                }
                history.add(UserMessage.from(prompt));
                continue;
            }

            listener.onStep(AgentEvent.builder()
                    .sessionId(sessionId)
                    .type(AgentEvent.Type.ACTION)
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

            String observation = toolProvider.executeTool(step.getAction(), step.getActionInput()).getContent();
            String preview = observation;
            if (observation.length() > 5000) {
                preview = observation.substring(0, 5000) + "\n\n...(Total " + observation.length() + " chars, truncated for display)";
            }
            listener.onStep(AgentEvent.builder()
                    .sessionId(sessionId)
                    .type(AgentEvent.Type.OBSERVATION)
                    .content(preview)
                    .step(i)
                    .build());
            history.add(UserMessage.from("Observation: " + observation));
        }
        return "❌ Failed to answer within step limit.";
    }

    private AgentStep parseOutput(String llmOutput) {
        try {
            String json = llmOutput.replaceAll("```json", "").replaceAll("```", "").trim();
            AgentStep step = JSON.parseObject(json, AgentStep.class);
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
