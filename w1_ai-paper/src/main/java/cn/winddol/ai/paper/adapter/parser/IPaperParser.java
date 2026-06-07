package cn.winddol.ai.paper.adapter.parser;

import cn.winddol.ai.paper.model.entity.SectionPO;

import java.util.List;

public interface IPaperParser {

    String parsePdfToMarkdown(String absolutePath);

    List<SectionPO> parse(String markdown);

    String extractTitle(String markdown);

    String extractAbstract(List<SectionPO> pos);
}
