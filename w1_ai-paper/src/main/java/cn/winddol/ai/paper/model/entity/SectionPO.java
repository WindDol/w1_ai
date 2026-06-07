package cn.winddol.ai.paper.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor  // 必须有这个
@AllArgsConstructor // 建议配合使用
@Builder
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
