package cn.winddol.ai.config;

import cn.winddol.ai.domain.agent.adapter.event.NotificationService;
import cn.winddol.ai.framework.event.AgentEvent;
import cn.winddol.ai.framework.memory.AgentMemory;
import cn.winddol.ai.framework.memory.AgentMemoryFactory;
import cn.winddol.ai.framework.tool.Tool;
import cn.winddol.ai.framework.tool.ToolProvider;
import cn.winddol.ai.infrastructure.adapter.ai.DeepSeekAdapter;
import cn.winddol.ai.paper.api.IScientificResearchTools;
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

    /**
     * 将 AgentEvent 桥接到旧的 SSE NotificationService，
     * 保持原有 SSE 推送行为不变。
     */
    @Bean
    public Consumer<AgentEvent> agentEventSink(NotificationService notificationService) {
        return event -> notificationService.send(event.getSessionId(), event);
    }

    /**
     * 基于 Redis ChatMemoryStore 创建 AgentMemoryFactory。
     */
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

    /**
     * 把 ScientificResearchTools 包装为框架的 ToolProvider，
     * ResearchAgent 通过 ToolProvider 调用工具，不再直接依赖具体类。
     */
    @Bean
    public ToolProvider researchToolProvider(IScientificResearchTools tools) {
        return new ToolProvider() {
            @Override
            public List<Tool> getTools() {
                return Collections.emptyList(); // 暂不需要工具列表
            }

            @Override
            public cn.winddol.ai.shared.model.tool.ToolResult executeTool(String toolName, String inputJson) {
                return cn.winddol.ai.shared.model.tool.ToolResult.ok(
                        dispatchTool(tools, toolName, inputJson)
                );
            }
        };
    }

    private String dispatchTool(IScientificResearchTools tools, String toolName, String inputRaw) {
        com.alibaba.fastjson.JSONObject params = smartParse(inputRaw);
        switch (toolName) {
            case "searchLibrary":
                String query = params.getString("query");
                Long pId = params.getLong("paperId");
                Double threshold = params.getDouble("threshold");
                if (query == null && !params.isEmpty()) query = inputRaw;
                return tools.searchLibrary(query, pId, threshold);
            case "getPaperOutline":
                return tools.getPaperOutline(params.getLong("paperId"));
            case "readSection":
                return tools.readSection(params.getString("sectionUuid"));
            case "lookupReference":
                Long refPaperId = params.getLong("paperId");
                String refIndex = params.getString("refIndex");
                if (refPaperId == null || refIndex == null)
                    return "Error: Missing parameters. Required: paperId, refIndex.";
                return tools.lookupReference(refPaperId, refIndex);
            case "checkPaperRelations":
                Long relationPaperId = params.getLong("paperId");
                if (relationPaperId == null)
                    return "Error: Missing parameters. Required: paperId.";
                return tools.checkPaperRelations(relationPaperId);
            case "getTopCitedReferences":
                Integer limit = params.getInteger("limit");
                if (limit == null) limit = 5;
                return tools.getTopCitedReferences(limit);
            case "findPapers":
                String paperQuery = params.getString("query");
                Double paperThreshold = params.getDouble("threshold");
                if (paperQuery == null && !params.isEmpty()) paperQuery = inputRaw;
                if (paperQuery == null) return "Error: Missing 'query' parameter.";
                return tools.findPapers(paperQuery, paperThreshold);
            default:
                return "Error: Unknown tool '" + toolName + "'.";
        }
    }

    private com.alibaba.fastjson.JSONObject smartParse(String input) {
        if (input == null || input.isBlank()) return new com.alibaba.fastjson.JSONObject();
        String trimmed = input.trim();
        if (trimmed.startsWith("{")) {
            try {
                return com.alibaba.fastjson.JSON.parseObject(trimmed);
            } catch (Exception ignored) {}
        }
        com.alibaba.fastjson.JSONObject fallback = new com.alibaba.fastjson.JSONObject();
        fallback.put("query", trimmed);
        fallback.put("sectionUuid", trimmed);
        fallback.put("paperId", trimmed.replaceAll("\\D", ""));
        return fallback;
    }

    /**
     * AgentMemory 适配器，桥接 LangChain4j 的 ChatMemory。
     */
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

    /**
     * 桥接 Bean：让 DeepSeekAdapter 满足 librarian 模块的 IAiAdapter 接口。
     * 两个模块的 AgentPaperEntity/PaperAuditResult 类型不同但字段相同，手动映射。
     */
    @org.springframework.context.annotation.Bean
    public cn.winddol.ai.agent.librarian.api.IAiAdapter librarianAiAdapter(DeepSeekAdapter adapter) {
        return new cn.winddol.ai.agent.librarian.api.IAiAdapter() {
            @Override
            public String rewriteQueryIfNecessary(String question,
                    java.util.List<dev.langchain4j.data.message.ChatMessage> history) {
                return adapter.rewriteQueryIfNecessary(question, history);
            }

            @Override
            public cn.winddol.ai.agent.librarian.domain.PaperAuditResult analyzeRelation(
                    cn.winddol.ai.agent.librarian.domain.AgentPaperEntity newPaper,
                    cn.winddol.ai.agent.librarian.domain.AgentPaperEntity oldPaper,
                    String newAbstract) {
                // 类型映射：librarian 类型 → 旧 domain 类型
                cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity oldNew =
                        cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity.builder()
                                .id(newPaper.getId()).title(newPaper.getTitle())
                                .abstractText(newPaper.getAbstractText()).years(newPaper.getYears())
                                .build();
                cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity oldOld =
                        cn.winddol.ai.domain.agent.model.entity.AgentPaperEntity.builder()
                                .id(oldPaper.getId()).title(oldPaper.getTitle())
                                .abstractText(oldPaper.getAbstractText()).years(oldPaper.getYears())
                                .build();
                cn.winddol.ai.domain.agent.model.entity.PaperAuditResult oldResult =
                        adapter.analyzeRelation(oldNew, oldOld, newAbstract);
                cn.winddol.ai.agent.librarian.domain.PaperAuditResult result =
                        new cn.winddol.ai.agent.librarian.domain.PaperAuditResult();
                result.setType(oldResult.getType());
                result.setReason(oldResult.getReason());
                return result;
            }
        };
    }
}
