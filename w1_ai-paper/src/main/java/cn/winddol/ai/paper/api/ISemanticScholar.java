package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.S2PaperData;

public interface ISemanticScholar {
    S2PaperData searchPaper(String rawReference);
}
