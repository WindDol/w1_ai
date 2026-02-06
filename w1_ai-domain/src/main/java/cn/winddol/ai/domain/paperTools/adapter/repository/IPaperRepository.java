package cn.winddol.ai.domain.paperTools.adapter.repository;

import cn.winddol.ai.domain.agent.model.entity.KnowledgeRelationEntity;
import cn.winddol.ai.domain.paperTools.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paperTools.model.entity.*;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;
import cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IPaperRepository {
    Long saveFullPaper(String title, List<SectionPO> sectionPOs, String fingerprint, String abstractText);

    List<SectionEntity> getSectionByUuid(List<String> uuid);

    PaperEntity getPaperById(Long id);

    void saveEnrichmentData(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection, Map<String, Set<String>> inTextCitationLinks);

    List<SearchResultDTO.SectionDTO> searchSectionsByVector(Long paperId, String vector, int i);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByVector(Long paperId, String vector, int topK);

    PaperEntity selectPaperById(Long paperId);

    SectionEntity selectSectionById(String sectionUuid);

    List<SymbolEntity> selectSymbolsByUuids(String sectionUuid);

    SectionEntity getSectionSibling(Long paperId, int offset, int i);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByKeyword(String query, Long paperId);

    List<ReferenceItem> selectReferences();

    void updateReferences(ReferenceItem ref);

    List<SectionReferenceLinkEntity> selectReferenceLinks(Long paperId, String refId);
    List<SectionReferenceLinkEntity> selectLinksBySectionId(String sectionId);
    ReferenceItem selectReferenceByIndex(Long paperId, String refIndex);

    List<SearchResultDTO.ReferenceDTO> searchReferencesByVector(Long paperId, String vector, int topK);

    GlobalReferenceEntity selectGlobalReferenceByS2Id(String s2Id);

    GlobalReferenceEntity selectGlobalReferenceByFingerprint(String fingerprint);

    Long insertGlobalReference(GlobalReferenceEntity globalNode);

    Long updateGlobalReference(GlobalReferenceEntity globalNode);

    Long findPaperIdByFingerprint(String fingerprint);

    ReferenceItem lookupReference(Long paperId, String refIndex);

    List<ReferenceItem> selectPendingReferencesByPaperId(Long paperId);

    void updateStatus(Long paperId, String status);

    void updateStatusWithError(Long paperId, String status, String errorMessage);
    List<KnowledgeRelationEntity> findRelationsByPaperId(Long paperId);
}
