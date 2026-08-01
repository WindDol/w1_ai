package cn.winddol.ai.paper.api;

import cn.winddol.ai.shared.model.tool.ToolResult;

public interface IScientificResearchTools {

    String searchLibrary(String query, Long paperId, Double threshold);

    ToolResult searchLibraryWithEvidence(String query, Long paperId, Double threshold);

    String getPaperOutline(Long paperId);

    String readSection(String sectionUuid);

    ToolResult readSectionWithEvidence(String sectionUuid);

    String lookupReference(Long paperId, String refIndex);

    ToolResult lookupReferenceWithEvidence(Long paperId, String refIndex);

    String checkPaperRelations(Long paperId);

    String findPapers(String query, Double threshold);

    String getTopCitedReferences(Integer limit);
}
