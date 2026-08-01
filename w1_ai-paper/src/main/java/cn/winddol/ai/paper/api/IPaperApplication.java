package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.domain.PaperDetailVO;
import cn.winddol.ai.paper.domain.PaperVO;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.workspace.ArtifactContent;
import cn.winddol.ai.paper.domain.workspace.ArtifactType;
import cn.winddol.ai.paper.domain.workspace.IngestionWorkspaceView;
import cn.winddol.ai.paper.domain.workspace.PaperWorkspaceView;
import cn.winddol.ai.paper.domain.workspace.SectionWorkspaceView;
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

    PaperWorkspaceView getPaperReading(Long paperId);

    SectionWorkspaceView getSectionReading(String sectionId);

    IngestionWorkspaceView getIngestionDetails(String jobId);

    ArtifactContent readIngestionArtifact(String jobId, ArtifactType type);
}
