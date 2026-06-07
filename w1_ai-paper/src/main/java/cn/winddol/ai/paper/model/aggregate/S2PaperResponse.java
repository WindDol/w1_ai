package cn.winddol.ai.paper.model.aggregate;

import cn.winddol.ai.paper.model.entity.S2PaperData;
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

}
