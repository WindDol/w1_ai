package cn.winddol.ai.test;

import cn.winddol.ai.paper.api.IPaperRepository;
import cn.winddol.ai.paper.api.IRetrievalIndexService;
import cn.winddol.ai.paper.domain.PaperEntity;
import cn.winddol.ai.infrastructure.scheduler.GlobalScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 为阶段一状态机建立前已经入库的论文补建 Chunk 检索索引。
 *
 * 该测试会真实写入数据库并调用 embedding 服务，必须通过 paperIds 系统属性显式执行。
 * 旧论文没有 parser artifact 时无法恢复页码，但不影响 Chunk 全文和向量检索。
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("dev")
class LegacyPaperChunkBackfillIT {

    @Autowired
    private IRetrievalIndexService retrievalIndexService;

    @Autowired
    private IPaperRepository paperRepository;

    @MockBean
    private GlobalScheduler globalScheduler;

    /**
     * 逐篇重建指定论文的 Chunk；相同论文重复执行时会整体替换旧索引，不产生重复数据。
     */
    @Test
    void rebuildLegacyPaperChunks() {
        List<Long> paperIds = requiredPaperIds();
        int totalChunks = 0;

        for (Long paperId : paperIds) {
            PaperEntity paper = paperRepository.selectPaperById(paperId);
            assertNotNull(paper, "论文不存在，paperId=" + paperId);

            int chunkCount = retrievalIndexService.rebuild(paperId, null);
            assertTrue(chunkCount > 0,
                    "论文没有生成任何 Chunk，paperId=" + paperId + ", title=" + paper.getTitle());
            totalChunks += chunkCount;
            System.out.printf("[BACKFILL] paperId=%d chunks=%d title=%s%n",
                    paperId, chunkCount, paper.getTitle());
        }

        System.out.printf("[BACKFILL] completed papers=%d totalChunks=%d%n",
                paperIds.size(), totalChunks);
    }

    /**
     * 从 -DpaperIds=7,9,11 读取、校验并去重论文 ID，未传参数时主动拒绝修改数据库。
     */
    private List<Long> requiredPaperIds() {
        String configured = "7,9,11,14,15,16,17,18,19,20,21";

        try {
            List<Long> paperIds = Arrays.stream(configured.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .filter(value -> value > 0)
                    .distinct()
                    .toList();
            assertTrue(!paperIds.isEmpty(), "paperIds 中没有有效的正整数 ID");
            return paperIds;
        } catch (NumberFormatException error) {
            throw new AssertionError("paperIds 必须是逗号分隔的正整数：" + configured, error);
        }
    }
}
