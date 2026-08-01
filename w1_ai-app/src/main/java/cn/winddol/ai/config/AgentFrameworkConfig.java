package cn.winddol.ai.config;

import cn.winddol.ai.domain.agent.adapter.event.NotificationService;
import cn.winddol.ai.framework.event.AgentEvent;
import cn.winddol.ai.framework.memory.AgentMemory;
import cn.winddol.ai.framework.memory.AgentMemoryFactory;
import cn.winddol.ai.framework.tool.Tool;
import cn.winddol.ai.framework.tool.ToolProvider;
import cn.winddol.ai.paper.api.IScientificResearchTools;
import cn.winddol.ai.shared.model.tool.ToolResult;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Configuration
public class AgentFrameworkConfig {

    /** 将框架事件桥接到既有 SSE 推送通道。 */
    @Bean
    public Consumer<AgentEvent> agentEventSink(NotificationService notificationService) {
        return event -> notificationService.send(event.getSessionId(), event);
    }

    /** 基于 Redis ChatMemoryStore 为每个会话创建独立记忆。 */
    @Bean
    public AgentMemoryFactory agentMemoryFactory(ChatMemoryStore chatMemoryStore) {
        return new AgentMemoryFactory() {
            @Override
            public AgentMemory create(String sessionId, int maxMessages) {
                ChatMemory chatMemory = MessageWindowChatMemory.builder()
                        .id(sessionId)
                        .maxMessages(maxMessages)
                        .chatMemoryStore(chatMemoryStore)
                        .build();
                return new AgentMemoryBridge(chatMemory);
            }
        };
    }

    /** 将论文工具适配为 ResearchAgent 可调用的统一 ToolProvider。 */
    @Bean
    public ToolProvider researchToolProvider(IScientificResearchTools tools) {
        return new ToolProvider() {
            @Override
            public List<Tool> getTools() {
                return Collections.emptyList();
            }

            @Override
            public ToolResult executeTool(String toolName, String inputJson) {
                return dispatchTool(tools, toolName, inputJson);
            }
        };
    }

    /**
     * 调度 ResearchAgent 工具。带证据的工具直接返回结构化 ToolResult，其他工具只返回文本内容。
     */
    private ToolResult dispatchTool(IScientificResearchTools tools, String toolName, String inputRaw) {
        com.alibaba.fastjson.JSONObject params = smartParse(inputRaw);
        return switch (toolName) {
            case "searchLibrary" -> {
                String query = params.getString("query");
                Long paperId = params.getLong("paperId");
                Double threshold = params.getDouble("threshold");
                if (query == null && !params.isEmpty()) {
                    query = inputRaw;
                }
                yield tools.searchLibraryWithEvidence(query, paperId, threshold);
            }
            case "getPaperOutline" -> ToolResult.ok(tools.getPaperOutline(params.getLong("paperId")));
            case "readSection" -> tools.readSectionWithEvidence(params.getString("sectionUuid"));
            case "lookupReference" -> {
                Long paperId = params.getLong("paperId");
                String refIndex = params.getString("refIndex");
                if (paperId == null || refIndex == null) {
                    yield ToolResult.ok("Error: Missing parameters. Required: paperId, refIndex.");
                }
                yield tools.lookupReferenceWithEvidence(paperId, refIndex);
            }
            case "checkPaperRelations" -> {
                Long paperId = params.getLong("paperId");
                if (paperId == null) {
                    yield ToolResult.ok("Error: Missing parameters. Required: paperId.");
                }
                yield ToolResult.ok(tools.checkPaperRelations(paperId));
            }
            case "getTopCitedReferences" -> {
                Integer limit = params.getInteger("limit");
                yield ToolResult.ok(tools.getTopCitedReferences(limit == null ? 5 : limit));
            }
            case "findPapers" -> {
                String query = params.getString("query");
                Double threshold = params.getDouble("threshold");
                if (query == null && !params.isEmpty()) {
                    query = inputRaw;
                }
                if (query == null) {
                    yield ToolResult.ok("Error: Missing 'query' parameter.");
                }
                yield ToolResult.ok(tools.findPapers(query, threshold));
            }
            default -> ToolResult.ok("Error: Unknown tool '" + toolName + "'.");
        };
    }

    /** 兼容 Agent 有时直接传入的纯文本参数。 */
    private com.alibaba.fastjson.JSONObject smartParse(String input) {
        if (input == null || input.isBlank()) {
            return new com.alibaba.fastjson.JSONObject();
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("{")) {
            try {
                return com.alibaba.fastjson.JSON.parseObject(trimmed);
            } catch (Exception ignored) {
                // 继续按纯文本参数处理。
            }
        }
        com.alibaba.fastjson.JSONObject fallback = new com.alibaba.fastjson.JSONObject();
        fallback.put("query", trimmed);
        fallback.put("sectionUuid", trimmed);
        fallback.put("paperId", trimmed.replaceAll("\\D", ""));
        return fallback;
    }

    /** 将 LangChain4j ChatMemory 适配为框架内部记忆接口。 */
    private static class AgentMemoryBridge implements AgentMemory {
        private final ChatMemory chatMemory;

        AgentMemoryBridge(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
        }

        @Override
        public void addMessage(ChatMessage message) {
            chatMemory.add(message);
        }

        @Override
        public List<ChatMessage> getMessages() {
            return chatMemory.messages();
        }

        @Override
        public void clear() {
            chatMemory.clear();
        }

        @Override
        public String getSessionId() {
            return (String) chatMemory.id();
        }

        @Override
        public ChatMemory asChatMemory() {
            return chatMemory;
        }
    }
}
