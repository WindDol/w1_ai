package cn.winddol.ai.domain.paperTools.service;

import cn.winddol.ai.domain.paperTools.adapter.ai.ISymbolExtractor;
import cn.winddol.ai.domain.paperTools.adapter.external.SemanticScholarClient;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.RefMetadata;
import cn.winddol.ai.domain.paperTools.adapter.external.dto.S2PaperResponse;
import cn.winddol.ai.domain.paperTools.adapter.repository.IPaperRepository;
import cn.winddol.ai.domain.paperTools.model.entity.SectionEntity;
import cn.winddol.ai.domain.paperTools.model.entity.SectionReferenceLinkEntity;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceEnum;
import cn.winddol.ai.domain.paperTools.model.valobj.ReferenceItem;
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
    public void enrichReferences() {
        List<ReferenceItem> pendingRefs = repository.selectReferences();
        log.info("🚀 Starting enrichment for {} references...", pendingRefs.size());

        for (ReferenceItem ref : pendingRefs) {
            boolean enriched = false;
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

                // --- 第三步：核心修复 - 严格匹配验证 ---
                if (success && isValidMatch(meta, matchedPaper)) {
                    // 只有校验通过才写入摘要和真实标题
                    ref.setSourceType(ReferenceEnum.API);
                    ref.setTitle(matchedPaper.getTitle());
                    if(!StringUtils.isBlank(matchedPaper.getAbstractText())){
                        ref.setPaperAbstract(matchedPaper.getAbstractText());
                        enriched = true;
                    }
                    Thread.sleep(2000);
                    log.info("✅ [Success] Ref {}: {} ({})",
                            ref.getRefId(), matchedPaper.getTitle(), matchedPaper.getYear());
                } else {
                    if(matchedPaper != null || success){
                        ref.setTitle("NOT_FOUND");
                    }
                    String reason = (matchedPaper == null) ? "No result from API" :
                            "Year mismatch (Expected: " + meta.getYear() + ", Found: " + matchedPaper.getYear() + ")";
                    log.warn("❌ [Discarded] Ref {}: {}", ref.getRefId(), reason);

                }
                if(!enriched && !StringUtils.isBlank(ref.getTitle())){
                    log.info("API failed for abstract [{}], switching to Context Generation... {}", ref.getRefId(),ref.getTitle());
                    List<SectionReferenceLinkEntity> links = repository.selectReferenceLinks(ref.getPaperId(),ref.getRefId());
                    if(!links.isEmpty()){
                        List<String> contextSnippets = new ArrayList<>();
                        for(SectionReferenceLinkEntity link:links){
                            SectionEntity section = repository.selectSectionById(link.getSectionId());
                            if (section != null) {
                                // 3. 截取窗口 (前后40 字符)
                                String snippet = extractWindow(section.getContent(), ref.getRefId());
                                contextSnippets.add(snippet);
                            }
                        }
                        if (!contextSnippets.isEmpty()) {
                            String syntheticAbstract = extractor.summarizeReferenceContext(
                                    ref.getRefId(), contextSnippets
                            );
                            if(StringUtils.isBlank(ref.getTitle()) || ref.getTitle().equals("NOT_FOUND")){
                                ref.setTitle("Contextual Reference [" + ref.getRefId() + "]");
                            }
                            ref.setPaperAbstract(syntheticAbstract);

                            log.info("✅ Context Generated for [{}]", ref.getRefId());
                            enriched = true;
                        }
                    }
                    ref.setSourceType(ReferenceEnum.CONTEXT);

                }
                // 4. 持久化结果
                repository.updateReferences(ref);

            } catch (Exception e) {
                log.error("🔥 Critical error processing ref {}: ", ref.getRefId(), e);
            }
        }
    }

    /**
     * 严格验证逻辑：防止张冠李戴
     */
    private boolean isValidMatch(RefMetadata meta, S2PaperResponse.S2PaperData apiResult) {
        if (apiResult == null) return false;

        // 1. 勘误表/索引一票否决
        String title = apiResult.getTitle().toLowerCase();
        if (title.contains("erratum") || title.contains("correction") || title.contains("author index")) {
            return false;
        }

        // 2. 年份校验 (±1年)
        if (meta.getYear() != null && apiResult.getYear() != 0) {
            if (Math.abs(meta.getYear() - apiResult.getYear()) > 1) return false;
        }

        // 3. 【新增】作者姓氏校验 - 这是防撞衫最有效的办法
        List<String> expectedSurnames = meta.getAuthorSurnames();
        if (expectedSurnames != null && !expectedSurnames.isEmpty()) {
            if (apiResult.getAuthors() == null || apiResult.getAuthors().isEmpty()) {
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

