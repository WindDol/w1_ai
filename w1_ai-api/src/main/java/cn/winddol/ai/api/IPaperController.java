package cn.winddol.ai.api;

import cn.winddol.ai.api.dto.PaperDTO;
import cn.winddol.ai.api.dto.PaperDetailDTO;
import cn.winddol.ai.api.dto.PaperIngestJobDTO;
import cn.winddol.ai.api.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPaperController {
    ResponseEntity<Response<PaperIngestJobDTO>> upload(MultipartFile file);
    ResponseEntity<Response<PaperIngestJobDTO>> getIngestJob(String jobId);
    ResponseEntity<Response<PaperIngestJobDTO>> retryIngestJob(String jobId);
    ResponseEntity<Response<PaperIngestJobDTO>> rerunIngestJob(String jobId, String stage);
    Response<List<PaperDTO>> listPapers();
    Response<PaperDetailDTO> getPaperDetails(Long paperId);

}
