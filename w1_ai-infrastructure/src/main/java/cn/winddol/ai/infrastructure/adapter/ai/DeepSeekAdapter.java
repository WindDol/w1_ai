package cn.winddol.ai.infrastructure.adapter.ai;

import cn.winddol.ai.domain.agent.adapter.repository.IAiAdapter;
import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.RefMetadata;
import cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition;
import com.alibaba.fastjson.JSON;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class DeepSeekAdapter implements ISymbolExtractor, IAiAdapter {

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

            if ( jsonStr.trim().isEmpty()) {
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
        ### Role
        You are an advanced Bibliometric Parsing AI. Your task is to structure raw citation text into precise metadata and generate an optimized search query.

        ### Input Data
        Raw Text: "%s"

        ### Extraction Tasks
        1. **Year**: Identify the 4-digit publication year (e.g., 2023).
        2. **Authors**: Extract all author surnames.
           - Remove initials (e.g., "J. Smith" -> "Smith").
           - Keep compound surnames intact (e.g., "Van der Waals").
        3. **Title**: Identify the full title of the paper.
        4. **Journal/Venue**: Identify the journal, conference abbreviation, or publisher (e.g., "Nature", "CVPR", "arXiv", "Phys. Rev. B").

        ### Search String Construction (CRITICAL)
        Generate a `searchString` optimized for academic search engines (Google Scholar).
        **Pattern**: `[First Author Surname] [Year] [Journal/Venue] [Title]`
        - **Requirement**: You MUST include the **Journal/Venue** if visible in the text.
        - **Formatting**: Remove all non-alphanumeric punctuation (commas, brackets) from the search string to ensure broad matching.

        ### Output Format
        Return STRICT JSON only (no markdown code blocks, no explanation):
        {
          "year": 2024,
          "authorSurnames": ["Smith", "Doe"],
          "title": "The theory of everything",
          "journal": "Nature Physics",
          "searchString": "Smith 2024 Nature Physics The theory of everything"
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
        A concise summary (approx. 100-200 words). Start with: "Based on the context, this reference appears to propose/discuss..."
        """.formatted(refIndex, refIndex, joinedSnippets);

        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            log.error("Context summary failed", e);
            return null;
        }
    }

    @Override
    public String fuseSyntheticAbstracts(String desc1, String desc2) {
        String prompt = """
            You are a knowledge integration engine. You are given two separate contextual summaries of the same scientific paper, derived from different citing sources.
            
            [Summary 1]
            %s
            
            [Summary 2]
            %s
            
            [Task]
            Synthesize these into a single, cohesive, and more comprehensive summary.
            - Eliminate redundancies.
            - Retain specific details about methods, findings, or applications from both sources.
            - Maintain a professional, objective tone.
            - Start with: "Based on multiple contexts, this reference discusses..."
            
            [Output]
            A single paragraph summary (max 250 words).
            """.formatted(desc1, desc2);

        return chatLanguageModel.generate(prompt);
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

    @Override
    public String rewriteQueryIfNecessary(String currentQuestion, List<ChatMessage> history) {
        String rewritePrompt = """
        You are an expert research assistant.
        Your task:
        1. Analyze the conversation history and the follow-up question.
        2. Resolve pronouns (it, this, that, etc.) based on history.
        3. Translate the resolved question into a concise, professional academic English search query.
        
        [Rules]
        - Output ONLY the English rewritten question.
        - Do not provide any explanations.
        
        [History]
        %s
        
        [Follow-up Question]
        %s
        
        [English Standalone Question]
        """.formatted(formatHistory(history), currentQuestion);

        // 调用 LLM (建议用 DeepSeek-V3，翻译和逻辑都极强)
        String response = chatLanguageModel.generate(rewritePrompt);
        return response.trim();
    }

    private String formatHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "No history.";
        return history.stream()
                .map(m -> (m instanceof UserMessage ? "User: " : "AI: ") + m.text())
                .collect(Collectors.joining("\n"));
    }
}
