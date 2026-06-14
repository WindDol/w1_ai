package cn.winddol.ai.paper.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class S2PaperData {
    private String paperId;
    private String title;
    @JsonProperty("abstract") // 处理 Java 关键字冲突
    private String abstractText;
    private int citationCount;
    private int year;
    private List<S2Author> authors;
}
