package cn.winddol.ai.api;

import cn.winddol.ai.api.dto.PaperDTO;
import cn.winddol.ai.api.dto.PaperDetailDTO;
import cn.winddol.ai.api.dto.PaperIngestJobDTO;
import cn.winddol.ai.api.dto.workspace.ArtifactContentDTO;
import cn.winddol.ai.api.dto.workspace.IngestionWorkspaceDTO;
import cn.winddol.ai.api.dto.workspace.PaperWorkspaceDTO;
import cn.winddol.ai.api.dto.workspace.SectionWorkspaceDTO;
import cn.winddol.ai.api.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPaperController {
    ResponseEntity<Response<PaperIngestJobDTO>> upload(MultipartFile file);
    ResponseEntity<Response<PaperIngestJobDTO>> getIngestJob(String jobId);
    ResponseEntity<Response<PaperIngestJobDTO>> retryIngestJob(String jobId);
    ResponseEntity<Response<PaperIngestJobDTO>> rerunIngestJob(String jobId, String stage);
    ResponseEntity<Response<PaperWorkspaceDTO>> getPaperReading(Long paperId);
    ResponseEntity<Response<SectionWorkspaceDTO>> getSection(String sectionId);
    ResponseEntity<Response<IngestionWorkspaceDTO>> getIngestionDetails(String jobId);
    ResponseEntity<Response<ArtifactContentDTO>> getIngestionArtifact(String jobId, String type);
    Response<List<PaperDTO>> listPapers();
    Response<PaperDetailDTO> getPaperDetails(Long paperId);

}
