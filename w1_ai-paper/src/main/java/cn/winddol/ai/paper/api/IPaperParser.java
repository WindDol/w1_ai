package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.SectionPO;

import java.util.List;

public interface IPaperParser {

    String parsePdfToMarkdown(String absolutePath);

    List<SectionPO> parse(String markdown);

    String extractTitle(String markdown);

    String extractAbstract(List<SectionPO> pos);
}
