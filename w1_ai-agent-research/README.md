# w1_ai-agent-research

## 模块定位

`w1_ai-agent-research` 负责面向用户问题的研究型问答编排。它处理问题改写、会话并发控制、工具调用、研究步骤记录和最终回答生成，不直接实现论文数据库检索。

论文检索与章节阅读工具由 `w1_ai-paper` 提供，并在应用层通过 `ToolProvider` 注入。

## 目录结构

```text
cn.winddol.ai.agent.research
├─ api/       Research Agent 对外能力和依赖端口
├─ domain/    研究步骤与过程模型
└─ internal/  Agent、Supervisor 和流程编排实现
```

## 文件说明

### `api`

| 文件 | 作用 |
| --- | --- |
| `IResearchAgent.java` | 执行单次研究任务的核心接口。 |
| `IResearchOrchestrator.java` | 从用户问题到最终研究结果的应用入口。 |
| `ISupervisor.java` | 问题改写、翻译和任务准备能力。 |
| `ISessionLockService.java` | 防止同一会话并发执行多个研究任务。 |
| `IAiAdapter.java` | Research 场景所需 AI 能力端口。 |
| `ResearchEventListener.java` | 向外报告研究过程和步骤事件。 |

### `domain`

| 文件 | 作用 |
| --- | --- |
| `AgentStep.java` | 通用 Agent 步骤数据。 |
| `ResearchAgentStep.java` | Research Agent 的思考、工具和结果步骤。 |

### `internal`

| 文件 | 作用 |
| --- | --- |
| `SupervisorImpl.java` | 结合历史消息重写和规范化用户问题。 |
| `ResearchAgentImpl.java` | 驱动模型和工具完成循环研究。 |
| `ResearchOrchestratorImpl.java` | 管理会话锁、记忆、Supervisor 与 ResearchAgent 的调用顺序。 |

## 主要链路

```text
ResearchController
  -> IResearchOrchestrator
  -> ISessionLockService
  -> ISupervisor
  -> IResearchAgent
  -> ToolProvider
  -> w1_ai-paper 检索/阅读工具
  -> 最终回答
```

## 使用原则

- Agent 提示词、步骤控制和研究策略放在本模块。
- 论文实体、向量检索和引用查询不要复制到本模块。
- 新增工具优先在能力所有者模块实现，再通过 `ToolProvider` 暴露。

