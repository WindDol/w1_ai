package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.*;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class AgentReaderServiceImpl {

    private final IPaperRepository repository;

    public AgentReaderServiceImpl(IPaperRepository repository) {
        this.repository = repository;
    }

    public List<OutlineNode> getPaperOutline(Long paperId) {
        PaperEntity paper = repository.selectPaperById(paperId);
        return paper.getOutline();
    }

    public String checkPaperRelations(Long paperId) {
        List<PaperRelationDTO> relations = repository.findRelationsByPaperId(paperId);
        if (relations.isEmpty()) {
            return "The Librarian found no direct conflicts or specific relations with other papers in the library.";
        }

        StringBuilder sb = new StringBuilder("Inter-paper relations from the Librarian's audit:\n");
        for (PaperRelationDTO rel : relations) {
            String type = rel.getType();
            String otherPaperInfo = String.format("Paper [%d] (%s)", rel.getRelatedId(), rel.getRelatedTitle());
            String reason = rel.getDescription();

            if ("OUTGOING".equals(rel.getDirection())) {
                String action = getActivePhrasing(type);
                sb.append(String.format("- This paper **%s** %s.\n  *Reason: %s*\n",
                        action, otherPaperInfo, reason));
            } else {
                String passiveAction = getPassivePhrasing(type);
                sb.append(String.format("- This paper **%s** %s.\n  *Note: %s*\n",
                        passiveAction, otherPaperInfo, reason));
            }
        }
        return sb.toString();
    }

    public String readSectionWithContext(String sectionUuid) {
        SectionEntity section = repository.selectSectionById(sectionUuid);
        if (section == null) return "Error: Section not found.";
        StringBuilder sb = new StringBuilder();
        sb.append("### Navigation Path\n");
        sb.append(buildBreadcrumb(section)).append("\n\n");

        String symbolsUuid = section.getParentId();
        int safetyCounter = 0;
        while (symbolsUuid != null && safetyCounter < 5) {
            SectionEntity parent = repository.selectSectionById(symbolsUuid);
            symbolsUuid = parent.getParentId();
            if (symbolsUuid == null) {
                symbolsUuid = parent.getId();
                break;
            }
            safetyCounter++;
        }
        List<SymbolEntity> relatedSymbols = repository.selectSymbolsByUuids(symbolsUuid);

        if (!relatedSymbols.isEmpty()) {
            sb.append("### Mathematical Dictionary (Relevant to this section)\n");
            for (SymbolEntity sym : relatedSymbols) {
                sb.append(String.format("- **%s**: %s", sym.getSymbol(), sym.getDescription()));
                if (sym.getDefinitionFormula() != null) {
                    sb.append(" (Def: $").append(sym.getDefinitionFormula()).append("$)");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (section.getParentId() != null) {
            SectionEntity parent = repository.selectSectionById(section.getParentId());
            if (parent != null && parent.getContent() != null) {
                String parentIntro = parent.getContent().length() > 500 ?
                        parent.getContent().substring(0, 500) + "..." :
                        parent.getContent();
                if (!parentIntro.trim().isEmpty()) {
                    sb.append("### Parent Context (Background info from ").append(parent.getHeader()).append(")\n");
                    sb.append(parentIntro).append("\n\n");
                }
            }
        }

        sb.append("### Current Section Content\n");
        sb.append(section.getContent()).append("\n\n");
        injectCitationContext(sb, section);
        SectionEntity prev = getSibling(section, -1);
        SectionEntity next = getSibling(section, 1);

        sb.append("### Nearby Sections\n");
        if (prev != null) sb.append("- Previous: ").append(prev.getHeader()).append(" (ID: ").append(prev.getId()).append(")\n");
        if (next != null) sb.append("- Next: ").append(next.getHeader()).append(" (ID: ").append(next.getId()).append(")\n");

        return sb.toString();
    }

    private void injectCitationContext(StringBuilder sb, SectionEntity section) {
        List<SectionReferenceLinkEntity> links = repository.selectLinksBySectionId(section.getId());
        if (!links.isEmpty()) {
            sb.append("### External References Cited in This Section\n");
            sb.append("(Note: Use this context to understand references like [x] mentioned in the text.)\n\n");
            for (SectionReferenceLinkEntity link : links) {
                ReferenceItem ref = repository.selectReferenceByIndex(section.getPaperId(), link.getRefIndex());

                if (ref != null && ref.getPaperAbstract() != null) {
                    sb.append(String.format("- **[%s] %s**\n", ref.getRefId(), ref.getTitle()));
                    if (ref.getLinkedPaperId() != null) {
                        sb.append(String.format("  > [In-Library Full Text Available] Use `getPaperOutline(%d)` for details.\n", ref.getLinkedPaperId()));
                    }
                    String abs = ref.getPaperAbstract();
                    String shortAbs = abs.length() > 500 ? abs.substring(0, 500) + "..." : abs;

                    sb.append("  > Abstract/Summary: ").append(shortAbs).append("\n\n");
                }
            }
        }
    }

    private String buildBreadcrumb(SectionEntity section) {
        LinkedList<String> hierarchy = new LinkedList<>();
        hierarchy.add(section.getHeader());
        String currentParentId = section.getParentId();
        int safetyCounter = 0;
        while (currentParentId != null && safetyCounter < 5) {
            SectionEntity parent = repository.selectSectionById(currentParentId);
            if (parent == null) break;

            hierarchy.addFirst(parent.getHeader());
            currentParentId = parent.getParentId();
            safetyCounter++;
        }
        PaperEntity paper = repository.selectPaperById(section.getPaperId());
        if (paper != null) {
            hierarchy.addFirst("Paper: \"" + paper.getTitle() + "\"");
        }
        return String.join(" > ", hierarchy);
    }

    private SectionEntity getSibling(SectionEntity current, int offset) {
        return repository.getSectionSibling(current.getPaperId(), current.getIdx(), offset);
    }

    public String lookupReference(Long paperId, String refIndex) {
        ReferenceItem ref = repository.lookupReference(paperId, refIndex);

        if (ref == null) {
            return String.format("Reference [%s] not found in paper %d. Try searchLibrary if you know the topic.", refIndex, paperId);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Details for Reference [").append(ref.getRefId()).append("]\n");
        sb.append("- **Title**: ").append(ref.getTitle()).append("\n");

        String type = "CONTEXT".equalsIgnoreCase(ref.getSourceType()) ? "Semantic Scholar API" : "Inferred from Citation Context (API Unavailable)";
        sb.append("- **Source Type**: ").append(type).append("\n");

        sb.append("- **Abstract/Summary**: ").append(ref.getPaperAbstract()).append("\n");

        if (ref.getLinkedPaperId() != null) {
            sb.append("\n**SYSTEM ALERT**: The FULL TEXT of this paper is already in your library!");
            sb.append("\n- Action Hint: You can explore its full content using `getPaperOutline(paperId=" + ref.getLinkedPaperId() + ")`.");
        }

        return sb.toString();
    }

    private String getActivePhrasing(String type) {
        return switch (type.toUpperCase()) {
            case "FOUNDATIONAL" -> "serves as a FOUNDATIONAL BASIS for";
            case "EXTENDS"      -> "EXTENDS the work of";
            case "CONTRADICTS"  -> "CONTRADICTS or REFUTES";
            case "SUPPORT"      -> "SUPPORTS the findings of";
            case "ALTERNATIVE"  -> "presents an ALTERNATIVE approach to";
            default             -> "has a relation (" + type + ") with";
        };
    }

    private String getPassivePhrasing(String type) {
        return switch (type.toUpperCase()) {
            case "FOUNDATIONAL" -> "is BUILT UPON the foundation of";
            case "EXTENDS"      -> "is EXTENDED by";
            case "CONTRADICTS"  -> "is CONTRADICTED by";
            case "SUPPORT"      -> "is SUPPORTED by";
            case "ALTERNATIVE"  -> "is considered an ALTERNATIVE to";
            default             -> "is referenced (" + type + ") by";
        };
    }
}
