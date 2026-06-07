package cn.winddol.ai.paper.adapter.external;

import cn.winddol.ai.paper.model.entity.S2PaperData;

public interface ISemanticScholar {
    S2PaperData searchPaper(String rawReference);
}
