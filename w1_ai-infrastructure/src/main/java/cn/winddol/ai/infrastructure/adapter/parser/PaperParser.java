package cn.winddol.ai.infrastructure.adapter.parser;

import cn.winddol.ai.paper.api.IPaperParser;
import cn.winddol.ai.paper.domain.SectionPO;
import cn.winddol.ai.paper.domain.ingest.PdfParseResult;
import cn.winddol.ai.infrastructure.parser.MarkdownParser;
import cn.winddol.ai.infrastructure.parser.PythonParserAdapter;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaperParser implements IPaperParser {
    @Resource
    private PythonParserAdapter pythonParser;

    @Resource
    private MarkdownParser parser;

    @Value("${python.parser-version:unknown}")
    private String parserVersion;

    @Override
    public String parserType() {
        return pythonParser.parserType();
    }

    @Override
    public String parserVersion() {
        return parserVersion;
    }

    @Override
    public PdfParseResult parsePdf(String absolutePath) {
        PythonParserAdapter.ParserOutput output = pythonParser.parsePdfWithArtifacts(absolutePath);
        return new PdfParseResult(output.markdown(), output.parserArtifactPath());
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
