package cn.winddol.ai.paper.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperVO {
    private Long id;
    private String title;
    private String status;
    private String fingerprint;
    private LocalDateTime createdAt;
}
