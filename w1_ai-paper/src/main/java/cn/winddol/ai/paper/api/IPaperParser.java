package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.SectionPO;
import cn.winddol.ai.paper.domain.ingest.PdfParseResult;

import java.util.List;

public interface IPaperParser {

    String parserType();

    String parserVersion();

    PdfParseResult parsePdf(String absolutePath);

    List<SectionPO> parse(String markdown);

    String extractTitle(String markdown);

    String extractAbstract(List<SectionPO> pos);
}
