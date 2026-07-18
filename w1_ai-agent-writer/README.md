# w1_ai-agent-writer

## 模块定位

`w1_ai-agent-writer` 是论文写作 Agent 的预留模块，目前只保留写作计划、Writer Agent 和编排器骨架，不是当前项目主线。

在论文摄取、检索和审计链路稳定前，不应在这里扩展复杂多 Agent 写作流程。

## 目录结构

```text
cn.winddol.ai.agent.writer
├─ api/       写作能力契约
├─ domain/    写作计划模型
└─ internal/  Writer 与编排器骨架
```

## 文件说明

| 文件 | 作用 |
| --- | --- |
| `api/IWriterAgent.java` | 执行具体写作任务的接口。 |
| `api/IPaperWriterOrchestrator.java` | 论文写作流程的顶层编排接口。 |
| `domain/WritingPlan.java` | 论文写作目标、章节和步骤计划。 |
| `internal/WriterAgentImpl.java` | Writer Agent 的当前基础实现。 |
| `internal/PaperWriterOrchestratorImpl.java` | 调度写作计划和 Writer Agent 的骨架。 |

## 后续扩展约束

- 写作内容必须基于可追溯的论文证据和引用。
- 不直接访问 Infrastructure，应依赖本模块或论文模块定义的接口。
- 在功能成熟前保持单一 Writer 流程，避免过早增加 Reviewer、Editor 等 Agent。

