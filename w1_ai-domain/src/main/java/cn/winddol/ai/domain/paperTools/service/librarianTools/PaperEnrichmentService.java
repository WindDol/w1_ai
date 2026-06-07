package cn.winddol.ai.domain.paperTools.service.librarianTools;

import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.model.entity.OutlineNode;
import cn.winddol.ai.domain.paperTools.model.entity.PaperEntity;
import cn.winddol.ai.domain.paperTools.model.entity.SectionEntity;
import cn.winddol.ai.domain.paperTools.model.valobj.SymbolDefinition;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PaperEnrichmentService implements cn.winddol.ai.shared.api.IPaperEnrichmentService {
    @Resource
    private IPaperRepository repository;
    @Resource
    private ISymbolExtractor extractor;
    @Resource
    private CitationLinkExtractor citationLinkExtractor;

    public void symbolExtractionAndStorage(Long paperId) {
        PaperEntity paper = repository.getPaperById(paperId);
        List<OutlineNode> outlineTree  = paper.getOutline();
        List<String> allUuids = flattenOutlineIds(outlineTree);
        int count = (int) (allUuids.size() * 0.7);

        List<SectionEntity> allSections = repository.getSectionByUuid(allUuids);
        // key: parentId, value: 子章节列表
        Map<String, List<SectionEntity>> hierarchyMap = allSections.stream()
                .filter(s -> s.getParentId() != null) // 只处理子节点
                .collect(Collectors.groupingBy(SectionEntity::getParentId));

        List<SectionEntity> rootSections = allSections.stream()
                .filter(s -> s.getParentId() == null)
                .sorted(Comparator.comparingInt(SectionEntity::getIdx))
                .toList();

        Map<String, SymbolDefinition> globalSymbolMap = new LinkedHashMap<>();

        for (SectionEntity root : rootSections) {

            // 策略：构建“聚合文本” = 父节点文本 + 所有子节点文本
            List<SectionEntity> flatTree = new ArrayList<>();
            collectSectionsRecursively(root, hierarchyMap, flatTree);


            for (SectionEntity section : flatTree) {

                // 检查：如果内容太短（比如只是个标题），跳过，省钱
                if (section.getContent().length() < 50) continue;

                // 检查：是否在扫描范围内 (前 70% 或 关键词)
                if (shouldScan(section.getHeader()) || section.getIdx() < count) {

                    log.info("Extracting symbols from: {}", section.getHeader());


                    String contextHeader = "Context: " + root.getHeader() + " > " + section.getHeader();
                    String contentWithContext = contextHeader + "\n\n" + section.getContent();

                    List<SymbolDefinition> extracted = extractor.extractFromSection(
                            paper.getTitle(),
                            contentWithContext
                    );

                    // 归并 (SourceId 还是记录当前 Section 的 ID，这样溯源更精准！)
                    mergeSymbols(globalSymbolMap, extracted, root.getId());
                }
            }

        }
        List<SymbolDefinition> finalSymbols = new ArrayList<>(globalSymbolMap.values());


        SectionEntity refSection = allSections.stream()
                .filter(s -> {
                    String h = s.getHeader().toUpperCase();
                    return h.contains("REFERENCE") || h.contains("BIBLIOGRAPHY") ||
                            h.contains("NOTES") || h.contains("REFERENCES") ||
                            h.contains("ACKNOWLEDGMENTS");
                })
                // 使用 max 算子，根据内容的长度进行比较
                .max(Comparator.comparingInt(s -> s.getContent() != null ? s.getContent().length() : 0))
                .orElse(null);

        if (refSection == null && !allSections.isEmpty()) {
            refSection = allSections.get(allSections.size() - 1);
        }

        Map<String, Set<String>> inTextCitationLinks = new HashMap<>();
        for (SectionEntity section : allSections) {
            if (section.equals(refSection)) continue;
            Set<String> linkedIndices = citationLinkExtractor.extractIndices(section.getContent());
            if (!linkedIndices.isEmpty()) {
                inTextCitationLinks.put(section.getId(), linkedIndices);
            }
        }

        // 6. 存回数据库 (Metadata 字段)
        if (!finalSymbols.isEmpty() || refSection != null) {
            repository.saveEnrichmentData(paperId, finalSymbols,refSection,inTextCitationLinks);
            log.info("Paper [{}] enriched with {} symbols.", paperId, finalSymbols.size());
        }
    }

    /**
     * 递归收集章节对象 (深度优先遍历 DFS)
     * 结果 list 的顺序就是：父 -> 子1 -> 子1.1 -> 子2 ...
     */
    private void collectSectionsRecursively(SectionEntity current,
                                            Map<String, List<SectionEntity>> hierarchyMap,
                                            List<SectionEntity> accumulator) {
        // 1. 把自己加入列表
        accumulator.add(current);

        // 2. 获取子节点
        List<SectionEntity> children = hierarchyMap.get(current.getId());

        // 3. 排序并递归
        if (children != null && !children.isEmpty()) {
            children.sort(Comparator.comparingInt(SectionEntity::getIdx));
            for (SectionEntity child : children) {
                collectSectionsRecursively(child, hierarchyMap, accumulator);
            }
        }
    }

    private List<String> flattenOutlineIds(List<OutlineNode> nodes) {
        List<String> ids = new ArrayList<>();
        if (nodes == null) return ids;
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
        return h.contains("INTRODUCTION") || h.contains("BACKGROUND") || h.contains("THEORY") || h.contains("PROOF") ||
                h.contains("MODEL") || h.contains("METHOD") || h.contains("APPENDIX") || header.contains("FORMULATION");// 跳过 ACKNOWLEDGMENTS, REFERENCES 等
    }

    private void mergeSymbols(Map<String, SymbolDefinition> map, List<SymbolDefinition> newSymbols, String sectionId) {
        for (SymbolDefinition newSym : newSymbols) {
            String key = newSym.getSymbol().trim();

            if (key.length() > 10 || isCommonMathConstant(key)) continue;
            SymbolDefinition target;

            if (map.containsKey(key)) {
                target = map.get(key);
                String newDesc = newSym.getDescription();
                String oldDesc = target.getDescription();
                boolean shouldUpdate = false;

                if (isValidDescription(newDesc)) {
                    // 1. 如果现有的描述本来就是空的，直接更新
                    if (!isValidDescription(oldDesc)) {
                        shouldUpdate = true;
                    }
                    // 2. 如果新描述明显更长、更详细，则更新
                    else if (newDesc.length() > oldDesc.length() + 5) {
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
        // 过滤 π, e, i, =, +, - 等基础数学符号
        return key.matches("(?i)^(pi|e|i|=|\\+|-)$") || key.equals("1") || key.equals("0");
    }

    private boolean isValidDescription(String desc) {
        return desc != null &&
                !desc.trim().isEmpty() &&
                !desc.equalsIgnoreCase("null") &&
                !desc.equalsIgnoreCase("none");
    }
}
