package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.*;

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

    List<SectionEntity> getSectionByUuid(List<String> uuid);

    PaperEntity getPaperById(Long id);

    List<ReferenceItem> selectReferences();

    void updateReferences(ReferenceItem ref);

    List<SectionReferenceLinkEntity> selectReferenceLinks(Long paperId, String refId);

    GlobalReferenceEntity selectGlobalReferenceByS2Id(String s2Id);

    GlobalReferenceEntity selectGlobalReferenceByFingerprint(String fingerprint);

    Long insertGlobalReference(GlobalReferenceEntity globalNode);

    Long updateGlobalReference(GlobalReferenceEntity globalNode);

    Long findPaperIdByFingerprint(String fingerprint);

    List<ReferenceItem> selectPendingReferencesByPaperId(Long paperId);

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

    void saveEnrichmentData(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection, Map<String, Set<String>> citationLinks);
}
