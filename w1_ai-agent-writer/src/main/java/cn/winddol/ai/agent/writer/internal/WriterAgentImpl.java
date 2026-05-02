package cn.winddol.ai.agent.writer.internal;

import cn.winddol.ai.agent.writer.api.IWriterAgent;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class WriterAgentImpl implements IWriterAgent {
    @Override
    public String generateOutline(String topic, List<String> references) {
        throw new UnsupportedOperationException("Writer agent not yet implemented");
    }

    @Override
    public String writeSection(String outlineItem, String context) {
        throw new UnsupportedOperationException("Writer agent not yet implemented");
    }

    @Override
    public String formatPaper(String markdown) {
        throw new UnsupportedOperationException("Writer agent not yet implemented");
    }
}
