package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefMetadata {
    private String title;
    private List<String> authorSurnames;
    private Integer year;
    private String searchString;
}
