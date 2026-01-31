package cn.winddol.ai.domain.paper.service;

import cn.winddol.ai.domain.paper.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paper.model.entity.OutlineNode;
import cn.winddol.ai.domain.paper.model.entity.PaperEntity;
import cn.winddol.ai.domain.paper.model.entity.SectionEntity;
import cn.winddol.ai.domain.paper.model.entity.SymbolEntity;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.awt.print.Paper;
import java.util.LinkedList;
import java.util.List;

@Service
public class AgentReaderService {
    @Resource
    private IPaperRepository repository;

    /**
     * 工具：获取目录 (Get Outline)
     * Agent 用这个来看书的结构
     */
    public List<OutlineNode> getPaperOutline(Long paperId) {
        PaperEntity paper = repository.selectPaperById(paperId);
        return paper.getOutline();
    }

    /**
     * 工具 智能阅读 (Smart Read)
     * 读取正文 + 自动注入局部符号表
     */
    public String readSectionWithContext(String sectionUuid) {
        SectionEntity section = repository.selectSectionById(sectionUuid);
        if (section == null) return "Error: Section not found.";
        StringBuilder sb = new StringBuilder();
        sb.append("### 📍 Navigation Path\n");
        sb.append(buildBreadcrumb(section)).append("\n\n");

        List<SymbolEntity> relatedSymbols = repository.selectSymbolsByUuids(sectionUuid);

        if (!relatedSymbols.isEmpty()) {
            sb.append("### 📖 Mathematical Dictionary (Relevant to this section)\n");
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
                // 只取父节点开头的一部分（比如前 300 字），作为背景知识
                String parentIntro = parent.getContent().length() > 300 ?
                        parent.getContent().substring(0, 300) + "..." :
                        parent.getContent();
                // 只有当父节点内容不为空时才添加
                if (!parentIntro.trim().isEmpty()) {
                    sb.append("### 💡 Parent Context (Background info from ").append(parent.getHeader()).append(")\n");
                    sb.append(parentIntro).append("\n\n");
                }
            }
        }


        sb.append("### 📄 Current Section Content\n");
        sb.append(section.getContent()).append("\n\n");

        SectionEntity prev = getSibling(section, -1); // 查 idx - 1
        SectionEntity next = getSibling(section, 1);  // 查 idx + 1

        sb.append("### 🧭 Nearby Sections\n");
        if (prev != null) sb.append("- Previous: ").append(prev.getHeader()).append(" (ID: ").append(prev.getId()).append(")\n");
        if (next != null) sb.append("- Next: ").append(next.getHeader()).append(" (ID: ").append(next.getId()).append(")\n");

        return sb.toString();
    }

    private String buildBreadcrumb(SectionEntity section) {
        LinkedList<String> hierarchy = new LinkedList<>();
        hierarchy.add(section.getHeader());
        String currentParentId = section.getParentId();
        int safetyCounter = 0;
        while (currentParentId != null && safetyCounter < 5) {
            SectionEntity parent = repository.selectSectionById(currentParentId);
            if (parent == null) break;

            // 插到队头
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
        return repository.getSectionSibling(current.getPaperId(),current.getIdx(), offset);
    }
}
