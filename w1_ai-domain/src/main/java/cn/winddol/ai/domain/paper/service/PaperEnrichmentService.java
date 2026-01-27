package cn.winddol.ai.domain.paper.service;

import cn.winddol.ai.domain.paper.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paper.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paper.model.entity.OutlineNode;
import cn.winddol.ai.domain.paper.model.entity.PaperEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionEntity;
import cn.winddol.ai.domain.paper.model.valobj.ReferenceItem;
import cn.winddol.ai.domain.paper.model.valobj.SymbolDefinition;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaperEnrichmentService implements IPaperEnrichmentService{
    @Resource
    private IPaperRepository repository;
    @Resource
    private ISymbolExtractor extractor;

    @Override
    public void symbolExtractionAndStorage(Long paperId) {
        PaperEntity paper = repository.getPaperById(paperId);
        List<OutlineNode> outlineTree  = paper.getOutline();
        List<String> allUuids = flattenOutlineIds(outlineTree);
        int count = allUuids.size() >> 1 ;

        List<SectionEntity> allSections = repository.getSectionByUuid(allUuids);

        // key: parentId, value: 子章节列表
        Map<String, List<SectionEntity>> hierarchyMap = allSections.stream()
                .filter(s -> s.getParentId() != null) // 只处理子节点
                .collect(Collectors.groupingBy(SectionEntity::getParentId));

        List<SectionEntity> rootSections = allSections.stream()
                .filter(s -> s.getParentId() == null)
                .toList();

        Map<String, SymbolDefinition> globalSymbolMap = new LinkedHashMap<>();

        for (SectionEntity root : rootSections) {

            // 策略：构建“聚合文本” = 父节点文本 + 所有子节点文本
            StringBuilder aggregatedContent = new StringBuilder();

            // 5.1 加入父节点内容
            aggregatedContent.append(root.getHeader()).append("\n")
                    .append(root.getContent()).append("\n\n");

            // 5.2 查找并加入子节点内容
            List<SectionEntity> children = hierarchyMap.getOrDefault(root.getId(), Collections.emptyList());
            // 按 idx 排序，保证阅读顺序
            children.sort(Comparator.comparingInt(SectionEntity::getIdx));

            for (SectionEntity child : children) {
                aggregatedContent.append(child.getHeader()).append("\n")
                        .append(child.getContent()).append("\n\n");
            }

            // 5.3 只有当聚合后的内容足够丰富（比如 > 200 字符）且在扫描范围内才提取
            // 这里的 shouldScan 逻辑可以用之前的“前 50%”或“关键词”策略
            if (shouldScan(root.getHeader()) || root.getIdx() < count) {
                log.info("Extracting symbols from aggregated chapter: {}", root.getHeader());

                // 【一次调用，搞定整章】
                List<SymbolDefinition> extracted = extractor.extractFromSection(
                        paper.getTitle(),
                        aggregatedContent.toString()
                );

                // 归并
                mergeSymbols(globalSymbolMap, extracted, root.getId());
            }

        }
        List<SymbolDefinition> finalSymbols = new ArrayList<>(globalSymbolMap.values());

        List<ReferenceItem> finalReferences = new ArrayList<>();
        SectionEntity refSection = allSections.stream()
                .filter(s -> s.getHeader().toUpperCase().contains("REFERENCE"))
                .findFirst()
                .orElse(null);

        // 6. 存回数据库 (Metadata 字段)
        if (!finalSymbols.isEmpty() || refSection != null) {
            repository.updatePaperMetadata(paperId, finalSymbols,refSection);
            log.info("Paper [{}] enriched with {} symbols.", paperId, finalSymbols.size());
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

            // 过滤掉常见无意义符号 (可选)
            if (key.length() > 10 || isCommonMathConstant(key)) continue;
            SymbolDefinition target;

            if (map.containsKey(key)) {
                // 已存在，取出来更新
                target = map.get(key);
                // 择优更新描述（保留最长的那个）
                if (isValidDescription(newSym.getDescription()) &&
                        newSym.getDescription().length() > target.getDescription().length() + 5) {
                    target.setDescription(newSym.getDescription());
                    target.setLatex(newSym.getLatex());
                    target.setDefinitionFormula(newSym.getDefinitionFormula());
                }
            } else {
                // 新符号
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
        return desc != null && !desc.trim().isEmpty() && !desc.equalsIgnoreCase("symbol");
    }
}
