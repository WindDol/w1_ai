package cn.winddol.ai.domain.paperTools.service.caservice;

import cn.winddol.ai.domain.agent.service.Librarian;
import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.RefMetadata;
import cn.winddol.ai.domain.paperTools.adapter.parser.IPaperParser;import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.model.entity.SectionPO;


import cn.winddol.ai.types.common.utils.FingerprintUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

//负责编排
@Service
@Slf4j
public class PaperApplicationService {
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
            String fingerprint = FingerprintUtils.generateRefFingerprint(
                    paperMeta.getAuthorSurnames(),
                    paperMeta.getYear()
            );

            Long paperId = paperRepository.saveFullPaper(title, pos, fingerprint);
            paperRepository.updateStatus(paperId, "PARSED");
            librarian.initiateAudit(paperId, title);
            return paperId;
        } finally {
            // 5. 任务完成后删除临时 PDF 文件
            if (tempFile.exists()) tempFile.delete();
        }
    }
}
