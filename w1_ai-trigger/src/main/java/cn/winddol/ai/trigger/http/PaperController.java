package cn.winddol.ai.trigger.http;

import cn.winddol.ai.api.IPaperController;
import cn.winddol.ai.api.response.Response;
import cn.winddol.ai.domain.paperTools.service.caservice.PaperApplicationService;
import cn.winddol.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
@Slf4j
@RequestMapping("/api/v1/paper")
@RestController
public class PaperController implements IPaperController {
    @Resource
    private PaperApplicationService paperApplicationService;

    @PostMapping("/upload")
    public Response<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            Long paperId =  paperApplicationService.uploadAndParse(file);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data("文件已接收，Librarian 正在后台处理...,paperId为: %d".formatted(paperId))
                    .build();
        } catch (IOException e) {
            log.error("上传文件失败", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }

    }
}
