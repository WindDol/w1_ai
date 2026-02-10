package cn.winddol.ai.domain.paperTools.service;

import cn.winddol.ai.domain.agent.model.entity.KnowledgeRelationEntity;
import cn.winddol.ai.domain.agent.service.bussiness.Librarian;
import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.model.entity.RefMetadata;
import cn.winddol.ai.domain.paperTools.adapter.parser.IPaperParser;import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.adapter.tools.IFingerprintUtils;
import cn.winddol.ai.domain.paperTools.model.entity.PaperEntity;
import cn.winddol.ai.domain.paperTools.model.entity.SectionPO;


import cn.winddol.ai.domain.paperTools.model.entity.SymbolEntity;
import cn.winddol.ai.domain.paperTools.model.valobj.PaperDetailVO;
import cn.winddol.ai.domain.paperTools.model.valobj.PaperVO;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;
import cn.winddol.ai.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//负责编排
@Service
@Slf4j
public class PaperApplicationService implements IPaperApplication {
    @Resource
    private Librarian librarian;

    @Resource
    private IPaperParser parser;

    @Resource
    private ISymbolExtractor symbolExtractor;
    @Resource
    private IPaperRepository paperRepository;


    @Value("${upload.path:./temp_pdf/}")
    private String uploadPath;
    @Resource
    private IFingerprintUtils fingerprintUtils;

    @Override
    public Long uploadAndParse(MultipartFile file) throws IOException,AppException {
        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();
        File tempFile = new File(dir, UUID.randomUUID() + ".pdf");
        file.transferTo(tempFile);
        try {
            // 2. 调用 Python 获取 Markdown
            String markdown = parser.parsePdfToMarkdown(tempFile.getAbsolutePath());
            List<SectionPO> pos = parser.parse(markdown);
            String title = parser.extractTitle(markdown);
            String abstractText = parser.extractAbstract(pos);

            StringBuilder headerContent = new StringBuilder(pos.get(0).getContent());
            for(int i = 0 ; i < pos.size(); i++){
                if(pos.get(i).header.equals(title)){
                    headerContent.append(pos.get(i).getContent());
                    headerContent.append(pos.get(i+1).getContent());
                }
            }
            RefMetadata paperMeta = symbolExtractor.extractRefMetadata(headerContent.toString());
            if (paperMeta == null || paperMeta.getAuthorSurnames().isEmpty() || paperMeta.getYear() == 0) {
                log.error("❌ Metadata extraction failed. Aborting ingestion to prevent library pollution.");
                return null;
            }
            String fingerprint = fingerprintUtils.generateRefFingerprint(
                    paperMeta.getAuthorSurnames(),
                    paperMeta.getYear()
            );

            Long paperId = paperRepository.saveFullPaper(title, pos, fingerprint,abstractText,paperMeta.getYear());
            paperRepository.updateStatus(paperId, "PARSED");
            librarian.initiateAudit(paperId, title);
            return paperId;
        }catch (AppException e){
            throw e;
        } finally {
            // 5. 任务完成后删除临时 PDF 文件
            if (tempFile.exists()) tempFile.delete();
        }
    }

    @Override
    public List<PaperVO> listAllPapers() {
        return paperRepository.listAllPapers();
    }
    @Override
    public PaperDetailVO getPaperDetails(Long paperId) {
        PaperEntity paper = paperRepository.getPaperDetailsById(paperId);
        List<SymbolEntity> symbols = paperRepository.findByPaperId(paperId);
        List<KnowledgeRelationEntity> relations = paperRepository.findRelationsByPaperId(paperId);
        List<ReferenceItem> referenceItemList = paperRepository.selectReferencesByPaperId(paperId); // 注意：这里最好查已入库的引用详情

        StringBuilder novelty = new StringBuilder();

        if (!relations.isEmpty()) {
            novelty.append("**🔗 Inter-paper relations from the Librarian's audit:**\n\n");

            for (KnowledgeRelationEntity rel : relations) {
                String type = rel.getType();
                String otherPaperInfo = String.format("Paper [%d] (%s)", rel.getRelatedId(), rel.getRelatedTitle());

                if ("OUTGOING".equals(rel.getDirection())) {
                    String actionPhrase = getActivePhrasing(type);

                    novelty.append(String.format("- This paper **%s** %s.\n  *Reason: %s*\n",
                            actionPhrase, otherPaperInfo, rel.getDescription()));
                } else {
                    String passivePhrase = getPassivePhrasing(type);
                    novelty.append(String.format("- This paper **%s** %s.\n  *Note: %s*\n",
                            passivePhrase, otherPaperInfo, rel.getDescription()));
                }
            }
        } else {
            // 没有关系时的默认文案
            novelty.append("🦉 *The Librarian found no direct or specific relations with other papers in the library yet.*");
        }

        PaperDetailVO vo = PaperDetailVO.builder()
                .id(paper.getId())
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .noveltyAssessment(novelty.toString()) // 这里的 String 已经是格式化好的 Markdown
                .symbolCount(symbols.size())
                .referenceCount(referenceItemList.size())
                // 取前 5 个符号展示
                .keySymbols(symbols.stream().limit(5).collect(Collectors.toList()))
                .build();

        return vo;
    }

    // --- ⬇️ 将这两个辅助方法添加在 Service 类下方 (或者提取到工具类) ---

    private String getActivePhrasing(String type) {
        if (type == null) return "relates to";
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
        if (type == null) return "is related to";
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
