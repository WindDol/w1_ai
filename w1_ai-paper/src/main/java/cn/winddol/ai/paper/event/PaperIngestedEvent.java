package cn.winddol.ai.paper.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaperIngestedEvent {

    private Long paperId;
    private String title;
}
