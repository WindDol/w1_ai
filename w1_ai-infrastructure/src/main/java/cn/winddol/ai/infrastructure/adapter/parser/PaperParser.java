package cn.winddol.ai.infrastructure.adapter.parser;

import cn.winddol.ai.domain.paperTools.adapter.parser.IPaperParser;
import cn.winddol.ai.domain.paperTools.model.entity.SectionPO;
import cn.winddol.ai.infrastructure.parser.MarkdownParser;
import cn.winddol.ai.infrastructure.parser.PythonParserAdapter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaperParser implements IPaperParser {
    @Resource
    private PythonParserAdapter pythonParser;

    @Resource
    private MarkdownParser parser;

    @Override
    public String parsePdfToMarkdown(String absolutePath) {
        return pythonParser.parsePdfToMarkdown(absolutePath);
    }

    @Override
    public List<SectionPO> parse(String markdown) {
        return parser.parse(markdown);
    }

    @Override
    public String extractTitle(String markdown) {
        return parser.extractTitle(markdown);
    }

    @Override
    public String extractAbstract(List<SectionPO> pos) {
        return parser.extractAbstract(pos);
    }
}
