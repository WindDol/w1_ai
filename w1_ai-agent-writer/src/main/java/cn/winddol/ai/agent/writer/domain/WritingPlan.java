package cn.winddol.ai.agent.writer.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WritingPlan {
    private String title;
    private List<String> sectionOutlines;
    private List<String> references;
    private String targetJournal;
}
