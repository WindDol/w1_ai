package cn.winddol.ai.domain.paper.adapter.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class S2PaperResponse {
    private int total;
    private int offset;
    private List<S2PaperData> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class S2PaperData {
        private String paperId;
        private String title;
        @JsonProperty("abstract") // 处理 Java 关键字冲突
        private String abstractText;
        private int citationCount;
        private int year;
        private List<S2Author> authors;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class S2Author {
        private String name; // S2 API 返回的全名，如 "J. A. Acebrón"
    }
}
