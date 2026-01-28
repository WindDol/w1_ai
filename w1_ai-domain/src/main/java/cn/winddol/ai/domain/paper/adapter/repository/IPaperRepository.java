package cn.winddol.ai.domain.paper.adapter.repository;

import cn.winddol.ai.domain.paper.model.entity.PaperEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionPO;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;

import java.util.List;

public interface IPaperRepository {
    void saveFullPaper(String title, List<SectionPO> sectionPOs);

    List<SectionEntity> getSectionByUuid(List<String> uuid);

    PaperEntity getPaperById(Long id);

    void saveEnrichmentData(Long paperId, List<SymbolDefinition> finalSymbols, SectionEntity refSection);

}
