package cn.winddol.ai.domain.paperTools.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class RefMetadata {
    private String title;       // 可能为空
    private List<String> authorSurnames;  // 用于验证
    private Integer year;       // 用于验证 (最关键)
    private String searchString; // 专门构造给 API 搜的字符串
}