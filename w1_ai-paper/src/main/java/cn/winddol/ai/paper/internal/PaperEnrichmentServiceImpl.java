package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.model.entity.OutlineNode;
import cn.winddol.ai.paper.model.entity.PaperEntity;
import cn.winddol.ai.paper.model.entity.SectionEntity;
import cn.winddol.ai.paper.model.valobj.SymbolDefinition;
import cn.winddol.ai.shared.api.IPaperEnrichmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaperEnrichmentServiceImpl implements IPaperEnrichmentService {

    @Resource
    private IPaperRepository repository;
    @Resource
    private ISymbolExtractor extractor;
    @Resource
    private CitationLinkExtractor citationLinkExtractor;

    @Override
    public void symbolExtractionAndStorage(Long paperId) {
        PaperEntity paper = repository.getPaperById(paperId);
        if (paper == null || paper.getOutline() == null) {
            log.warn("Paper [{}] has no outline, skip symbol enrichment.", paperId);
            return;
        }

        List<OutlineNode> outlineTree = paper.getOutline();
        List<String> allUuids = flattenOutlineIds(outlineTree);
        int count = (int) (allUuids.size() * 0.7);

        List<SectionEntity> allSections = repository.getSectionByUuid(allUuids);
        Map<String, List<SectionEntity>> hierarchyMap = allSections.stream()
                .filter(s -> s.getParentId() != null)
                .collect(Collectors.groupingBy(SectionEntity::getParentId));

        List<SectionEntity> rootSections = allSections.stream()
                .filter(s -> s.getParentId() == null)
                .sorted(Comparator.comparingInt(SectionEntity::getIdx))
                .toList();

        Map<String, SymbolDefinition> globalSymbolMap = new LinkedHashMap<>();
        for (SectionEntity root : rootSections) {
            List<SectionEntity> flatTree = new ArrayList<>();
            collectSectionsRecursively(root, hierarchyMap, flatTree);

            for (SectionEntity section : flatTree) {
                if (section.getContent() == null || section.getContent().length() < 50) {
                    continue;
                }
                if (shouldScan(section.getHeader()) || section.getIdx() < count) {
                    log.info("Extracting symbols from: {}", section.getHeader());
                    String contextHeader = "Context: " + root.getHeader() + " > " + section.getHeader();
                    String contentWithContext = contextHeader + "\n\n" + section.getContent();
                    List<SymbolDefinition> extracted = extractor.extractFromSection(paper.getTitle(), contentWithContext);
                    mergeSymbols(globalSymbolMap, extracted, root.getId());
                }
            }
        }

        List<SymbolDefinition> finalSymbols = new ArrayList<>(globalSymbolMap.values());
        SectionEntity refSection = findReferenceSection(allSections);

        Map<String, Set<String>> inTextCitationLinks = new HashMap<>();
        for (SectionEntity section : allSections) {
            if (section.equals(refSection)) {
                continue;
            }
            Set<String> linkedIndices = citationLinkExtractor.extractIndices(section.getContent());
            if (!linkedIndices.isEmpty()) {
                inTextCitationLinks.put(section.getId(), linkedIndices);
            }
        }

        if (!finalSymbols.isEmpty() || refSection != null) {
            repository.saveEnrichmentData(paperId, finalSymbols, refSection, inTextCitationLinks);
            log.info("Paper [{}] enriched with {} symbols.", paperId, finalSymbols.size());
        }
    }

    private SectionEntity findReferenceSection(List<SectionEntity> allSections) {
        SectionEntity refSection = allSections.stream()
                .filter(s -> {
                    String h = s.getHeader().toUpperCase();
                    return h.contains("REFERENCE") || h.contains("BIBLIOGRAPHY")
                            || h.contains("NOTES") || h.contains("REFERENCES")
                            || h.contains("ACKNOWLEDGMENTS");
                })
                .max(Comparator.comparingInt(s -> s.getContent() != null ? s.getContent().length() : 0))
                .orElse(null);

        if (refSection == null && !allSections.isEmpty()) {
            return allSections.get(allSections.size() - 1);
        }
        return refSection;
    }

    private void collectSectionsRecursively(SectionEntity current,
                                            Map<String, List<SectionEntity>> hierarchyMap,
                                            List<SectionEntity> accumulator) {
        accumulator.add(current);
        List<SectionEntity> children = hierarchyMap.get(current.getId());
        if (children != null && !children.isEmpty()) {
            children.sort(Comparator.comparingInt(SectionEntity::getIdx));
            for (SectionEntity child : children) {
                collectSectionsRecursively(child, hierarchyMap, accumulator);
            }
        }
    }

    private List<String> flattenOutlineIds(List<OutlineNode> nodes) {
        List<String> ids = new ArrayList<>();
        if (nodes == null) {
            return ids;
        }
        for (OutlineNode node : nodes) {
            ids.add(node.getId());
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                ids.addAll(flattenOutlineIds(node.getChildren()));
            }
        }
        return ids;
    }

    private boolean shouldScan(String header) {
        String h = header.toUpperCase();
        return h.contains("INTRODUCTION") || h.contains("BACKGROUND") || h.contains("THEORY")
                || h.contains("PROOF") || h.contains("MODEL") || h.contains("METHOD")
                || h.contains("APPENDIX") || header.contains("FORMULATION");
    }

    private void mergeSymbols(Map<String, SymbolDefinition> map, List<SymbolDefinition> newSymbols, String sectionId) {
        for (SymbolDefinition newSym : newSymbols) {
            String key = newSym.getSymbol().trim();
            if (key.length() > 10 || isCommonMathConstant(key)) {
                continue;
            }

            SymbolDefinition target;
            if (map.containsKey(key)) {
                target = map.get(key);
                String newDesc = newSym.getDescription();
                String oldDesc = target.getDescription();
                boolean shouldUpdate = false;

                if (isValidDescription(newDesc)) {
                    if (!isValidDescription(oldDesc)) {
                        shouldUpdate = true;
                    } else if (newDesc.length() > oldDesc.length() + 5) {
                        shouldUpdate = true;
                    }
                }
                if (shouldUpdate) {
                    target.setDescription(newDesc);
                    target.setLatex(newSym.getLatex());
                    target.setDefinitionFormula(newSym.getDefinitionFormula());
                }
            } else {
                target = newSym;
                map.put(key, target);
            }

            target.getScopes().add(sectionId);
            if (target.getScopes().size() > 3) {
                target.setGlobal(true);
            }
        }
    }

    private boolean isCommonMathConstant(String key) {
        return key.matches("(?i)^(pi|e|i|=|\\+|-)$") || key.equals("1") || key.equals("0");
    }

    private boolean isValidDescription(String desc) {
        return desc != null
                && !desc.trim().isEmpty()
                && !desc.equalsIgnoreCase("null")
                && !desc.equalsIgnoreCase("none");
    }
}
