package cn.winddol.ai.api;

import cn.winddol.ai.api.dto.PaperDTO;
import cn.winddol.ai.api.dto.PaperDetailDTO;
import cn.winddol.ai.api.response.Response;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPaperController {
    Response<String> upload(MultipartFile file);
    Response<List<PaperDTO>> listPapers();
    Response<PaperDetailDTO> getPaperDetails(Long paperId);

}
