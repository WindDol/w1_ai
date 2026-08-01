package cn.winddol.ai.test;

import cn.winddol.ai.agent.librarian.api.ILibrarianAgent;
import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.domain.KnowledgeRelationEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手动关系回填测试：直接审计已入库论文，不经过上传、OCR 或 PaperIngestJob。
 *
 * <p>该类以 IT 结尾，默认不会被当前 Surefire 的 {@code *UnitTest} 规则执行。
 * 运行前显式传入 {@code -Dpaper.librarian.audit.backfill.paper-ids=7,9,14}，
 * 因为每篇论文最多会产生 {@code neighbor-limit} 次真实 LLM 审计调用。</p>
 */
@Tag("manual")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class LibrarianAuditBackfillIT {

    @Autowired
    private ILibrarianAgent librarianAgent;

    @Autowired
    private IPaperRepository paperRepository;

    private String paperIds = "7,9,11,14,15,16,17,18,19,20,21,22,23";

    @Value("${paper.librarian.audit.version:librarian-evidence-v1}")
    private String auditVersion;

    /**
     * 对配置中的每一篇旧论文直接运行关系审计，并验证新审计版本至少写入一条出向关系。
     */
    @Test
    void backfillConfiguredExistingPapersWithoutIngestJob() {
        List<Long> ids = parsePaperIds(paperIds);
        assertFalse(ids.isEmpty(), "Set -Dpaper.librarian.audit.backfill.paper-ids=7,9,14 before running this test");

        for (Long paperId : ids) {
            assertNotNull(paperRepository.selectPaperById(paperId), "Paper does not exist: " + paperId);

            librarianAgent.auditAgainstLibrary(paperId);

            List<KnowledgeRelationEntity> relations = paperRepository.findRelationsByPaperId(paperId);
            List<KnowledgeRelationEntity> currentAuditRelations = relations.stream()
                    .filter(item -> "OUTGOING".equals(item.getDirection()))
                    .filter(item -> auditVersion.equals(item.getAuditVersion()))
                    .toList();
            System.out.printf("[LIBRARIAN-BACKFILL] paperId=%d auditedRelations=%d%n",
                    paperId, currentAuditRelations.size());
            currentAuditRelations.forEach(item -> System.out.printf(
                    "  -> target=%d type=%s confidence=%s status=%s%n",
                    item.getRelatedId(), item.getType(), item.getConfidence(), item.getAuditStatus()));

            assertTrue(!currentAuditRelations.isEmpty(),
                    "No versioned relation was created for paperId=" + paperId
                            + ". Verify its embedding, section_chunks, and similar-paper candidates.");
        }
    }

    /**
     * 将命令行传入的逗号分隔论文 ID 转为去重后的有序列表。
     */
    private List<Long> parsePaperIds(String rawIds) {
        if (rawIds == null || rawIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .distinct()
                .toList();
    }
}
