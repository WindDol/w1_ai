package cn.winddol.ai.infrastructure.embedding;

import cn.winddol.ai.infrastructure.dao.SectionMapper;
import cn.winddol.ai.infrastructure.dao.SymbolMapper;
import cn.winddol.ai.infrastructure.dao.po.Section;
import cn.winddol.ai.infrastructure.dao.po.Symbol;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmbeddingProcessor {
    @Resource
    private EmbeddingModel embeddingModel;
    @Resource
    private SymbolMapper symbolMapper;
    @Resource
    private SectionMapper sectionMapper;

    public void embedSymbols() {
        // 1. 查询所有 embedding 为空的符号
        List<Symbol> symbols = symbolMapper.selectList(
                new LambdaQueryWrapper<Symbol>().isNull(Symbol::getEmbedding)
        );

        for (Symbol s : symbols) {
            // 2. 构造语义文本： 符号 + 描述 + LaTeX
            StringBuilder textBuilder = new StringBuilder();

            // 基础信息
            textBuilder.append("Symbol: ").append(s.getSymbol());
            textBuilder.append("; Meaning: ").append(s.getDescription());
            textBuilder.append("; LaTeX Representation: ").append(s.getLatex());

            if (s.getDefinitionFormula() != null) {
                textBuilder.append("; Definition Formula: ").append(s.getDefinitionFormula());
            }

            String textToEmbed = textBuilder.toString();
            Response<Embedding> response = embeddingModel.embed(textToEmbed);
            float[] vector = response.content().vector(); // LangChain4j 返回的是 float[]

            // 4. 更新数据库
            s.setEmbedding(vector);
            symbolMapper.updateById(s);
            log.info("Embedded symbol: {}", s.getSymbol());
        }
    }

    /**
     * 为章节生成向量 (只 Embed 标题和前 200 字，省钱且精准)
     */
    public void embedSections() {
        List<Section> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<Section>().isNull(Section::getEmbedding)
        );

        for (Section sec : sections) {
            // 策略：Header + 少量 Content (避免噪音)
            String preview = sec.getContent().length() > 300 ?
                    sec.getContent().substring(0, 300) : sec.getContent();
            String text = "Section: " + sec.getHeader() + "\nContent: " + preview;

            float[] vector = embeddingModel.embed(text).content().vector();

            sec.setEmbedding(vector);
            sectionMapper.updateById(sec);
        }
    }
}
