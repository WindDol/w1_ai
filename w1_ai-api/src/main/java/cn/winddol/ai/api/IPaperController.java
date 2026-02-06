package cn.winddol.ai.api;

import cn.winddol.ai.api.response.Response;
import org.springframework.web.multipart.MultipartFile;

public interface IPaperController {
    Response<String> upload(MultipartFile file);
}
