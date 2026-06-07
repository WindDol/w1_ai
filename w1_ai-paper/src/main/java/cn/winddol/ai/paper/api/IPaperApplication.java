package cn.winddol.ai.paper.api;

import cn.winddol.ai.paper.model.valobj.PaperDetailVO;
import cn.winddol.ai.paper.model.valobj.PaperVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IPaperApplication {

    Long uploadAndParse(MultipartFile file) throws IOException;

    List<PaperVO> listAllPapers();

    PaperDetailVO getPaperDetails(Long paperId);
}
