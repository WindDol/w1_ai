package cn.winddol.ai.domain.paper.adapter.repository;

import cn.winddol.ai.domain.paper.model.aggregate.SearchResultDTO;
import cn.winddol.ai.domain.paper.model.entity.PaperEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionPO;
import cn.winddol.ai.domain.paper.model.entity.SymbolEntity;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;

import java.util.List;

public interface IPaperRepository {
    void saveFullPaper(String title, List<SectionPO> sectionPOs);

    List<SectionEntity> getSectionByUuid(List<String> uuid);

    PaperEntity getPaperById(Long id);

    void saveEnrichmentData(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection);

    List<SearchResultDTO.SectionDTO> searchSectionsByVector(String vector, int i);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByVector(String vector, int topK);

    PaperEntity selectPaperById(Long paperId);

    SectionEntity selectSectionById(String sectionUuid);

    List<SymbolEntity> selectSymbolsByUuids(String sectionUuid);

    SectionEntity getSectionSibling(Long paperId, int offset, int i);

    List<SearchResultDTO.SymbolDTO> searchSymbolsByKeyword(String query);
}
