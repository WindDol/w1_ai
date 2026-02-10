package cn.winddol.ai.trigger.http;

import cn.winddol.ai.api.IPaperController;
import cn.winddol.ai.api.dto.PaperDTO;
import cn.winddol.ai.api.dto.PaperDetailDTO;
import cn.winddol.ai.api.dto.SymbolDTO;
import cn.winddol.ai.api.response.Response;
import cn.winddol.ai.domain.paperTools.model.entity.SymbolEntity;
import cn.winddol.ai.domain.paperTools.model.valobj.PaperDetailVO;
import cn.winddol.ai.domain.paperTools.model.valobj.PaperVO;
import cn.winddol.ai.domain.paperTools.service.IPaperApplication;
import cn.winddol.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequestMapping("/api/v1/paper")
@RestController
public class PaperController implements IPaperController {
    @Resource
    private IPaperApplication paperApplicationService;
    @Override
    @PostMapping("/upload")
    public Response<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            Long paperId =  paperApplicationService.uploadAndParse(file);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data("文件已接收，Librarian 正在后台处理...,paperId为: %d".formatted(paperId))
                    .build();
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
    @Override
    @GetMapping("/list")
    public Response<List<PaperDTO>> listPapers() {
        try {
            List<PaperVO> paperVOS = paperApplicationService.listAllPapers();
            List<PaperDTO> list = paperVOS.stream().map(s -> PaperDTO.builder().id(s.getId())
                    .title(s.getTitle())
                    .status(s.getStatus())
                    .fingerprint(s.getFingerprint())
                    .createdAt(s.getCreatedAt()).build()).toList();
            return Response.<List<PaperDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(list)
                    .build();
        }catch (Exception e) {
            log.error("查询论文失败", e);
            return Response.<List<PaperDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
    @Override
    @GetMapping("/{paperId}/details")
    public Response<PaperDetailDTO> getPaperDetails(@PathVariable Long paperId) {
        try {
            PaperDetailVO paperDetail = paperApplicationService.getPaperDetails(paperId);
            List<SymbolEntity> keySymbols = paperDetail.getKeySymbols();
            List<SymbolDTO> list = keySymbols.stream().map(s ->
                    SymbolDTO.builder().id(s.getId()).paperId(s.getPaperId()).symbol(s.getSymbol())
                            .latex(s.getLatex()).description(s.getDescription()).definitionFormula(s.getDefinitionFormula()).build()
            ).toList();
            PaperDetailDTO paperDetailDTO = PaperDetailDTO.builder().id(paperDetail.getId())
                    .title(paperDetail.getTitle()).abstractText(paperDetail.getAbstractText())
                    .noveltyAssessment(paperDetail.getNoveltyAssessment())
                    .symbolCount(paperDetail.getSymbolCount()).referenceCount(paperDetail.getReferenceCount())
                    .keySymbols(list).relatedPaperTitles(paperDetail.getRelatedPaperTitles()).build();
            return Response.<PaperDetailDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(paperDetailDTO)
                    .build();
        }catch (Exception e) {
            log.error("查询论文失败", e);
            return Response.<PaperDetailDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }


}
