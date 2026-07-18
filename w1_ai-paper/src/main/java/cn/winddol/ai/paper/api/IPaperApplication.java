package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.PaperDetailVO;
import cn.winddol.ai.paper.domain.PaperVO;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IPaperApplication {

    PaperIngestJob submit(MultipartFile file) throws IOException;

    PaperIngestJob getIngestJob(String jobId);

    PaperIngestJob retry(String jobId);

    PaperIngestJob rerunFrom(String jobId, PaperIngestStage stage);

    int recoverInterruptedJobs();

    List<PaperVO> listAllPapers();

    PaperDetailVO getPaperDetails(Long paperId);
}
