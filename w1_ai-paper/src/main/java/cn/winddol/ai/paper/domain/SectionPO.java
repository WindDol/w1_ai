package cn.winddol.ai.paper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionPO {
    private String uuid;
    private String header;
    private int level;
    private StringBuilder contentBuffer;
    private String parentId;

    public SectionPO(String header, int level, String parentId) {
        this.uuid = UUID.randomUUID().toString();
        this.header = header;
        this.level = level;
        this.parentId = parentId;
        this.contentBuffer = new StringBuilder();
    }

    public String getContent() {
        return contentBuffer == null ? "" : contentBuffer.toString().trim();
    }
}
