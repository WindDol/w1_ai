# w1_ai-paper

## 模块定位

`w1_ai-paper` 是论文能力的核心业务模块，拥有论文摄取状态机、论文/章节/引用/符号模型、Outline 结构恢复、检索阅读工具以及入库后增强流程。

本模块只定义业务规则和所需端口。MonkeyOCR HTTP、PostgreSQL、文件系统和 Embedding 的具体实现位于 `w1_ai-infrastructure`。

## 目录结构

```text
cn.winddol.ai.paper
├─ api/                  对外应用接口及基础设施端口
├─ domain/               论文领域模型
│  ├─ ingest/            摄取任务、阶段和状态机
│  └─ retrieval/         Chunk、证据、召回通道和检索请求模型
└─ internal/
   ├─ *.java             跨能力应用编排
   ├─ structure/         OCR Markdown 与 Outline 结构恢复
   ├─ enrichment/        引用、符号和元数据增强
   └─ retrieval/         论文检索与章节阅读
```

## `api` 文件说明

| 文件 | 作用 |
| --- | --- |
| `IPaperApplication.java` | 上传、任务查询、重试、阶段重跑、恢复和论文查询的应用入口。 |
| `IPaperIngestJobRepository.java` | 摄取任务和阶段执行审计的持久化端口。 |
| `IPaperRepository.java` | 论文、章节、引用、符号、关系和检索数据访问端口。 |
| `IPaperParser.java` | PDF 解析、Markdown 解析和元数据提取端口。 |
| `IFileStorageService.java` | 原始 PDF 与解析产物的持久文件存储端口。 |
| `IEmbeddingService.java` | 文本向量化端口。 |
| `IFingerprintUtils.java` | 论文指纹生成端口。 |
| `ISymbolExtractor.java` | 元数据与符号抽取端口。 |
| `ISemanticScholar.java` | Semantic Scholar 外部查询端口。 |
| `IScientificResearchTools.java` | 向 Agent 暴露的论文搜索和阅读工具集合。 |
| `IPaperRetrievalService.java` | 对外提供统一的结构化证据检索用例。 |
| `IRetrievalRepository.java` | Chunk 索引与向量/全文候选召回端口。 |
| `IRetrievalIndexService.java` | 从现有章节重建结构化 Chunk 索引。 |
| `IParserArtifactReader.java` | 从 MonkeyOCR 产物读取带页码的文本块。 |
| `PaperIngestedEvent.java` | 核心入库完成后发布给 Librarian 的事件。 |

## `domain` 文件说明

### 论文模型

| 文件组 | 作用 |
| --- | --- |
| `PaperEntity`, `PaperVO`, `PaperDetailVO` | 论文核心实体、列表视图和详情视图。 |
| `SectionEntity`, `SectionPO`, `OutlineNode` | 章节实体、解析中间模型和 Outline 树节点。 |
| `ReferenceItem`, `ReferenceEnum`, `GlobalReferenceEntity` | 局部引用、引用类型和全局引用实体。 |
| `SectionReferenceLinkEntity` | 章节正文与引用条目的关联。 |
| `SymbolEntity`, `SymbolDefinition` | 论文中的数学符号及解释。 |
| `KnowledgeRelationEntity` | 论文之间的可读知识关系。 |
| `SearchResultDTO` | 混合检索结果。 |
| `RefMetadata` | 指纹生成所需作者、年份等元数据。 |
| `S2Author`, `S2PaperData`, `S2PaperResponse` | Semantic Scholar 响应模型。 |

### `domain/retrieval`

| 文件组 | 作用 |
| --- | --- |
| `SectionChunk` | 隶属于章节的最小检索和引用单元，不替代原有 Section。 |
| `PaperRetrievalQuery`, `PaperRetrievalResult` | 统一检索输入和带版本、耗时的结果。 |
| `RetrievalCandidate`, `PaperEvidence` | 召回阶段候选与最终结构化证据。 |
| `EvidenceType`, `RetrievalChannel` | 区分章节/符号/引用及向量/全文来源。 |
| `SourceTextBlock` | MonkeyOCR 文本块及原始页码。 |

### `domain/ingest`

| 文件 | 作用 |
| --- | --- |
| `PaperIngestJob.java` | 一次论文摄取任务的聚合状态，包括文件、产物、错误、版本和当前阶段。 |
| `PaperIngestStatus.java` | `UPLOADED/PARSING/PARSED/FAILED/AUDITING/READY` 等任务状态。 |
| `PaperIngestStage.java` | 文件校验、OCR、结构恢复、持久化、向量化、审计等可重跑阶段。 |
| `PaperIngestStateMachine.java` | 校验状态迁移是否合法，防止任务越级或倒退。 |
| `IngestStageRunStatus.java` | 单个阶段执行记录的运行状态。 |
| `StoredPaperFile.java` | 已持久保存 PDF 的路径、摘要和大小。 |
| `PdfParseResult.java` | Parser 返回的 Markdown 与远端/本地产物位置。 |

