package cn.winddol.ai.domain.paper.model.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class SectionPO {
    public String uuid;         // 数据库主键
    public String header;       // 标题 (如 "II. Background")
    public int level;           // 标题级别 (1=#, 2=##)
    public StringBuilder contentBuffer = new StringBuilder(); // 内容缓冲
    public String parentId;

    public SectionPO(String header, int level, String parentId) {
        this.uuid = UUID.randomUUID().toString();
        this.header = header;
        this.level = level;
        this.parentId = parentId;
    }

    public String getContent() {
        return contentBuffer.toString().trim();
    }
}
