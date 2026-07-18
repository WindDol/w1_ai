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
| `ILibrarianAiAdapter.java` | 面向 Librarian 场景的 AI 推理端口。 |
| `IAiAdapter.java` | 当前保留的通用 AI 适配契约；后续应与场景化接口统一。 |

### `domain`

| 文件 | 作用 |
| --- | --- |
| `AgentPaperEntity.java` | 提供给审计 Agent 的论文摘要模型。 |
| `KnowledgeRelationEntity.java` | 两篇论文之间的知识关系。 |
| `PaperAuditResult.java` | 单次论文审计的结构化结果。 |
| `RelationType.java` | 支持、冲突、扩展等关系类型。 |

### `internal`

| 文件 | 作用 |
| --- | --- |
| `LibrarianAgentImpl.java` | 拉取候选论文、调用 AI 判断关系并保存审计结果。 |
| `LibrarianPaperEventListener.java` | 监听 `PaperIngestedEvent`，记录审计阶段并更新最终摄取状态。 |

## 主要链路

```text
PaperIngestionWorkflow
  -> PaperIngestedEvent
  -> LibrarianPaperEventListener
  -> LibrarianAgentImpl
  -> ILibrarianRepository / ILibrarianAiAdapter
  -> READY 或 FAILED
```

## 当前边界说明

- 模块仍临时依赖旧 `w1_ai-domain`，这是后续迁移目标，不应继续新增旧包依赖。
- 审计结果归本模块所有；其他模块如需读取，应通过只读接口或 `w1_ai-paper` 的公开模型访问。

