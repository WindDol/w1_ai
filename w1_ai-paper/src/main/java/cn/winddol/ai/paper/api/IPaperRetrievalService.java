package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalQuery;
import cn.winddol.ai.paper.domain.retrieval.PaperRetrievalResult;

public interface IPaperRetrievalService {

    /**
     * 从论文库中检索经过排序且可追溯的证据。
     */
    PaperRetrievalResult retrieve(PaperRetrievalQuery query);
}
