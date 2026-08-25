package cn.winddol.ai.paper.internal.retrieval;

import cn.winddol.ai.paper.domain.*;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.retrieval.EvidenceType;
import cn.winddol.ai.shared.model.tool.ToolEvidence;
import cn.winddol.ai.shared.model.tool.ToolResult;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        return readSectionWithEvidence(sectionUuid).getContent();
    }

    /**
     * 读取章节正文与符号，并把本次实际读取的章节和符号转换为可供 Agent 输出的证据链。
     */
    public ToolResult readSectionWithEvidence(String sectionUuid) {
        SectionEntity section = repository.selectSectionById(sectionUuid);
        if (section == null) {
            return ToolResult.ok("Section not found: " + sectionUuid);
        }

        StringBuilder sb = new StringBuilder();
        PaperEntity paper = repository.selectPaperById(section.getPaperId());
        String headingPath = resolveHeadingPath(section);
        sb.append("### Navigation Path\n");
        if (paper != null && paper.getTitle() != null) {
            sb.append("Paper: \"").append(paper.getTitle()).append("\" > ");
        }
        sb.append(headingPath).append("\n\n");

        List<SymbolEntity> symbols = repository.selectSymbolsByUuids(sectionUuid);
        if (symbols != null && !symbols.isEmpty()) {
            sb.append("### Mathematical Dictionary (Relevant to this section)\n");
            for (SymbolEntity s : symbols) {
                sb.append(String.format("- **%s**: %s", s.getSymbol(), s.getDescription()));
                if (s.getDefinitionFormula() != null) {
                    sb.append(String.format(" [Def: $%s$]", s.getDefinitionFormula()));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        SectionEntity parent = null;
        if (section.getParentId() != null && !section.getParentId().isBlank()) {
            parent = repository.selectSectionById(section.getParentId());
        }
        if (parent != null) {
            sb.append("### Parent Section Background\n");
            sb.append("**").append(valueOrLegacy(parent.getHeader())).append("**\n");
            if (parent.getContent() != null && !parent.getContent().isBlank()) {
                sb.append(abbreviate(parent.getContent(), 500)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("### Current Section Content\n");
        sb.append(section.getContent() == null ? "" : section.getContent()).append("\n\n");

        List<ReferenceItem> references = appendCitationContext(sb, section);
        appendNearbySections(sb, section);
        List<ToolEvidence> evidence = new ArrayList<>();
        evidence.add(ToolEvidence.builder()
                .evidenceKey("SECTION:" + section.getId())
                .evidenceType(EvidenceType.SECTION.name())
                .paperId(section.getPaperId())
                .paperTitle(paper == null ? null : paper.getTitle())
                .sectionId(section.getId())
                .heading(section.getHeader())
                .headingPath(headingPath)
                .quote(abbreviate(section.getContent(), 1600))
                .accessedVia("readSection")
                .build());
        if (symbols != null) {
            for (SymbolEntity symbol : symbols) {
                evidence.add(ToolEvidence.builder()
                        .evidenceKey(symbolEvidenceKey(symbol, section.getId()))
                        .evidenceType(EvidenceType.SYMBOL.name())
                        .paperId(section.getPaperId())
                        .paperTitle(paper == null ? null : paper.getTitle())
                        .sectionId(section.getId())
                        .heading(symbol.getSymbol())
                        .headingPath(headingPath)
                        .quote(symbolQuote(symbol))
                        .accessedVia("readSection")
                        .build());
            }
        }
        for (ReferenceItem reference : references) {
            evidence.add(ToolEvidence.builder()
                    .evidenceKey("REFERENCE:" + section.getPaperId() + ':' + reference.getRefId())
                    .evidenceType(EvidenceType.REFERENCE.name())
                    .paperId(section.getPaperId())
                    .paperTitle(paper == null ? null : paper.getTitle())
                    .sectionId(section.getId())
                    .heading(reference.getTitle())
                    .headingPath(headingPath)
                    .referenceIndex(reference.getRefId())
                    .quote(abbreviate(referenceText(reference), 1600))
                    .accessedVia("readSection")
                    .build());
        }
        return ToolResult.ok(sb.toString(), evidence);
    }

    /**
     * 插入本节实际引用的参考文献摘要。这恢复了 Smart Read 中与引用相关的功能，同时保持当前章节内容完整不变。
     */
    private List<ReferenceItem> appendCitationContext(StringBuilder content, SectionEntity section) {
        List<SectionReferenceLinkEntity> links = repository.selectLinksBySectionId(section.getId());
        if (links == null || links.isEmpty()) {
            return List.of();
        }
        List<ReferenceItem> references = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SectionReferenceLinkEntity link : links) {
            ReferenceItem reference = repository.selectReferenceByIndex(section.getPaperId(), link.getRefIndex());
            if (reference != null && seen.add(String.valueOf(reference.getRefId()))) {
                references.add(reference);
            }
        }
        if (references.isEmpty()) {
            return List.of();
        }

        content.append("### External References Cited in This Section\n");
        content.append("Use this context to understand citation markers in the section.\n\n");
        for (ReferenceItem reference : references) {
            content.append("- **[").append(valueOrLegacy(reference.getRefId())).append("] ")
                    .append(valueOrLegacy(reference.getTitle())).append("**\n");
            if (reference.getLinkedPaperId() != null) {
                content.append("  Full text is available in the library (paperId=")
                        .append(reference.getLinkedPaperId()).append(").\n");
            }
            content.append("  Abstract/Summary: ")
                    .append(abbreviate(referenceText(reference), 500)).append("\n\n");
        }
        return List.copyOf(references);
    }

    /**
     * 将相邻的节段保留为导航链接。它们的内容不会被注入，因为
     * 匹配的节段仍作为语义上下文单元；代理程序可在需要时通过链接进行跳转。
     */
    private void appendNearbySections(StringBuilder content, SectionEntity section) {
        if (section.getPaperId() == null || section.getIdx() == null) {
            return;
        }
        SectionEntity previous = repository.getSectionSibling(section.getPaperId(), section.getIdx(), -1);
        SectionEntity next = repository.getSectionSibling(section.getPaperId(), section.getIdx(), 1);
        if (previous == null && next == null) {
            return;
        }
        content.append("### Nearby Sections\n");
        if (previous != null) {
            content.append("- Previous: ").append(sectionLink(previous)).append("\n");
        }
        if (next != null) {
            content.append("- Next: ").append(sectionLink(next)).append("\n");
        }
        content.append("\n");
    }

    private String sectionLink(SectionEntity section) {
        return "[sectionId=" + valueOrLegacy(section.getId()) + "] " + valueOrLegacy(section.getHeader());
    }

    public String lookupReference(Long paperId, String refIndex) {
        return lookupReferenceWithEvidence(paperId, refIndex).getContent();
    }

    /**
     * 查询指定参考文献，并将该条参考文献作为可复查证据返回给 Agent。
     */
    public ToolResult lookupReferenceWithEvidence(Long paperId, String refIndex) {
        ReferenceItem ref = repository.lookupReference(paperId, refIndex);
        if (ref == null) {
            return ToolResult.ok("Reference [" + refIndex + "] not found in Paper " + paperId);
        }
        String content = String.format("**[%s] %s**\n\nAbstract: %s",
                ref.getRefId(), ref.getTitle(), ref.getPaperAbstract());
        PaperEntity paper = repository.selectPaperById(paperId);
        ToolEvidence evidence = ToolEvidence.builder()
                .evidenceKey("REFERENCE:" + paperId + ':' + ref.getRefId())
                .evidenceType(EvidenceType.REFERENCE.name())
                .paperId(paperId)
                .paperTitle(paper == null ? null : paper.getTitle())
                .heading(ref.getTitle())
                .referenceIndex(ref.getRefId())
                .quote(abbreviate(referenceText(ref), 1600))
                .accessedVia("lookupReference")
                .build();
        return ToolResult.ok(content, List.of(evidence));
    }

    /**
     * 从当前章节向父节点回溯，拼接真实存在的 Outline 路径，不虚构缺失层级。
     */
    private String resolveHeadingPath(SectionEntity section) {
        LinkedList<String> headings = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        SectionEntity current = section;
        while (current != null && current.getId() != null && visited.add(current.getId())) {
            if (current.getHeader() != null && !current.getHeader().isBlank()) {
                headings.addFirst(current.getHeader());
            }
            if (current.getParentId() == null || current.getParentId().isBlank()) {
                break;
            }
            current = repository.selectSectionById(current.getParentId());
        }
        return String.join(" > ", headings);
    }

    /**
     * 为章节内或全局符号生成稳定证据键，优先使用数据库 ID。
     */
    private String symbolEvidenceKey(SymbolEntity symbol, String sectionId) {
        if (symbol.getId() != null) {
            return "SYMBOL:" + symbol.getId();
        }
        return "SYMBOL:" + sectionId + ':' + String.valueOf(symbol.getSymbol());
    }

    /**
     * 将符号描述和定义拼成读者可直接核对的原始文本。
     */
    private String symbolQuote(SymbolEntity symbol) {
        StringBuilder quote = new StringBuilder();
        if (symbol.getDescription() != null) {
            quote.append(symbol.getDescription());
        }
        if (symbol.getDefinitionFormula() != null && !symbol.getDefinitionFormula().isBlank()) {
            if (!quote.isEmpty()) {
                quote.append(' ');
            }
            quote.append("Definition: ").append(symbol.getDefinitionFormula());
        }
        return quote.toString();
    }

    /**
     * 参考文献优先展示摘要，摘要缺失时回退到原始引文文本。
     */
    private String referenceText(ReferenceItem reference) {
        if (reference.getPaperAbstract() != null && !reference.getPaperAbstract().isBlank()) {
            return reference.getPaperAbstract();
        }
        return reference.getRawText();
    }

    /**
     * 限制事件体积，同时保留足够上下文供读者通过章节接口继续核对。
     */
    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }

    public String checkPaperRelations(Long paperId) {
        var relations = repository.findRelationsByPaperId(paperId);
        if (relations == null || relations.isEmpty()) {
            return "No inter-paper relations found.";
        }
        StringBuilder sb = new StringBuilder("### Inter-paper Relations\n");
        for (var rel : relations) {
            sb.append(String.format("- **%s** Paper[%d] (%s, confidence=%s, status=%s): %s\n",
                    rel.getType(), rel.getRelatedId(), rel.getDirection(),
                    formatConfidence(rel.getConfidence()), valueOrLegacy(rel.getAuditStatus()), rel.getDescription()));
            appendAuditEvidence(sb, "Supporting evidence", rel.getSupportingEvidence());
            appendAuditEvidence(sb, "Conflicting evidence", rel.getConflictingEvidence());
            if (rel.getAuditVersion() != null && !rel.getAuditVersion().isBlank()) {
                sb.append("  Audit: version=").append(rel.getAuditVersion())
                        .append(", model=").append(valueOrLegacy(rel.getModelName()))
                        .append(", retrieval=").append(valueOrLegacy(rel.getRetrievalVersion()))
                        .append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 展示关系审计实际引用的少量证据，让读者可以通过 sectionId 或 referenceIndex 回到原文核查。
     */
    private void appendAuditEvidence(StringBuilder content, String label, String rawEvidence) {
        if (rawEvidence == null || rawEvidence.isBlank() || "[]".equals(rawEvidence.trim())) {
            return;
        }
        try {
            List<ToolEvidence> evidence = JSON.parseArray(rawEvidence, ToolEvidence.class);
            if (evidence == null || evidence.isEmpty()) {
                return;
            }
            content.append("  ").append(label).append(":\n");
            for (ToolEvidence item : evidence) {
                content.append("  - [").append(item.getEvidenceKey()).append("] ")
                        .append(valueOrLegacy(item.getHeadingPath()))
                        .append(" | ").append(valueOrLegacy(item.getQuote()))
                        .append("\n");
            }
        } catch (Exception ignored) {
            content.append("  ").append(label).append(": unavailable (legacy audit payload)\n");
        }
    }

    private String formatConfidence(Double confidence) {
        return confidence == null ? "legacy" : String.format("%.2f", confidence);
    }

    private String valueOrLegacy(String value) {
        return value == null || value.isBlank() ? "legacy" : value;
    }
}
