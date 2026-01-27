package cn.winddol.ai.infrastructure.adapter.ai;

import cn.winddol.ai.domain.paper.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class DeepSeekAdapter implements ISymbolExtractor {
    @Value("${ai.llm.api-key}")
    private String apiKey;

    private ChatLanguageModel chatModel;

    @PostConstruct
    public void init(){
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.deepseek.com") // 关键点！
                .modelName("deepseek-chat")          // DeepSeek V3 模型名
                .temperature(0.0)                    // 设为 0 让提取更稳定
                .timeout(java.time.Duration.ofSeconds(60))
                .logRequests(true)                   // 调试时打印请求
                .logResponses(true)
                .build();
    }


    @Override
    public List<SymbolDefinition> extractFromSection(String paperTitle, String content) {
        // 1. 在 Prompt 中引入标题作为 Context
        // 2. 增加更明确的结构定界符
        // 3. 强化对 JSON 格式的约束
        String prompt = """
        You are a rigorous research assistant specializing in scientific literature.
        
        [Context]
        Paper Title: "%s"
        
        [Task]
        Extract all mathematical symbols and their specific meanings from the provided text snippet, AND their defining equations (if present).
        
        [Rules]
        1. Format: [{"symbol": "...", "latex": "...", "description": "...", "definition_formula": "..."}]
        2. "symbol": The variable itself (e.g., "z").
        3. "latex": The LaTeX code for the symbol (e.g., "z").
        4. "description": Physical or mathematical meaning.
        5. "definition_formula": If the text explicitly defines the symbol with an equation (e.g., "z = x + iy"), extract the right-hand side LaTeX. If not, return null.
        
        [Example]
        Text: "The order parameter z is defined as z = \\frac{1}{N}\\sum e^{i\\theta_j}."
        Output: [{"symbol": "z", "latex": "z", "description": "Order parameter", "definition_formula": "\\\\frac{1}{N}\\\\sum e^{i\\\\theta_j}"}]
        [Text to Analyze]
        ---
        %s
        ---
        """.formatted(paperTitle, content);

        try {
            String response = chatModel.generate(prompt);

            // 这里的 cleanJson 非常关键，因为模型有时会无视 "No Markdown" 的指令
            String jsonStr = cleanJson(response);

            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                return Collections.emptyList();
            }

            return JSON.parseArray(jsonStr, SymbolDefinition.class);

        } catch (Exception e) {
            // 建议增加日志记录，方便调试哪个片段导致了解析失败
            log.error("Failed to extract symbols from paper: {}", paperTitle, e);
            return Collections.emptyList();
        }
    }

    private String cleanJson(String response) {
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
}
