package cn.winddol.ai.paper.api;

import cn.winddol.ai.domain.agent.model.entity.KnowledgeRelationEntity;
import cn.winddol.ai.domain.paperTools.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paperTools.model.entity.*;
import cn.winddol.ai.domain.paperTools.model.valobj.PaperVO;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IPaperRepository {

    Long saveFullPaper(String title, List<SectionPO> sectionPOs, String fingerprint, String abstractText, Integer year);

    void updateStatus(Long paperId, String status);

    void updateStatusWithError(Long paperId, String status, String errorMessage);

    List<PaperVO> listAllPapers();

    PaperEntity getPaperDetailsById(Long paperId);

    List<SymbolEntity> findByPaperId(Long paperId);

    List<KnowledgeRelationEntity> findRelationsByPaperId(Long paperId);

    List<ReferenceItem> selectReferencesByPaperId(Long paperId);

    List<SearchResultDTO.SectionDTO> searchSectionsByVector(Long paperId, String vector, int topK);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByKeyword(String query, Long paperId);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByVector(Long paperId, String vector, int topK);

    List<SearchResultDTO.ReferenceDTO> searchReferencesByVector(Long paperId, String vector, int topK);

    PaperEntity selectPaperById(Long paperId);

    SectionEntity selectSectionById(String sectionUuid);

    List<SymbolEntity> selectSymbolsByUuids(String sectionUuid);

    List<SectionReferenceLinkEntity> selectLinksBySectionId(String sectionId);

    ReferenceItem selectReferenceByIndex(Long paperId, String refIndex);

    ReferenceItem lookupReference(Long paperId, String refIndex);

    SectionEntity getSectionSibling(Long paperId, int currentIdx, int offset);

    List<PaperEntity> searchPapers(String query, Double threshold);

    List<GlobalReferenceEntity> getTopFrequentReferences(Integer limit);

    void saveEnrichmentData(Long paperId, List<cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition> finalSymbols, SectionEntity refSection, Map<String, Set<String>> citationLinks);
}
