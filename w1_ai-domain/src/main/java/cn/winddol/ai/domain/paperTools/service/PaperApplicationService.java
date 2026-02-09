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
    public Long uploadAndParse(MultipartFile file) throws IOException {
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
            for(SectionPO  po:pos){
                if(po.header.equals(title)){
                    headerContent.append(po.getContent());
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

            Long paperId = paperRepository.saveFullPaper(title, pos, fingerprint,abstractText);
            paperRepository.updateStatus(paperId, "PARSED");
            librarian.initiateAudit(paperId, title);
            return paperId;
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
        List<ReferenceItem> referenceItemList = paperRepository.selectReferencesByPaperId(paperId);
        StringBuilder novelty = new StringBuilder();
        if (!relations.isEmpty()) {
            novelty.append("**\uD83D\uDD17 Inter-paper relations from the Librarian's audit:**\n");
            for (KnowledgeRelationEntity rel : relations) {
                if ("OUTGOING".equals(rel.getDirection())) {
                    // 当前论文 -> 评价 -> 别人
                    novelty.append(String.format("- This paper [%s] %s Paper [%d] (%s). Reason: %s\n",
                            rel.getType(), rel.getType(), rel.getRelatedId(), rel.getRelatedTitle(), rel.getDescription()));
                } else {
                    // 别人 -> 评价 -> 当前论文
                    novelty.append(String.format("- This paper IS %s BY Paper [%d] (%s). Note: %s\n",
                            rel.getType(), rel.getRelatedId(), rel.getRelatedTitle(), rel.getDescription()));
                }
            }
        }else{
            novelty.append("The Librarian found no direct or specific relations with other papers in the library.");
        }

        return PaperDetailVO.builder()
                .id(paper.getId())
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .noveltyAssessment(novelty.toString())
                .symbolCount(symbols.size())
                .referenceCount(referenceItemList.size())
                .keySymbols(symbols.stream().limit(5).collect(Collectors.toList()))
                .build();
    }
}
