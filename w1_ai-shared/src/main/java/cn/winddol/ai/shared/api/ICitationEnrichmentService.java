package cn.winddol.ai.shared.api;

public interface ICitationEnrichmentService {
    void enrichPaperReferences(Long paperId);
    void enrichReferences();
}
