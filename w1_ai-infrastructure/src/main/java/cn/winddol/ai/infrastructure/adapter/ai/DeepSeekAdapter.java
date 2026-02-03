package cn.winddol.ai.infrastructure.adapter.ai;

import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.RefMetadata;
import cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class DeepSeekAdapter implements ISymbolExtractor {

    @Resource
    private ChatLanguageModel chatLanguageModel;


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
            String response = chatLanguageModel.generate(prompt);

            // 防止模型有时会无视 "No Markdown" 的指令
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

    @Override
    public RefMetadata extractRefMetadata(String rawText) {
        String prompt = """
            You are a bibliometric expert. Analyze this citation reference.
            Raw Text: "%s"
            
            Tasks:
            1. Identify the **Publication Year**.
            2. Identify **ALL Author Surnames** visible in the text.
               - Ignore initials (e.g., "J. Smith" -> "Smith").
               - If "et al." appears, just list the visible authors.
            3. Identify the **Title**.
            4. Construct a Search Query.
            
            Return JSON ONLY:
            {
              "year": 1995,
              "authorSurnames": ["Smith", "Doe", "Johnson"],  // List of strings
              "title": "...",
              "searchString": "..."
            }
            """.formatted(rawText);

        try {
            String json = cleanJson(chatLanguageModel.generate(prompt));
            return JSON.parseObject(json, RefMetadata.class);
        } catch (Exception e) {
            log.error("LLM extraction failed", e);
            return null;
        }
    }

    @Override
    public String summarizeReferenceContext(String refIndex, List<String> snippets) {
        String joinedSnippets = String.join("\n---\n", snippets);

        String prompt = """
        You are a scientific researcher. 
        The following text snippets are from a main paper that cites Reference [%s].
        
        [Goal]
        Infer and summarize the key contribution, method, or finding of Reference [%s] based ONLY on how it is described in these snippets.
        
        [Snippets]
        %s
        
        [Output]
        A concise summary (approx. 50-100 words). Start with: "Based on the context, this reference appears to propose/discuss..."
        """.formatted(refIndex, refIndex, joinedSnippets);

        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            log.error("Context summary failed", e);
            return null;
        }
    }


    private String cleanTitle(String response) {
        if (response == null) return null;
        String clean = response.trim();
        // 去掉可能的首尾引号
        if (clean.startsWith("\"") && clean.endsWith("\"")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        return clean;
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
