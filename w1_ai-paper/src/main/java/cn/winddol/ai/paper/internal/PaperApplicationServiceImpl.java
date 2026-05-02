package cn.winddol.ai.paper.internal;

import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.parser.IPaperParser;
import cn.winddol.ai.domain.paperTools.adapter.tools.IFingerprintUtils;
import cn.winddol.ai.paper.api.IFileStorageService;
import cn.winddol.ai.paper.api.IPaperApplication;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.*;
import cn.winddol.ai.paper.event.PaperIngestedEvent;
import cn.winddol.ai.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaperApplicationServiceImpl implements IPaperApplication {

    private final IFileStorageService fileStorageService;
    private final IPaperParser parser;
    private final ISymbolExtractor symbolExtractor;
    private final IPaperRepository paperRepository;
    private final IFingerprintUtils fingerprintUtils;
    private final ApplicationEventPublisher eventPublisher;

    public PaperApplicationServiceImpl(IFileStorageService fileStorageService,
                                       IPaperParser parser,
                                       ISymbolExtractor symbolExtractor,
                                       IPaperRepository paperRepository,
                                       IFingerprintUtils fingerprintUtils,
                                       ApplicationEventPublisher eventPublisher) {
        this.fileStorageService = fileStorageService;
        this.parser = parser;
        this.symbolExtractor = symbolExtractor;
        this.paperRepository = paperRepository;
        this.fingerprintUtils = fingerprintUtils;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Long uploadAndParse(MultipartFile file) throws IOException {
        File tempFile = fileStorageService.saveTempFile(file);
        try {
            String markdown = parser.parsePdfToMarkdown(tempFile.getAbsolutePath());
            List<SectionPO> pos = parser.parse(markdown);
            String title = parser.extractTitle(markdown);
            String abstractText = parser.extractAbstract(pos);

            StringBuilder headerContent = new StringBuilder(pos.get(0).getContent());
            for (int i = 0; i < pos.size(); i++) {
                if (pos.get(i).getHeader().equals(title)) {
                    headerContent.append(pos.get(i).getContent());
                    headerContent.append(pos.get(i + 1).getContent());
                }
            }
            RefMetadata paperMeta = symbolExtractor.extractRefMetadata(headerContent.toString());
            if (paperMeta == null || paperMeta.getAuthorSurnames().isEmpty() || paperMeta.getYear() == 0) {
                log.error("Metadata extraction failed. Aborting ingestion to prevent library pollution.");
                return null;
            }
            String fingerprint = fingerprintUtils.generateRefFingerprint(
                    paperMeta.getAuthorSurnames(),
                    paperMeta.getYear()
            );

            Long paperId = paperRepository.saveFullPaper(title, pos, fingerprint, abstractText, paperMeta.getYear());
            paperRepository.updateStatus(paperId, "PARSED");

            // Publish event instead of directly invoking Librarian
            eventPublisher.publishEvent(new PaperIngestedEvent(paperId, title));

            return paperId;
        } catch (AppException e) {
            throw e;
        } finally {
            if (tempFile.exists()) {
                fileStorageService.deleteTempFile(tempFile);
            }
        }
    }

    @Override
    public List<PaperVO> listAllPapers() {
        return paperRepository.listAllPapers();
    }

    @Override
    public PaperDetailVO getPaperDetails(Long paperId) {
        PaperEntity paper = paperRepository.getPaperDetailsById(paperId);
        List<SymbolEntity> symbols = paperRepository.findByPaperId(paperId);
        List<PaperRelationDTO> relations = paperRepository.findRelationsByPaperId(paperId);
        List<ReferenceItem> referenceItemList = paperRepository.selectReferencesByPaperId(paperId);

        StringBuilder novelty = new StringBuilder();

        if (!relations.isEmpty()) {
            novelty.append("**Inter-paper relations from the Librarian's audit:**\n\n");

            for (PaperRelationDTO rel : relations) {
                String type = rel.getType();
                String otherPaperInfo = String.format("Paper [%d] (%s)", rel.getRelatedId(), rel.getRelatedTitle());

                if ("OUTGOING".equals(rel.getDirection())) {
                    String actionPhrase = getActivePhrasing(type);
                    novelty.append(String.format("- This paper **%s** %s.\n  *Reason: %s*\n",
                            actionPhrase, otherPaperInfo, rel.getDescription()));
                } else {
                    String passivePhrase = getPassivePhrasing(type);
                    novelty.append(String.format("- This paper **%s** %s.\n  *Note: %s*\n",
                            passivePhrase, otherPaperInfo, rel.getDescription()));
                }
            }
        } else {
            novelty.append("The Librarian found no direct or specific relations with other papers in the library yet.");
        }

        PaperDetailVO vo = PaperDetailVO.builder()
                .id(paper.getId())
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .noveltyAssessment(novelty.toString())
                .symbolCount(symbols.size())
                .referenceCount(referenceItemList.size())
                .keySymbols(symbols.stream().limit(5).collect(Collectors.toList()))
                .build();

        return vo;
    }

    private String getActivePhrasing(String type) {
        if (type == null) return "relates to";
        return switch (type.toUpperCase()) {
            case "FOUNDATIONAL" -> "serves as a FOUNDATIONAL BASIS for";
            case "EXTENDS"      -> "EXTENDS the work of";
            case "CONTRADICTS"  -> "CONTRADICTS or REFUTES";
            case "SUPPORT"      -> "SUPPORTS the findings of";
            case "ALTERNATIVE"  -> "presents an ALTERNATIVE approach to";
            default             -> "has a relation (" + type + ") with";
        };
    }

    private String getPassivePhrasing(String type) {
        if (type == null) return "is related to";
        return switch (type.toUpperCase()) {
            case "FOUNDATIONAL" -> "is BUILT UPON the foundation of";
            case "EXTENDS"      -> "is EXTENDED by";
            case "CONTRADICTS"  -> "is CONTRADICTED by";
            case "SUPPORT"      -> "is SUPPORTED by";
            case "ALTERNATIVE"  -> "is considered an ALTERNATIVE to";
            default             -> "is referenced (" + type + ") by";
        };
    }
}
