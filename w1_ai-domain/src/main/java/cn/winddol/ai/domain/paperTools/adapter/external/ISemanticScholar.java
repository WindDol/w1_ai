package cn.winddol.ai.domain.paperTools.adapter.external;

import cn.winddol.ai.domain.paperTools.model.entity.S2PaperData;

public interface ISemanticScholar {
    S2PaperData searchPaper(String rawReference);
}