## `internal` 外层编排

| 文件 | 作用 |
| --- | --- |
| `PaperApplicationServiceImpl.java` | 应用入口；校验上传、创建任务、去重、提交异步执行、重试和恢复。 |
| `PaperIngestionDispatcher.java` | 在数据库预占 Worker 租约后将任务提交到专用线程池，防止重复入队。 |
| `PaperIngestionWorkflow.java` | 摄取主编排；按阶段调用 Parser、Normalizer、Repository 和事件发布，并在执行期间续租。 |
| `PaperIngestStageException.java` | 携带失败阶段和错误码的内部异常。 |
| `ScientificResearchToolsImpl.java` | 聚合 retrieval 子包能力，作为 Agent 工具门面。 |

跨多个组件的编排保留在 `internal` 外层；单一职责实现放入子包。

## `internal/structure`

| 文件 | 作用 |
| --- | --- |
| `PaperStructureNormalizer.java` | 结构恢复入口，输出规范 Markdown 和审计报告。 |
| `HeadingCandidateExtractor.java` | 从 OCR Markdown 提取显式标题和裸文本候选。 |
| `HeadingRuleScorer.java` | 根据编号、上下文、噪声和引用区间为候选评分并决策。 |
| `NormalizedMarkdownBuilder.java` | 根据决策生成最终 Markdown，低置信内容保留为正文。 |
| `HeadingCandidate.java` | 候选标题及行号、来源层级、上下文信息。 |
| `HeadingDecision.java` | 候选的动作、目标层级、置信度和原因。 |
| `CandidateKind.java` | 标题、列表、引用、噪声等候选分类。 |
| `DecisionAction.java` | 输出标题、拆分标题正文、降级正文或删除噪声。 |
| `PaperStructureNormalizationResult.java` | 规范 Markdown、完整决策列表、告警和总置信度。 |

## `internal/enrichment`

| 文件 | 作用 |
| --- | --- |
| `PaperEnrichmentServiceImpl.java` | 从章节提取符号并持久化，执行论文后处理。 |
| `CitationEnrichmentServiceImpl.java` | 将局部参考文献匹配到全局引用并建立关联。 |
| `CitationLinkExtractor.java` | 从章节正文提取引用编号。 |

## `internal/retrieval`

| 文件 | 作用 |
| --- | --- |
| `HybridRetrieverServiceImpl.java` | 组合文本向量与 Repository 过滤执行论文库检索。 |
| `StructuredSectionChunker.java` | 在章节边界内按段落/句子构建有重叠的 Chunk。 |
| `RetrievalIndexServiceImpl.java` | 生成 Chunk、补充 Outline 路径/页码并向量化。 |
| `ReciprocalRankFusion.java` | 使用 RRF 融合不同检索通道并去重。 |
| `HeadingPathResolver.java` | 根据 Section 父子关系生成稳定 Outline 路径。 |
| `EvidenceQuoteExtractor.java` | 从命中 Chunk 生成长度受控的证据片段。 |
| `AgentReaderServiceImpl.java` | 获取 Outline、带上下文读取章节、查引用和论文关系。 |
| `AgentCommonToolsImpl.java` | 查找论文和高被引参考文献等公共 Agent 工具。 |

## 摄取主链路

```text
submit PDF
  -> 持久保存原始文件 + SHA-256 去重
  -> 创建 PaperIngestJob
  -> PaperIngestionDispatcher
  -> MONKEY_OCR
  -> STRUCTURE_NORMALIZATION
  -> METADATA_EXTRACTION
  -> PAPER_PERSISTENCE
  -> REFERENCE/SYMBOL
  -> EMBEDDING (Paper/Symbol + Section Chunk retrieval index)
  -> PaperIngestedEvent
  -> Librarian audit
  -> READY
```

每个阶段写入开始、完成或失败记录；失败后可从指定阶段重跑，原始 PDF 和中间产物用于审计和复现。

## 依赖规则

- 可以依赖 `w1_ai-shared`，不能依赖 `w1_ai-infrastructure` 或 `w1_ai-trigger`。
- 基础设施需求先在 `api` 定义端口，再由 Infrastructure 实现。
- 新论文业务模型只能进入本模块，禁止重新放入旧 `domain.paperTools`。
