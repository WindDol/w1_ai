package cn.winddol.ai.domain.paperTools.service;

public interface IScientificResearchTools {
    String searchLibrary(String query, Long paperId, Double threshold);
    String getPaperOutline(Long paperId);
    String readSection(String sectionUuid);
    String lookupReference(Long paperId, String refIndex);
    String checkPaperRelations(Long paperId);
    String findPapers(String query, Double threshold);
    String getTopCitedReferences(Integer limit);

}
