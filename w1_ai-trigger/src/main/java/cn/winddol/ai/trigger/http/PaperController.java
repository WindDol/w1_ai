package cn.winddol.ai.trigger.http;

import cn.winddol.ai.api.IPaperController;
import cn.winddol.ai.api.dto.PaperDTO;
import cn.winddol.ai.api.dto.PaperDetailDTO;
import cn.winddol.ai.api.dto.PaperIngestJobDTO;
import cn.winddol.ai.api.dto.SymbolDTO;
import cn.winddol.ai.api.dto.workspace.ArtifactContentDTO;
import cn.winddol.ai.api.dto.workspace.ArtifactSummaryDTO;
import cn.winddol.ai.api.dto.workspace.IngestStageRunDTO;
import cn.winddol.ai.api.dto.workspace.IngestionWorkspaceDTO;
import cn.winddol.ai.api.dto.workspace.OutlineNodeDTO;
import cn.winddol.ai.api.dto.workspace.PaperRelationDTO;
import cn.winddol.ai.api.dto.workspace.PaperWorkspaceDTO;
import cn.winddol.ai.api.dto.workspace.ReferenceDTO;
import cn.winddol.ai.api.dto.workspace.SectionWorkspaceDTO;
import cn.winddol.ai.api.response.Response;
import cn.winddol.ai.paper.domain.KnowledgeRelationEntity;
import cn.winddol.ai.paper.domain.OutlineNode;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.paper.domain.SymbolEntity;
import cn.winddol.ai.paper.domain.PaperDetailVO;
import cn.winddol.ai.paper.domain.PaperVO;
import cn.winddol.ai.paper.api.IPaperApplication;
import cn.winddol.ai.paper.domain.ingest.PaperIngestJob;
import cn.winddol.ai.paper.domain.ingest.PaperIngestStage;
import cn.winddol.ai.paper.domain.workspace.ArtifactContent;
import cn.winddol.ai.paper.domain.workspace.ArtifactType;
import cn.winddol.ai.paper.domain.workspace.IngestionWorkspaceView;
import cn.winddol.ai.paper.domain.workspace.PaperWorkspaceView;
import cn.winddol.ai.paper.domain.workspace.SectionWorkspaceView;
import cn.winddol.ai.shared.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

    /** 返回阅读论文所需的 Outline、符号、引用和关系数据。 */
    @Override
    @GetMapping("/{paperId}/reading")
    public ResponseEntity<Response<PaperWorkspaceDTO>> getPaperReading(@PathVariable Long paperId) {
        try {
            return ResponseEntity.ok(responseSuccess(toPaperWorkspace(paperApplicationService.getPaperReading(paperId))));
        } catch (Exception error) {
            log.warn("Unable to read paper {}", paperId, error);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFailure(error));
        }
    }

    /** 返回指定章节的正文、Outline 路径、局部符号和引用。 */
    @Override
    @GetMapping("/sections/{sectionId}")
    public ResponseEntity<Response<SectionWorkspaceDTO>> getSection(@PathVariable String sectionId) {
        try {
            return ResponseEntity.ok(responseSuccess(toSectionWorkspace(paperApplicationService.getSectionReading(sectionId))));
        } catch (Exception error) {
            log.warn("Unable to read section {}", sectionId, error);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFailure(error));
        }
    }

    /** 返回原有摄取任务的阶段执行记录与可查看产物。 */
    @Override
    @GetMapping("/ingestions/{jobId}/details")
    public ResponseEntity<Response<IngestionWorkspaceDTO>> getIngestionDetails(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(responseSuccess(toIngestionWorkspace(paperApplicationService.getIngestionDetails(jobId))));
        } catch (Exception error) {
            log.warn("Unable to inspect ingestion {}", jobId, error);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFailure(error));
        }
    }

    /** 读取原有摄取任务保存的解析产物，不接受客户端文件路径。 */
    @Override
    @GetMapping("/ingestions/{jobId}/artifacts/{type}")
    public ResponseEntity<Response<ArtifactContentDTO>> getIngestionArtifact(
            @PathVariable String jobId,
            @PathVariable String type) {
        try {
            ArtifactType artifactType = ArtifactType.valueOf(type.trim().toUpperCase(Locale.ROOT));
            ArtifactContent artifact = paperApplicationService.readIngestionArtifact(jobId, artifactType);
            return ResponseEntity.ok(responseSuccess(ArtifactContentDTO.builder()
                    .type(artifact.type().name())
                    .label(artifact.label())
                    .content(artifact.content())
                    .truncated(artifact.truncated())
                    .build()));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(responseFailure(error));
        } catch (Exception error) {
            log.warn("Unable to read {} artifact for ingestion {}", type, jobId, error);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFailure(error));
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
                .fileSize(job.getFileSize())
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
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    private PaperWorkspaceDTO toPaperWorkspace(PaperWorkspaceView view) {
        var paper = view.getPaper();
        return PaperWorkspaceDTO.builder()
                .id(paper.getId()).title(paper.getTitle()).abstractText(paper.getAbstractText())
                .status(view.getStatus()).year(paper.getYear()).createdAt(paper.getCreatedAt())
                .latestJobId(view.getLatestJobId()).outline(mapOutline(paper.getOutline()))
                .symbols(view.getSymbols().stream().map(this::toSymbol).toList())
                .references(view.getReferences().stream().map(this::toReference).toList())
                .relations(view.getRelations().stream().map(this::toRelation).toList())
                .build();
    }

    private SectionWorkspaceDTO toSectionWorkspace(SectionWorkspaceView view) {
        var section = view.getSection();
        return SectionWorkspaceDTO.builder()
                .id(section.getId()).paperId(section.getPaperId()).paperTitle(view.getPaperTitle())
                .title(section.getHeader()).parentId(section.getParentId()).index(section.getIdx())
                .headingPath(view.getHeadingPath()).content(section.getContent())
                .symbols(view.getSymbols().stream().map(this::toSymbol).toList())
                .references(view.getReferences().stream().map(this::toReference).toList())
                .build();
    }

    private IngestionWorkspaceDTO toIngestionWorkspace(IngestionWorkspaceView view) {
        return IngestionWorkspaceDTO.builder()
                .job(toDto(view.getJob()))
                .stageRuns(view.getStageRuns().stream().map(run -> IngestStageRunDTO.builder()
                        .id(run.getId()).stage(name(run.getStage())).attempt(run.getAttempt())
                        .status(name(run.getStatus())).errorCode(run.getErrorCode())
                        .errorMessage(run.getErrorMessage()).startedAt(run.getStartedAt())
                        .finishedAt(run.getFinishedAt()).build()).toList())
                .artifacts(view.getArtifacts().stream().map(artifact -> ArtifactSummaryDTO.builder()
                        .type(artifact.type().name()).label(artifact.label())
                        .available(artifact.available()).build()).toList())
                .build();
    }

    private List<OutlineNodeDTO> mapOutline(List<OutlineNode> nodes) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream().map(node -> OutlineNodeDTO.builder()
                .id(node.getId()).title(node.getTitle()).level(node.getLevel())
                .children(mapOutline(node.getChildren())).build()).toList();
    }

    private SymbolDTO toSymbol(SymbolEntity symbol) {
        return SymbolDTO.builder().id(symbol.getId()).paperId(symbol.getPaperId())
                .symbol(symbol.getSymbol()).latex(symbol.getLatex())
                .description(symbol.getDescription()).definitionFormula(symbol.getDefinitionFormula())
                .build();
    }

    private ReferenceDTO toReference(ReferenceItem reference) {
        return ReferenceDTO.builder().id(reference.getId()).refId(reference.getRefId())
                .rawText(reference.getRawText()).title(reference.getTitle())
                .abstractText(reference.getPaperAbstract()).linkedPaperId(reference.getLinkedPaperId())
                .citationCount(reference.getCitationCount()).build();
    }

    private PaperRelationDTO toRelation(KnowledgeRelationEntity relation) {
        return PaperRelationDTO.builder().relatedId(relation.getRelatedId())
                .relatedTitle(relation.getRelatedTitle()).type(relation.getType())
                .description(relation.getDescription()).direction(relation.getDirection())
                .confidence(relation.getConfidence()).auditStatus(relation.getAuditStatus())
                .supportingEvidence(relation.getSupportingEvidence())
                .conflictingEvidence(relation.getConflictingEvidence())
                .auditVersion(relation.getAuditVersion()).modelName(relation.getModelName())
                .retrievalVersion(relation.getRetrievalVersion()).build();
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private <T> Response<T> responseSuccess(T data) {
        return Response.<T>builder().code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo()).data(data).build();
    }

    private <T> Response<T> responseFailure(Exception error) {
        return Response.<T>builder().code(ResponseCode.UN_ERROR.getCode())
                .info(error.getMessage()).build();
    }

}
