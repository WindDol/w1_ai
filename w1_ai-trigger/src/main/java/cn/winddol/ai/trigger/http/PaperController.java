package cn.winddol.ai.trigger.http;

import cn.winddol.ai.api.IPaperController;
import cn.winddol.ai.api.dto.PaperDTO;
import cn.winddol.ai.api.dto.PaperDetailDTO;
import cn.winddol.ai.api.dto.PaperIngestJobDTO;
import cn.winddol.ai.api.dto.SymbolDTO;
import cn.winddol.ai.api.response.Response;
import cn.winddol.ai.paper.domain.SymbolEntity;
import cn.winddol.ai.paper.domain.PaperDetailVO;
import cn.winddol.ai.paper.domain.PaperVO;
import cn.winddol.ai.paper.api.IPaperApplication;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.shared.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequestMapping("/api/v1/paper")
@RestController
public class PaperController implements IPaperController {
    private final IPaperApplication paperApplicationService;

    public PaperController(IPaperApplication paperApplicationService) {
        this.paperApplicationService = paperApplicationService;
    }

    @Override
    @PostMapping("/upload")
    public ResponseEntity<Response<PaperIngestJobDTO>> upload(@RequestParam("file") MultipartFile file) {
        try {
            PaperIngestJob job = paperApplicationService.submit(file);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Response.<PaperIngestJobDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(toDto(job))
                    .build());
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return ResponseEntity.badRequest().body(Response.<PaperIngestJobDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build());
        }
    }

    @Override
    @GetMapping("/ingestions/{jobId}")
    public ResponseEntity<Response<PaperIngestJobDTO>> getIngestJob(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(success(paperApplicationService.getIngestJob(jobId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(failure(e));
        }
    }

    @Override
    @PostMapping("/ingestions/{jobId}/retry")
    public ResponseEntity<Response<PaperIngestJobDTO>> retryIngestJob(@PathVariable String jobId) {
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(success(paperApplicationService.retry(jobId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(failure(e));
        }
    }

    @Override
    @PostMapping("/ingestions/{jobId}/rerun")
    public ResponseEntity<Response<PaperIngestJobDTO>> rerunIngestJob(
            @PathVariable String jobId,
            @RequestParam("stage") String stage) {
        try {
            PaperIngestStage requestedStage = PaperIngestStage.valueOf(stage.trim().toUpperCase());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(success(paperApplicationService.rerunFrom(jobId, requestedStage)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(failure(e));
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

    private Response<PaperIngestJobDTO> success(PaperIngestJob job) {
        return Response.<PaperIngestJobDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toDto(job))
                .build();
    }

    private Response<PaperIngestJobDTO> failure(Exception error) {
        return Response.<PaperIngestJobDTO>builder()
                .code(ResponseCode.UN_ERROR.getCode())
                .info(error.getMessage())
                .build();
    }

    private PaperIngestJobDTO toDto(PaperIngestJob job) {
        return PaperIngestJobDTO.builder()
                .jobId(job.getId())
                .paperId(job.getPaperId())
                .originalFilename(job.getOriginalFilename())
                .fileSha256(job.getFileSha256())
                .status(job.getStatus() == null ? null : job.getStatus().name())
                .currentStage(job.getCurrentStage() == null ? null : job.getCurrentStage().name())
                .failedStage(job.getFailedStage() == null ? null : job.getFailedStage().name())
                .errorCode(job.getErrorCode())
                .errorMessage(job.getErrorMessage())
                .attemptCount(job.getAttemptCount())
                .parserType(job.getParserType())
                .parserVersion(job.getParserVersion())
                .normalizerVersion(job.getNormalizerVersion())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

}
