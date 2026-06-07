package cn.winddol.ai.paper.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class S2Author {
    private String name; // S2 API 返回的全名，如 "J. A. Acebrón"
}
