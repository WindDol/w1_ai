# w1_ai-agent-librarian

## 模块定位

`w1_ai-agent-librarian` 负责论文入库后的知识审计：比较新论文与已有论文，识别支持、扩展、冲突等关系，并推动摄取任务从 `AUDITING` 进入 `READY` 或 `FAILED`。

它消费 `w1_ai-paper` 发布的论文入库事件，但不负责 PDF 解析、章节恢复和论文持久化。

## 目录结构

```text
cn.winddol.ai.agent.librarian
├─ api/       模块对外能力及所需端口
├─ domain/    审计领域模型
└─ internal/  审计实现与事件编排
```

## 文件说明

### `api`

| 文件 | 作用 |
| --- | --- |
| `ILibrarianAgent.java` | 发起论文知识审计的业务入口。 |
| `ILibrarianRepository.java` | Librarian 所需论文与关系数据访问端口。 |
| `ILibrarianEvidenceProvider.java` | 在指定论文内部检索正文、符号和引用证据的端口。 |
| `IAiAdapter.java` | 接收候选论文与真实证据、输出结构化关系判定的 AI 端口。 |

### `domain`

| 文件 | 作用 |
| --- | --- |
| `AgentPaperEntity.java` | 提供给审计 Agent 的论文摘要模型。 |
| `KnowledgeRelationEntity.java` | 两篇论文之间的知识关系。 |
| `AuditEvidenceBundle.java` | 一篇论文的检索证据及 retrievalVersion。 |
| `RelationAuditRequest.java` | 交给模型的候选论文、两侧证据和审计版本。 |
| `PaperAuditResult.java` | 模型返回的关系、理由、置信度和 evidenceKey。 |
| `RelationAuditRecord.java` | 可持久化的关系审计记录及其支持/反驳证据。 |
| `RelationAuditStatus.java` | `CONFIRMED/PENDING_REVIEW/LEGACY` 审计状态。 |
| `RelationType.java` | 支持、冲突、扩展、替代及 `UNRELATED` 等关系类型。 |

### `internal`

| 文件 | 作用 |
| --- | --- |
| `LibrarianAgentImpl.java` | 拉取相似论文、检索双方证据、校验 evidenceKey 并保存审计结果。 |
| `LibrarianPaperEventListener.java` | 监听 `PaperIngestedEvent`，记录审计阶段并更新最终摄取状态。 |

## 主要链路

```text
PaperIngestionWorkflow
  -> PaperIngestedEvent
  -> LibrarianPaperEventListener
  -> LibrarianAgentImpl
  -> ILibrarianEvidenceProvider（论文内混合检索）
  -> IAiAdapter（关系判断）
  -> ILibrarianRepository（按论文对和版本替换审计记录）
  -> READY 或 FAILED
```

## 关系审计规则

- 候选论文仍由论文 embedding 相似度选出，但模型不允许仅凭主题相似就断言关系。
- 模型只能返回本次检索提供的 `evidenceKey`；服务层会再次过滤未知键。
- 置信度低于阈值，或没有任何有效证据时，记录为 `PENDING_REVIEW`，详情页不会把它表述为确定事实。
- 同一 `sourcePaperId + targetPaperId + auditVersion` 会先删除旧记录再写新记录，阶段重跑不会积累重复关系。
- 旧关系因没有证据链被标为 `LEGACY`；需要重新执行 `LIBRARIAN_AUDIT` 才能升级为可审计记录。

## 历史论文关系回填

`w1_ai-app` 提供 `LibrarianAuditBackfillIT` 作为手动集成测试。它直接调用
`ILibrarianAgent.auditAgainstLibrary(paperId)`，不会创建摄取任务、不会调用 MonkeyOCR，也不依赖 `jobId`。

```powershell
mvn -pl w1_ai-app -am test `
  -Dtest=LibrarianAuditBackfillIT `
  -Dsurefire.failIfNoSpecifiedTests=false `
  -Dpaper.librarian.audit.backfill.paper-ids=7,9,14
```

每篇论文会检索相似论文并调用真实 LLM；仅传入需要回填的论文 ID。测试会要求每篇论文至少生成一条
`librarian-evidence-v1` 的出向关系，否则明确失败并提示检查 embedding、Chunk 和相似候选。

## 当前边界说明

- 模块仍临时依赖旧 `w1_ai-domain`，这是后续迁移目标，不应继续新增旧包依赖。
- 审计结果归本模块所有；其他模块如需读取，应通过只读接口或 `w1_ai-paper` 的公开模型访问。
