package cn.winddol.ai.infrastructure.embedding;

import cn.winddol.ai.infrastructure.dao.SymbolMapper;
import cn.winddol.ai.infrastructure.dao.po.Symbol;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class KnowledgeRetriever {

    @Resource
    private SymbolMapper symbolMapper;
    @Resource
    private EmbeddingModel embeddingModel;

    public List<Symbol> searchSymbols(Long paperId, String query) {
        // 1. 尝试精确匹配 (Keyword Search)
        List<Symbol> exactMatches = symbolMapper.selectList(
                new LambdaQueryWrapper<Symbol>()
                        .eq(Symbol::getPaperId, paperId)
                        .eq(Symbol::getSymbol, query)
        );

        if (!exactMatches.isEmpty()) {
            return exactMatches; // 如果找到了精确的，直接返回
        }

        // 2. 如果没找到，进行语义搜索 (Vector Search)
        float[] queryVector = embeddingModel.embed(query).content().vector();
        String vectorStr = Arrays.toString(queryVector); // 转成字符串传给 SQL

        return symbolMapper.searchByVector(paperId, vectorStr, 5);
    }
}