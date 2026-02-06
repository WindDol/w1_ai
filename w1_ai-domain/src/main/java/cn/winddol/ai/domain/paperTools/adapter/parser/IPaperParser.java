package cn.winddol.ai.domain.paperTools.adapter.parser;

import cn.winddol.ai.domain.paperTools.model.entity.SectionPO;

import java.util.List;

public interface IPaperParser {

    String parsePdfToMarkdown(String absolutePath);

    List<SectionPO> parse(String markdown);

    String extractTitle(String markdown);

    String extractAbstract(List<SectionPO> pos);
}
