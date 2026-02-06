package cn.winddol.ai.domain.paperTools.service;

import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.external.SemanticScholarClient;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.RefMetadata;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.S2PaperResponse;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.model.entity.GlobalReferenceEntity;
import cn.winddol.ai.domain.paperTools.model.entity.SectionEntity;
import cn.winddol.ai.domain.paperTools.model.entity.SectionReferenceLinkEntity;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceEnum;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;
import cn.winddol.ai.types.common.utils.FingerprintUtils;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CitationEnrichmentService {
    @Resource
    private SemanticScholarClient s2Client;
    @Resource
    private IPaperRepository repository;
    @Resource
    private ISymbolExtractor extractor;
    /*
     富华引用文献
     */
    public void enrichPaperReferences(Long paperId) {
        List<ReferenceItem> pendingRefs = repository.selectPendingReferencesByPaperId(paperId);
        if (pendingRefs.isEmpty()) return;

        log.info("🚀 [Targeted Enrichment] Processing {} refs for Paper ID: {}", pendingRefs.size(), paperId);
        processReferences(pendingRefs);
    }
    public void enrichReferences() {
        List<ReferenceItem> pendingRefs = repository.selectReferences();
        log.info("🧹 [Global Cleanup] Processing {} leftover refs...", pendingRefs.size());
        processReferences(pendingRefs);
    }

    private void processReferences(List<ReferenceItem> pendingRefs) {
        for (ReferenceItem ref : pendingRefs) {
            int maxRetries = 5;
            int attempt = 0;
            boolean success = false;
            log.info("🚀 Starting enrichment for {} reference...", ref.getRawText());
            try {
                // --- 第一步：让 LLM 提取元数据 (解析 Raw Text) ---
                // 我们不再盲目搜原始文本，先分析出：标题、作者、年份
                RefMetadata meta = extractor.extractRefMetadata(ref.getRawText());
                log.info("MetaInfo {}", JSON.toJSONString(meta));
                if (meta == null || meta.getSearchString() == null) {
                    log.warn("⚠️ LLM could not parse metadata for: {}", ref.getRawText());
                    continue;
                }

                S2PaperResponse.S2PaperData matchedPaper = null;

                // --- 第二步：带重试机制的 API 搜索 ---
                while (attempt < maxRetries && !success) {
                    try {
                        attempt++;
                        // 使用 LLM 构造的优质搜索字符串进行搜索
                        matchedPaper = s2Client.searchPaper(meta.getSearchString());
                        success = true; // API 调用成功 (没报错)

                    } catch (HttpClientErrorException.TooManyRequests e) {
                        // 专门处理 429 限流
                        long waitTime = 2000L * attempt;
                        log.warn("⚠️ 429 Too Many Requests. Attempt {}. Sleeping {}ms", attempt, waitTime);
                        Thread.sleep(waitTime);
                    } catch (Exception e) {
                        log.error("❌ API Error on ref [{}]: {}", ref.getRefId(), e.getMessage());
                        break; // 其他错误不重试
                    }
                }
                String finalTitle = null;
                String finalAbstract = null;
                String s2Id = null;
                ReferenceEnum sourceType = ReferenceEnum.API;

                // --- 第三步：核心修复 - 严格匹配验证 ---
                if (success && isValidMatch(meta, matchedPaper)) {
                    // 只有校验通过才写入摘要和真实标题
                    finalTitle = matchedPaper.getTitle();
                    s2Id = matchedPaper.getPaperId();
                    finalAbstract = matchedPaper.getAbstractText();
                    Thread.sleep(2000);
                    log.info("✅ API Match: {} ({})", finalTitle, matchedPaper.getYear());
                }
                
                if (success && StringUtils.isBlank(finalAbstract)) {
                    log.info("API failed for [{}], switching to Context Generation...", ref.getRefId());
                    String syntheticAbstract = generateSyntheticAbstract(ref);
                    if (syntheticAbstract != null) {
                        finalTitle = StringUtils.isBlank(finalTitle) ? "Contextual Reference [" + ref.getRefId() + "]" : finalTitle;
                        finalAbstract = syntheticAbstract;
                        sourceType = ReferenceEnum.CONTEXT;
                    }
                }
                if(success){
                    if (finalAbstract != null) {
                        linkToGlobalReference(ref, meta, s2Id, finalTitle, finalAbstract, sourceType);
                    } else {
                        // 彻底找不到
                        ref.setTitle("NOT_FOUND");
                        repository.updateReferences(ref);
                    }
                }

            } catch (Exception e) {
                log.error("🔥 Critical error processing ref {}: ", ref.getRefId(), e);
            }
        }
    }


    public void linkToGlobalReference(ReferenceItem localRef, RefMetadata meta, String s2Id, String title, String abstractText, ReferenceEnum sourceType) {
        String fingerprint = FingerprintUtils.generateRefFingerprint(
                meta.getAuthorSurnames(), meta.getYear());
        GlobalReferenceEntity globalNode = null;
        if (s2Id != null) {
            globalNode = repository.selectGlobalReferenceByS2Id(s2Id);
        }
        if (globalNode == null) {
            globalNode = repository.selectGlobalReferenceByFingerprint(fingerprint);
        }
        Long Id = null;
        // 3. 维护全局节点
        if (globalNode == null) {
            // A. 创建新节点
            globalNode = new GlobalReferenceEntity();
            globalNode.setS2Id(s2Id);
            globalNode.setFingerprint(fingerprint);
            globalNode.setTitle(title);
            globalNode.setCitationCount(0);
            globalNode.setAbstractText(abstractText);
            globalNode.setSourceType(sourceType);
            Long internalId = repository.findPaperIdByFingerprint(fingerprint);
            globalNode.setLinkedPaperId(internalId);

            Id = repository.insertGlobalReference(globalNode);// 插入后返回 ID
            log.info("✨ Created New Global Node: {}", title);
        } else {
            // B. 节点已存在，检查是否需要“进化”摘要 (例如从 Synthetic 变为 API 真摘要)
            ReferenceEnum oldType = globalNode.getSourceType();
            String fusedAbstract = mergeKnowledge(
                    globalNode.getAbstractText(), oldType,
                    abstractText, sourceType
            );

            if (!fusedAbstract.equals(globalNode.getAbstractText())) {
                globalNode.setAbstractText(fusedAbstract);
                if (s2Id != null) globalNode.setS2Id(s2Id);
                Id = repository.updateGlobalReference(globalNode);
                log.info("🧠 Knowledge Fused for: {}", globalNode.getTitle());
            }

        }

        // 4. 更新本地 paper_references 表，建立外键关联
        localRef.setGlobalRefId(Id);
        localRef.setSourceType(sourceType);
        localRef.setTitle(globalNode.getTitle()); // 冗余一份标题方便查询
        localRef.setPaperAbstract(globalNode.getAbstractText()); // 冗余一份摘要

        repository.updateReferences(localRef);
    }

    private String mergeKnowledge(String oldAbs, ReferenceEnum oldType, String newAbs, ReferenceEnum newType) {
        // 情况 A：新旧都是合成的，需要 LLM 合并
        if (oldType == ReferenceEnum.CONTEXT && newType == ReferenceEnum.CONTEXT) {
            return extractor.fuseSyntheticAbstracts(oldAbs, newAbs);
        }

        // 情况 B：旧的是合成，新的是 API（真理降临）
        if (oldType == ReferenceEnum.CONTEXT && newType == ReferenceEnum.API) {
            return newAbs + "\n\n[Community Insight]: " + oldAbs.replace("Based on the context, ", "");
        }

        // 情况 C：旧的是 API，新的是合成（补充视角）
        if (oldType == ReferenceEnum.API && newType == ReferenceEnum.CONTEXT) {
            // 如果新信息已经在旧信息里体现了，可以不加。简单处理则直接追加。
            if (oldAbs.contains(newAbs.substring(0, Math.min(20, newAbs.length())))) return oldAbs;
            return oldAbs + "\n\n[Additional Context]: " + newAbs.replace("Based on the context, ", "");
        }

        return oldAbs;
    }

    private String generateSyntheticAbstract(ReferenceItem ref) {
        List<SectionReferenceLinkEntity> links = repository.selectReferenceLinks(ref.getPaperId(), ref.getRefId());
        if (links.isEmpty()) return null;

        List<String> contextSnippets = new ArrayList<>();
        for (SectionReferenceLinkEntity link : links) {
            SectionEntity section = repository.selectSectionById(link.getSectionId());
            if (section != null) {
                contextSnippets.add(extractWindow(section.getContent(), ref.getRefId()));
            }
        }
        return contextSnippets.isEmpty() ? null : extractor.summarizeReferenceContext(ref.getRefId(), contextSnippets);
    }

    /**
     * 严格验证逻辑：防止张冠李戴
     */
    private boolean isValidMatch(RefMetadata meta, S2PaperResponse.S2PaperData apiResult) {
        if (apiResult == null) {
            log.warn("🚨 Strict Author mismatch!");
            return false;
        }

        // 1. 勘误表/索引一票否决
        String title = apiResult.getTitle().toLowerCase();
        if (title.contains("erratum") || title.contains("correction") || title.contains("author index")) {
            log.warn("🚨 Strict Author mismatch!");
            return false;
        }

        // 2. 年份校验 (±1年)
        if (meta.getYear() != null && apiResult.getYear() != 0) {
            if (Math.abs(meta.getYear() - apiResult.getYear()) > 1) {
                log.warn("🚨 Strict Author mismatch!");
                return false;
            }
        }

        // 3. 【新增】作者姓氏校验 - 这是防撞衫最有效的办法
        List<String> expectedSurnames = meta.getAuthorSurnames();
        if (expectedSurnames != null && !expectedSurnames.isEmpty()) {
            if (apiResult.getAuthors() == null || apiResult.getAuthors().isEmpty()) {
                log.warn("🚨 Strict Author mismatch!");
                return false;
            }
            for (String expectedSurname : expectedSurnames) {
                String target = expectedSurname.toLowerCase().trim();

                // 在 API 返回的作者列表中寻找是否存在这个姓氏
                // 使用 contains 来匹配 (防止 "Van der Waals" vs "Waals" 的问题)
                boolean isFound = apiResult.getAuthors().stream()
                        .anyMatch(apiAuthor -> apiAuthor.getName().toLowerCase().contains(target));

                // 【一票否决】只要有一个提取出的作者没在 API 结果里找到，就认为匹配错误
                if (!isFound) {
                    log.warn("🚨 Strict Author mismatch! Missing author: '{}' in API result: {}",
                            expectedSurname,
                            apiResult.getAuthors().stream().map(S2PaperResponse.S2Author::getName).toList());
                    return false;
                }
            }
        }

        return true;
    }
    private static final Pattern CITATION_LOCATOR_PATTERN = Pattern.compile(
            "\\[([\\d\\s,\\-–—]+)\\]" +              // Group 1: []
                    "|<sup>([\\d\\s,\\-–—]+)</sup>" +        // Group 2: <sup>
                    "|\\$\\^\\{([\\d\\s,\\-–—]+)\\}\\$" +    // Group 3: $^{...}$
                    "|\\^\\{([\\d\\s,\\-–—]+)\\}"            // Group 4: ^{...}
    );
    /**
     * 智能提取引用上下文窗口
     */
    private String extractWindow(String content, String targetRefIndex) {
        Matcher matcher = CITATION_LOCATOR_PATTERN.matcher(content);

        while (matcher.find()) {
            String rawNumbers = getMatcherGroup(matcher);

            if (rawNumbers != null) {

                if (containsIndex(rawNumbers, targetRefIndex)) {

                    // 3. 找到了！以这个 matcher.start() 为中心截取
                    int foundIndex = matcher.start();
                    int start = Math.max(0, foundIndex - 300); // 前20字
                    int end = Math.min(content.length(), matcher.end() + 300); // 后20字

                    // 返回找到的第一个上下文 (通常第一个最重要)
                    // 如果你想更精细，可以收集所有出现的上下文拼起来
                    return "..." + content.substring(start, end) + "...";
                }
            }
        }

        return content.length() > 500 ? content.substring(0, 500) + "..." : content;
    }

    // 辅助方法：提取非空的捕获组
    private String getMatcherGroup(Matcher matcher) {
        if (matcher.group(1) != null) return matcher.group(1);
        if (matcher.group(2) != null) return matcher.group(2);
        if (matcher.group(3) != null) return matcher.group(3);
        if (matcher.group(4) != null) return matcher.group(4);
        return null;
    }

    // 辅助方法：判断 "10-15, 20" 是否包含 "12"
    private boolean containsIndex(String rawNumbers, String target) {
        try {
            int targetVal = Integer.parseInt(target.trim());
            String[] parts = rawNumbers.split("[,，]");

            for (String part : parts) {
                part = part.trim();
                if (part.contains("-") || part.contains("–") || part.contains("—")) {
                    // 处理范围
                    String[] limits = part.split("[-–—]");
                    if (limits.length == 2) {
                        int start = Integer.parseInt(limits[0].trim());
                        int end = Integer.parseInt(limits[1].trim());
                        if (targetVal >= start && targetVal <= end) return true;
                    }
                } else {
                    if (part.equals(target)) return true;
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }


}

