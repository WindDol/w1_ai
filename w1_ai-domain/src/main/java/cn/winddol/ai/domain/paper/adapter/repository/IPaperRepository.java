package cn.winddol.ai.domain.paper.adapter.repository;

import cn.winddol.ai.domain.paper.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paper.model.entity.*;
import cn.winddol.ai.domain.paper.model.valobj.ReferenceItem;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IPaperRepository {
    void saveFullPaper(String title, List<SectionPO> sectionPOs);

    List<SectionEntity> getSectionByUuid(List<String> uuid);

    PaperEntity getPaperById(Long id);

    void saveEnrichmentData(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection, Map<String, Set<String>> inTextCitationLinks);

    List<SearchResultDTO.SectionDTO> searchSectionsByVector(String vector, int i);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByVector(String vector, int topK);

    PaperEntity selectPaperById(Long paperId);

    SectionEntity selectSectionById(String sectionUuid);

    List<SymbolEntity> selectSymbolsByUuids(String sectionUuid);

    SectionEntity getSectionSibling(Long paperId, int offset, int i);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByKeyword(String query);

    List<ReferenceItem> selectReferences();

    void updateReferences(ReferenceItem ref);

    List<SectionReferenceLinkEntity> selectReferenceLinks(Long paperId, String refId);
}
