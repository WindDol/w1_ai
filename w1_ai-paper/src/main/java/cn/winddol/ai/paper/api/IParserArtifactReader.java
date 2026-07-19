package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.retrieval.SourceTextBlock;

import java.util.List;

public interface IParserArtifactReader {

    /**
     * 从解析产物中读取带页码的文本块；解析产物不含页码信息时允许返回空列表。
     */
    List<SourceTextBlock> readTextBlocks(String parserArtifactPath);
}
