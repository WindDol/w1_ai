package cn.winddol.ai.domain.agent.service.bussiness;

import cn.winddol.ai.domain.agent.adapter.embedding.IEmbeddingProcessor;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.service.librarianTools.CitationEnrichmentService;
import cn.winddol.ai.domain.paperTools.service.librarianTools.PaperEnrichmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class PaperAuditListener{
    @Resource
    private PaperEnrichmentService symbolService;
    @Resource
    private CitationEnrichmentService citationService;
    @Resource
    private IEmbeddingProcessor embeddingProcessor;
    @Resource
    private IPaperRepository paperRepository;
    @Resource
    private LibrarianAuditService auditService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Async
    @EventListener
    public void onAuditCommand(cn.winddol.ai.domain.paperTools.event.PaperIngestedEvent event) {
        long start = System.currentTimeMillis();
        Long paperId = event.getPaperId();
        log.info("⚙️ Infra: 收到 Librarian 的命令，后台线程开始处理 Paper [{}]...", paperId);
        paperRepository.updateStatus(paperId, "AUDITING");
        try {
            // === 这里放入你提供的逻辑代码 ===

            // 1. 符号提取
            log.info("1️⃣ 开始提取符号...");
            symbolService.symbolExtractionAndStorage(paperId);

            // 2. 引用抓取 (针对当前 Paper)
            log.info("2️⃣ 开始抓取引用...");
            threadPoolExecutor.execute(()-> citationService.enrichPaperReferences(paperId));
            // 3. 补全向量
            log.info("3️⃣ 开始向量化...");
            embeddingProcessor.embedReferences(paperId);
            embeddingProcessor.embedSections(paperId);
            // 4.冲突检测
            auditService.auditAgainstLibrary(paperId);
            paperRepository.updateStatus(paperId, "COMPLETED");
            log.info("✅ Paper [{}] 所有的后台处理工作已完成！", paperId);

        } catch (Exception e) {
            log.error("❌ 处理 Paper [{}] 时发生异常", paperId, e);
            paperRepository.updateStatusWithError(paperId, "ERROR", e.getMessage());
        }
        log.info("🎉 Librarian: Paper [{}] 全部就绪! 耗时: {}ms", paperId, System.currentTimeMillis() - start);
    }
}
