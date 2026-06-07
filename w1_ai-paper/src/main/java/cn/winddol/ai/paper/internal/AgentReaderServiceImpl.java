package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.model.entity.*;
import cn.winddol.ai.paper.model.valobj.ReferenceItem;
import cn.winddol.ai.paper.api.IPaperRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class AgentReaderServiceImpl {

    private final IPaperRepository repository;

    public AgentReaderServiceImpl(IPaperRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings("unchecked")
    public List<OutlineNode> getPaperOutline(Long paperId) {
        PaperEntity paper = repository.selectPaperById(paperId);
        if (paper == null || paper.getOutline() == null) {
            return List.of();
        }
        try {
            return (List<OutlineNode>) paper.getOutline();
        } catch (ClassCastException e) {
            return List.of();
        }
    }

    public String readSectionWithContext(String sectionUuid) {
        SectionEntity section = repository.selectSectionById(sectionUuid);
        if (section == null) return "Section not found: " + sectionUuid;

        StringBuilder sb = new StringBuilder();
        sb.append("### Current Section Content\n");
        sb.append(section.getContent()).append("\n\n");

        List<SymbolEntity> symbols = repository.selectSymbolsByUuids(sectionUuid);
        if (symbols != null && !symbols.isEmpty()) {
            sb.append("### Local Symbols\n");
            for (SymbolEntity s : symbols) {
                sb.append(String.format("- **%s**: %s", s.getSymbol(), s.getDescription()));
                if (s.getDefinitionFormula() != null) {
                    sb.append(String.format(" [Def: $%s$]", s.getDefinitionFormula()));
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public String lookupReference(Long paperId, String refIndex) {
        ReferenceItem ref = repository.lookupReference(paperId, refIndex);
        if (ref == null) {
            return "Reference [" + refIndex + "] not found in Paper " + paperId;
        }
        return String.format("**[%s] %s**\n\nAbstract: %s",
                ref.getRefId(), ref.getTitle(), ref.getPaperAbstract());
    }

    public String checkPaperRelations(Long paperId) {
        var relations = repository.findRelationsByPaperId(paperId);
        if (relations == null || relations.isEmpty()) {
            return "No inter-paper relations found.";
        }
        StringBuilder sb = new StringBuilder("### Inter-paper Relations\n");
        for (var rel : relations) {
            sb.append(String.format("- **%s** Paper[%d]: %s\n", rel.getType(), rel.getRelatedId(), rel.getDescription()));
        }
        return sb.toString();
    }
}
