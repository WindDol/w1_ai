package cn.winddol.ai.agent.writer.api;

import java.util.List;

public interface IWriterAgent {
    String generateOutline(String topic, List<String> references);
    String writeSection(String outlineItem, String context);
    String formatPaper(String markdown);
}
