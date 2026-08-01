# w1_ai-infrastructure

## 模块定位

`w1_ai-infrastructure` 实现领域模块声明的出站端口，负责数据库、pgvector、Redis、文件系统、MonkeyOCR、LLM、Embedding 和 Semantic Scholar 等技术细节。

它可以依赖业务模块的接口和模型；业务模块不能反向依赖 Infrastructure。

## 目录结构

```text
cn.winddol.ai.infrastructure
├─ adapter/      领域端口的实现
├─ dao/          MyBatis Mapper、PO、TypeHandler 和数据服务
├─ embedding/    旧/底层向量处理能力
├─ event/        SSE 通知实现
├─ external/     外部学术服务客户端
├─ parser/       MonkeyOCR、Markdown、Outline、引用解析
├─ redis/        Redis 会话锁
├─ scheduler/    旧调度器
└─ utils/        无状态技术工具
```

## `adapter` 文件说明

| 文件 | 作用 |
| --- | --- |
| `adapter/ai/DeepSeekAdapter.java` | 调用大模型并适配 Agent 所需 AI 接口。 |
| `adapter/ai/LibrarianRelationAiAdapter.java` | 调用大模型生成带证据键、置信度的论文关系审计结论。 |
| `adapter/paper/EmbeddingServiceImpl.java` | 实现论文模块的向量化端口。 |
| `adapter/paper/FileStorageServiceImpl.java` | 持久保存上传 PDF、Markdown、报告和解析产物。 |
| `adapter/paper/LibrarianRepositoryImpl.java` | 实现 Librarian 的论文候选和知识关系访问。 |
| `adapter/paper/LibrarianEvidenceProviderImpl.java` | 将论文模块混合检索结果适配为 Librarian 审计证据。 |
| `adapter/parser/PaperParser.java` | 聚合 Python Parser、MarkdownParser、ReferenceParser 等，适配 `IPaperParser`。 |
| `adapter/parser/MonkeyOcrArtifactReader.java` | 从 MonkeyOCR ZIP 的 content list 读取文本块和页码。 |
| `adapter/repository/PaperRepository.java` | 实现论文、章节、引用、符号和检索仓储端口。 |
| `adapter/repository/RetrievalRepository.java` | 实现 Chunk 索引替换及混合检索候选召回端口。 |
| `adapter/repository/PaperIngestJobRepository.java` | 实现摄取任务、乐观锁和阶段审计仓储端口。 |
| `adapter/repository/AgentRepository.java` | 兼容旧 Agent 数据访问接口。 |

## `dao` 文件说明

### Mapper

`PaperMapper`、`SectionMapper`、`ReferenceMapper`、`SymbolMapper` 分别访问论文核心表；`SectionChunkMapper` 保存检索 Chunk，`RetrievalMapper` 执行向量和全文候选查询；`GlobalReferenceMapper` 和 `SectionReferenceLinkMapper` 处理引用图谱；`PaperKnowledgeRelationMapper` 保存带置信度、证据 JSON、审计版本的论文关系；`PaperIngestJobMapper` 和 `PaperIngestStageRunMapper` 保存摄取状态及阶段记录；`AgentThoughtTraceMapper` 保存 Agent 轨迹。

### `dao/po`

PO 与数据库表一一对应：`Paper`、`Section`、`SectionChunkPO`、`Reference`、`Symbol`、`GlobalReference`、`SectionReferenceLink`、`PaperKnowledgeRelation`、`PaperIngestJobPO`、`PaperIngestStageRunPO` 和 `AgentThoughtTrace`。PO 不能直接暴露到 API 层。

### `dao/handler`

| 文件 | 作用 |
| --- | --- |
| `PgVectorHandler.java` | Java 向量与 PostgreSQL vector 类型转换。 |
| `OutlineNodeTypeHandler.java` | Outline JSON/数据库字段转换。 |
| `StringArrayTypeHandler.java` | PostgreSQL 字符串数组转换。 |

### `dao/impl`

`ReferenceService`、`SectionReferenceLinkService`、`SymbolService` 及其实现封装跨 Mapper 的数据操作。现有 `Serivece` 拼写是历史遗留，后续重命名时需同步 Spring 注入和引用。

## `parser` 文件说明

| 文件 | 作用 |
| --- | --- |
| `PythonParserAdapter.java` | 通过 HTTP 调用远端 MonkeyOCR，上传 PDF 并取得 Markdown/产物。 |
| `MarkdownParser.java` | 将规范 Markdown 转为 `SectionPO`，建立章节父子关系。 |
| `OutlineRepairer.java` | 根据原文和编号序列修复缺失父章节，不生成无来源标题。 |
| `PaperCleaner.java` | 清理出版信息和解析噪声。 |
| `ReferenceParser.java` | 解析编号、作者年份和 AIP 等参考文献格式。 |

## 其他目录

| 文件 | 作用 |
| --- | --- |
| `embedding/EmbeddingProcessor.java` | 旧 Embedding 处理实现。 |
| `embedding/KnowledgeRetriever.java` | 底层知识检索实现。 |
| `external/SemanticScholarClient.java` | Semantic Scholar API 客户端。 |
| `event/SseNotificationServiceImpl.java` | 向前端发送 Agent 过程事件。 |
| `redis/SessionLockService.java` | 基于 Redis 的会话互斥锁。 |
| `scheduler/GlobalScheduler.java` | 旧全局调度逻辑；新摄取恢复入口位于 Trigger。 |
| `utils/FileUtil.java` | 文件操作辅助。 |
| `utils/FingerprintUtils.java` | 论文指纹计算实现。 |
| `utils/TreeBuilderUtil.java` | Outline 树构建辅助。 |

## 配置与资源

- MyBatis XML 位于 `w1_ai-app/src/main/resources/mybatis/mapper`，其 namespace 和 resultType 必须与 Java 包名保持一致。
- MonkeyOCR 地址、超时、解析器类型和产物目录由 Spring 配置或环境变量注入。
- 数据库迁移脚本当前由 `w1_ai-app/src/main/resources/db/manual` 管理。

## 设计约束

- Adapter 负责“把技术实现翻译为领域端口”，Controller 不应直接调用 DAO。
- DAO 返回 PO 后，应在 Repository Adapter 中转换为领域模型。
- 业务状态机、Outline 决策规则和 Agent 策略不能放进本模块。
