# w1_ai-domain

## 模块定位

`w1_ai-domain` 是重构前的单体领域模块，目前作为兼容层保留。它同时混合了 Agent、论文模型、端口和服务实现，新的模块化架构正在逐步接管这些职责。

此模块不是新增功能的目标位置。新代码应按业务所有权进入 `w1_ai-paper`、`w1_ai-agent-research`、`w1_ai-agent-librarian`、`w1_ai-shared` 或 `w1_ai-infrastructure`。

## 当前目录

```text
cn.winddol.ai.domain
├─ agent/
│  ├─ adapter/   旧 AI、Embedding、事件、Redis、Repository 端口
│  ├─ event/     旧 Research 事件
│  ├─ model/     旧 Agent 与审计模型
│  └─ service/   旧 Research/Librarian 编排和实现
├─ paperTools/
│  ├─ adapter/   旧论文端口
│  ├─ event/     旧论文事件
│  ├─ model/     旧论文、章节、引用、符号模型
│  └─ service/   旧论文应用、增强、检索和阅读服务
└─ yyy/          模板占位包，无实际业务
```

## `agent` 目录说明

| 目录/文件组 | 当前作用 | 迁移目标 |
| --- | --- | --- |
| `adapter/ai`, `embedding` | 旧 AI 与 Embedding 端口。 | 对应 Agent/Paper 的 `api`。 |
| `adapter/event`, `redis`, `repository` | 通知、会话锁和仓储端口。 | 对应业务模块 `api`。 |
| `model/entity`, `aggregate`, `valobj` | Agent 步骤、论文审计、冲突和关系模型。 | Research 或 Librarian 的 `domain`。 |
| `service/ResearchOrchestrator.java` | 旧 Research 编排。 | `w1_ai-agent-research/internal`。 |
| `service/bussiness/ResearchAgent.java` | 旧 Research Agent。 | `w1_ai-agent-research/internal`。 |
| `service/bussiness/Librarian*.java` | 旧 Librarian 审计实现。 | `w1_ai-agent-librarian/internal`。 |
| `ResearchProcessListener`, `PaperAuditListener` | 旧流程监听器。 | 对应 Agent 的事件或 internal。 |

## `paperTools` 目录说明

| 目录/文件组 | 当前作用 | 迁移目标 |
| --- | --- | --- |
| `adapter/*` | Parser、Repository、Semantic Scholar、符号和指纹端口。 | `w1_ai-paper/api`。 |
| `event/PaperIngestedEvent.java` | 旧入库事件。 | `w1_ai-paper/api`。 |
| `model/aggregate`, `entity`, `valobj` | 旧论文、章节、引用、符号和查询模型。 | `w1_ai-paper/domain`。 |
| `service/PaperApplicationService.java` | 旧同步论文入库服务。 | `w1_ai-paper/internal` 的异步摄取链。 |
| `service/ScientificResearchTools.java` | 旧 Agent 工具门面。 | `w1_ai-paper/internal`。 |
| `service/librarianTools/*` | 旧引用与论文增强。 | `w1_ai-paper/internal/enrichment`。 |
| `service/researchTools/*` | 旧检索和阅读工具。 | `w1_ai-paper/internal/retrieval`。 |

## 迁移状态

- Paper 核心模型、端口、Outline、检索与增强能力已经在 `w1_ai-paper` 建立新归属。
- Librarian 已建立独立模块，但仍有少量旧依赖需要清理。
- Research 与 Writer 已有独立模块，旧实现应逐步停止注入。
- `yyy` 仅为模板残留，可在确认无引用后删除。

## 修改规则

1. 禁止向本模块增加新业务类型或服务。
2. 修复旧代码时优先迁移，而不是继续复制两份实现。
3. 每迁出一组类型，先更新 Infrastructure 和 Trigger，再扫描旧包引用。
4. 当 Paper、Research、Librarian 均无旧依赖后，从父 POM 移除本模块。

