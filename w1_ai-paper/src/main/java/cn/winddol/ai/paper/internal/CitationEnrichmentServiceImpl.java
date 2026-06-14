package cn.winddol.ai.paper.internal;

import cn.winddol.ai.paper.api.ISymbolExtractor;
import cn.winddol.ai.paper.api.ISemanticScholar;
import cn.winddol.ai.paper.api.IFingerprintUtils;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.GlobalReferenceEntity;
import cn.winddol.ai.paper.domain.RefMetadata;
import cn.winddol.ai.paper.domain.S2Author;
import cn.winddol.ai.paper.domain.S2PaperData;
import cn.winddol.ai.paper.domain.SectionEntity;
import cn.winddol.ai.paper.domain.SectionReferenceLinkEntity;
import cn.winddol.ai.paper.domain.ReferenceEnum;
import cn.winddol.ai.paper.domain.ReferenceItem;
import cn.winddol.ai.shared.api.ICitationEnrichmentService;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CitationEnrichmentServiceImpl implements ICitationEnrichmentService {

    @Resource
    private ISemanticScholar s2Client;
    @Resource
    private IPaperRepository repository;
    @Resource
    private ISymbolExtractor extractor;
    @Resource
    private IFingerprintUtils fingerprintUtils;

    @Override
    public void enrichPaperReferences(Long paperId) {
        List<ReferenceItem> pendingRefs = repository.selectPendingReferencesByPaperId(paperId);
        if (pendingRefs.isEmpty()) {
            return;
        }

        log.info("[Targeted Enrichment] Processing {} refs for Paper ID: {}", pendingRefs.size(), paperId);
        processReferences(pendingRefs);
    }

    @Override
    public void enrichReferences() {
        List<ReferenceItem> pendingRefs = repository.selectReferences();
        log.info("[Global Cleanup] Processing {} leftover refs...", pendingRefs.size());
        processReferences(pendingRefs);
    }

    private void processReferences(List<ReferenceItem> pendingRefs) {
        for (ReferenceItem ref : pendingRefs) {
            int maxRetries = 5;
            int attempt = 0;
            boolean success = false;
            log.info("Starting enrichment for {} reference...", ref.getRawText());
            try {
                RefMetadata meta = extractor.extractRefMetadata(ref.getRawText());
                log.info("MetaInfo {}", JSON.toJSONString(meta));
                if (meta == null || meta.getSearchString() == null) {
                    log.warn("LLM could not parse metadata for: {}", ref.getRawText());
                    continue;
                }

                S2PaperData matchedPaper = null;
                while (attempt < maxRetries && !success) {
                    try {
                        attempt++;
                        matchedPaper = s2Client.searchPaper(meta.getSearchString());
                        success = true;
                    } catch (HttpClientErrorException.TooManyRequests e) {
                        long waitTime = 2000L * attempt;
                        log.warn("429 Too Many Requests. Attempt {}. Sleeping {}ms", attempt, waitTime);
                        Thread.sleep(waitTime);
                    } catch (Exception e) {
                        log.error("API Error on ref [{}]: {}", ref.getRefId(), e.getMessage());
                        break;
                    }
                }

                String finalTitle = null;
                String finalAbstract = null;
                String s2Id = null;
                ReferenceEnum sourceType = ReferenceEnum.API;

                if (success && isValidMatch(meta, matchedPaper)) {
                    finalTitle = matchedPaper.getTitle();
                    s2Id = matchedPaper.getPaperId();
                    finalAbstract = matchedPaper.getAbstractText();
                    Thread.sleep(2000);
                    log.info("API Match: {} ({})", finalTitle, matchedPaper.getYear());
                }

                if (success && StringUtils.isBlank(finalAbstract)) {
                    log.info("API failed for [{}], switching to Context Generation...", ref.getRefId());
                    String syntheticAbstract = generateSyntheticAbstract(ref);
                    if (syntheticAbstract != null) {
                        finalTitle = StringUtils.isBlank(finalTitle)
                                ? "Contextual Reference [" + ref.getRefId() + "]"
                                : finalTitle;
                        finalAbstract = syntheticAbstract;
                        sourceType = ReferenceEnum.CONTEXT;
                    }
                }

                if (success) {
                    if (finalAbstract != null) {
                        linkToGlobalReference(ref, meta, s2Id, finalTitle, finalAbstract, sourceType);
                    } else {
                        ref.setTitle("NOT_FOUND");
                        repository.updateReferences(ref);
                    }
                }
            } catch (Exception e) {
                log.error("Critical error processing ref {}: ", ref.getRefId(), e);
            }
        }
    }

    public void linkToGlobalReference(ReferenceItem localRef,
                                      RefMetadata meta,
                                      String s2Id,
                                      String title,
                                      String abstractText,
                                      ReferenceEnum sourceType) {
        String fingerprint = fingerprintUtils.generateRefFingerprint(meta.getAuthorSurnames(), meta.getYear());
        GlobalReferenceEntity globalNode = null;
        if (s2Id != null) {
            globalNode = repository.selectGlobalReferenceByS2Id(s2Id);
        }
        if (globalNode == null) {
            globalNode = repository.selectGlobalReferenceByFingerprint(fingerprint);
        }

        Long id = null;
        if (globalNode == null) {
            globalNode = new GlobalReferenceEntity();
            globalNode.setS2Id(s2Id);
            globalNode.setFingerprint(fingerprint);
            globalNode.setTitle(title);
            globalNode.setCitationCount(0);
            globalNode.setAbstractText(abstractText);
            globalNode.setSourceType(sourceType);
            Long internalId = repository.findPaperIdByFingerprint(fingerprint);
            globalNode.setLinkedPaperId(internalId);

            id = repository.insertGlobalReference(globalNode);
            log.info("Created New Global Node: {}", title);
        } else {
            id = globalNode.getId();
            ReferenceEnum oldType = globalNode.getSourceType();
            String fusedAbstract = mergeKnowledge(globalNode.getAbstractText(), oldType, abstractText, sourceType);

            if (!fusedAbstract.equals(globalNode.getAbstractText())) {
                globalNode.setAbstractText(fusedAbstract);
                if (s2Id != null) {
                    globalNode.setS2Id(s2Id);
                }
                id = repository.updateGlobalReference(globalNode);
                log.info("Knowledge fused for: {}", globalNode.getTitle());
            }
        }

        localRef.setGlobalRefId(id);
        localRef.setSourceType(sourceType);
        localRef.setTitle(globalNode.getTitle());
        localRef.setPaperAbstract(globalNode.getAbstractText());
        repository.updateReferences(localRef);
    }

    private String mergeKnowledge(String oldAbs, ReferenceEnum oldType, String newAbs, ReferenceEnum newType) {
        if (oldType == ReferenceEnum.CONTEXT && newType == ReferenceEnum.CONTEXT) {
            return extractor.fuseSyntheticAbstracts(oldAbs, newAbs);
        }
        if (oldType == ReferenceEnum.CONTEXT && newType == ReferenceEnum.API) {
            return newAbs + "\n\n[Community Insight]: " + oldAbs.replace("Based on the context, ", "");
        }
        if (oldType == ReferenceEnum.API && newType == ReferenceEnum.CONTEXT) {
            if (oldAbs.contains(newAbs.substring(0, Math.min(20, newAbs.length())))) {
                return oldAbs;
            }
            return oldAbs + "\n\n[Additional Context]: " + newAbs.replace("Based on the context, ", "");
        }
        return oldAbs;
    }

    private String generateSyntheticAbstract(ReferenceItem ref) {
        List<SectionReferenceLinkEntity> links = repository.selectReferenceLinks(ref.getPaperId(), ref.getRefId());
        if (links.isEmpty()) {
            return null;
        }

        List<String> contextSnippets = new ArrayList<>();
        for (SectionReferenceLinkEntity link : links) {
            SectionEntity section = repository.selectSectionById(link.getSectionId());
            if (section != null) {
                contextSnippets.add(extractWindow(section.getContent(), ref.getRefId()));
            }
        }
        return contextSnippets.isEmpty()
                ? null
                : extractor.summarizeReferenceContext(ref.getRefId(), contextSnippets);
    }

    private boolean isValidMatch(RefMetadata meta, S2PaperData apiResult) {
        if (apiResult == null) {
            log.warn("Strict match failed: empty API result.");
            return false;
        }

        String title = apiResult.getTitle().toLowerCase();
        if (title.contains("erratum") || title.contains("correction") || title.contains("author index")) {
            log.warn("Strict match failed: correction/index result.");
            return false;
        }

        if (meta.getYear() != null && apiResult.getYear() != 0
                && Math.abs(meta.getYear() - apiResult.getYear()) > 1) {
            log.warn("Strict match failed: year mismatch.");
            return false;
        }

        List<String> expectedSurnames = meta.getAuthorSurnames();
        if (expectedSurnames != null && !expectedSurnames.isEmpty()) {
            if (apiResult.getAuthors() == null || apiResult.getAuthors().isEmpty()) {
                log.warn("Strict match failed: missing authors.");
                return false;
            }
            for (String expectedSurname : expectedSurnames) {
                String target = expectedSurname.toLowerCase().trim();
                boolean isFound = apiResult.getAuthors().stream()
                        .anyMatch(apiAuthor -> apiAuthor.getName().toLowerCase().contains(target));
                if (!isFound) {
                    log.warn("Strict match failed. Missing author: '{}' in API result: {}",
                            expectedSurname,
                            apiResult.getAuthors().stream().map(S2Author::getName).toList());
                    return false;
                }
            }
        }

        return true;
    }

    private static final Pattern CITATION_LOCATOR_PATTERN = Pattern.compile(
            "\\[([\\d\\s,\\-–—]+)]"
                    + "|<sup>([\\d\\s,\\-–—]+)</sup>"
                    + "|\\$\\^\\{([\\d\\s,\\-–—]+)}\\$"
                    + "|\\^\\{([\\d\\s,\\-–—]+)}"
    );

    private String extractWindow(String content, String targetRefIndex) {
        Matcher matcher = CITATION_LOCATOR_PATTERN.matcher(content);
        while (matcher.find()) {
            String rawNumbers = getMatcherGroup(matcher);
            if (rawNumbers != null && containsIndex(rawNumbers, targetRefIndex)) {
                int foundIndex = matcher.start();
                int start = Math.max(0, foundIndex - 300);
                int end = Math.min(content.length(), matcher.end() + 300);
                return "..." + content.substring(start, end) + "...";
            }
        }
        return content.length() > 500 ? content.substring(0, 500) + "..." : content;
    }

    private String getMatcherGroup(Matcher matcher) {
        if (matcher.group(1) != null) {
            return matcher.group(1);
        }
        if (matcher.group(2) != null) {
            return matcher.group(2);
        }
        if (matcher.group(3) != null) {
            return matcher.group(3);
        }
        if (matcher.group(4) != null) {
            return matcher.group(4);
        }
        return null;
    }

    private boolean containsIndex(String rawNumbers, String target) {
        try {
            int targetVal = Integer.parseInt(target.trim());
            String[] parts = rawNumbers.split("[,，]");
            for (String part : parts) {
                part = part.trim();
                if (part.contains("-") || part.contains("–") || part.contains("—")) {
                    String[] limits = part.split("[-–—]");
                    if (limits.length == 2) {
                        int start = Integer.parseInt(limits[0].trim());
                        int end = Integer.parseInt(limits[1].trim());
                        if (targetVal >= start && targetVal <= end) {
                            return true;
                        }
                    }
                } else if (part.equals(target)) {
                    return true;
                }
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        return false;
    }
}
