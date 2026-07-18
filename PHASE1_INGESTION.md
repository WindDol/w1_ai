# 阶段 1：论文摄取工作流

## 架构边界

阶段 1 将原来的同步上传方法拆成持久化异步工作流：

```text
HTTP upload
  -> w1_ai-paper: validate + store file + create PaperIngestJob
  -> paperIngestionExecutor
  -> w1_ai-paper: OCR / normalization / metadata / persistence
  -> PaperIngestedEvent
  -> w1_ai-agent-librarian: symbol / citation / embedding / audit
  -> READY or FAILED
```

- `w1_ai-paper/domain/ingest`：状态、阶段和任务领域模型。
- `w1_ai-paper/api`：任务仓储与应用用例 port。
- `w1_ai-paper/internal`：工作流、任务投递和应用服务。
- `w1_ai-infrastructure`：MyBatis 持久化、文件和解析器适配。
- `w1_ai-trigger`：HTTP 接口和恢复扫描任务。
- `w1_ai-app`：摄取专用线程池及环境配置。

## 数据库准备

启动应用前，对现有 PostgreSQL 数据库执行：

```powershell
psql -h localhost -p 15432 -U postgres -d w1_ai `
  -f .\w1_ai-app\src\main\resources\db\manual\phase-1-paper-ingestion.sql
```

脚本只新增：

- `paper_ingest_jobs`：任务当前快照、产物路径、失败原因和 worker 租约。
- `paper_ingest_stage_runs`：每个阶段每次尝试的审计历史。

同一个 PDF SHA-256 只对应一个任务。重复上传会返回原 `jobId`；失败后使用 retry，强制重新 OCR 使用 rerun。

## API

### 上传

```bash
curl -X POST "http://localhost:8091/api/v1/paper/upload" \
  -F "file=@paper.pdf"
```

接口返回 HTTP `202` 和任务对象。此时 `paperId` 允许为空，但 `jobId` 必须存在。

### 查询任务

```bash
curl "http://localhost:8091/api/v1/paper/ingestions/{jobId}"
```

重点字段：

- `status`：`UPLOADED/PARSING/PARSED/AUDITING/READY/FAILED`
- `currentStage`：当前技术阶段
- `failedStage/errorCode/errorMessage`：失败审计
- `attemptCount`：执行次数
- `paperId`：论文主记录创建后回填

### 从失败阶段继续

```bash
curl -X POST "http://localhost:8091/api/v1/paper/ingestions/{jobId}/retry"
```

### 从指定阶段重跑

```bash
curl -X POST \
  "http://localhost:8091/api/v1/paper/ingestions/{jobId}/rerun?stage=STRUCTURE_NORMALIZATION"
```

允许从以下阶段开始：

```text
MONKEY_OCR
STRUCTURE_NORMALIZATION
METADATA_EXTRACTION
PAPER_PERSISTENCE
SYMBOL_ENRICHMENT
CITATION_ENRICHMENT
EMBEDDING
LIBRARIAN_AUDIT
```

从 `STRUCTURE_NORMALIZATION` 重跑会复用 `raw-monkeyocr.md`，不会调用远程 GPU；从 `EMBEDDING` 重跑只清理并重建向量及之后的审计结果。

## 持久化产物

每个任务默认保存在：

```text
data/upload/{jobId}/
  source.pdf
  raw-monkeyocr.md
  normalized.md
  normalization-report.txt
  metadata.json
```

远程 MonkeyOCR 返回的原始 ZIP 保存在 `python.monkeyocr.http-output-dir`，其绝对路径记录在任务的 `parser_artifact_path` 字段中，因此版面图和中间 JSON 仍可追溯。

任务记录同时保存 parser、parser version 和 normalizer version，后续规则升级可以追溯。

## 配置

配置位于 `application-dev.yml`，均可由环境变量覆盖：

```text
PAPER_INGEST_CORE_POOL_SIZE
PAPER_INGEST_MAX_POOL_SIZE
PAPER_INGEST_QUEUE_CAPACITY
PAPER_INGEST_LEASE_MINUTES
PAPER_INGEST_HEARTBEAT_SECONDS
PAPER_INGEST_STALE_MINUTES
PAPER_INGEST_RECOVERY_DELAY_MS
PAPER_INGEST_RECOVERY_ENABLED
```

默认使用 2 个核心摄取线程、最多 4 个线程。远程 4090 通常不适合被大量并发请求，因此不要直接复用项目中原来的通用大线程池。

Dispatcher 会先在数据库中预占 Worker 租约，再把任务放入线程池，因此仍在队列中等待的任务也不会被恢复扫描重复提交。Worker 启动后先验证预占 Token，再由独立调度器定期续租。恢复扫描只选择没有 Worker、没有租约或租约已经过期的任务，避免长时间 MonkeyOCR 仍在执行时被第二个 Worker 重复领取。心跳间隔必须显著小于租约时长，代码会将异常配置限制在租约时长的三分之一以内。

## 验证清单

1. 执行默认单元测试：

   ```powershell
   mvn test -pl w1_ai-app -am
   ```

2. 正常上传后轮询任务，最终应为 `READY`，且 `paperId` 非空。
3. 暂停 SSH 隧道后上传一份新 PDF，应在 `MONKEY_OCR` 进入 `FAILED` 并记录错误。
4. 恢复隧道后调用 retry，应从 `MONKEY_OCR` 继续并增加 `attemptCount`。
5. 让 Embedding key 暂时无效，确认失败阶段为 `EMBEDDING`；恢复 key 后 retry，MonkeyOCR 服务日志不应出现新的 `/parse` 请求。
6. 对 READY 任务从 `STRUCTURE_NORMALIZATION` 重跑，确认复用原始 Markdown，章节、符号、引用和关系没有重复数据。
7. 处理期间停止并重启 Java 应用，超过恢复阈值后任务应被重新投递。

默认单元测试不需要 PostgreSQL、Redis、LLM 或 MonkeyOCR；只有手工链路验证需要启动真实依赖。
