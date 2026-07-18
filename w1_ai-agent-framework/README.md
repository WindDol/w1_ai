# w1_ai-agent-framework

## 模块定位

`w1_ai-agent-framework` 是 Agent 模块共享的最小抽象层，定义 Agent、编排器、记忆、事件和工具的通用契约。它不包含 Research、Librarian、Writer 的业务规则，也不负责模型、数据库或 Redis 的具体实现。

依赖方向：业务 Agent 模块可以依赖本模块，本模块只依赖 `w1_ai-shared` 和必要的通用库。

## 目录结构

```text
cn.winddol.ai.framework
├─ agent/         Agent 基类
├─ event/         Agent 生命周期事件与监听器
├─ memory/        会话记忆抽象及工厂
├─ orchestrator/  通用编排器契约
└─ tool/          Agent 工具及工具提供器契约
```

## 文件说明

| 文件 | 作用 |
| --- | --- |
| `agent/Agent.java` | Agent 抽象基类，保存 Agent 名称并要求实现系统提示词。 |
| `event/AgentEvent.java` | Agent 运行期间传递的通用事件模型。 |
| `event/AgentEventListener.java` | Agent 事件监听契约。 |
| `memory/AgentMemory.java` | 对话或任务记忆的通用访问契约。 |
| `memory/AgentMemoryFactory.java` | 按会话创建或获取 Agent 记忆。 |
| `orchestrator/AgentOrchestrator.java` | 多步骤 Agent 流程的顶层编排契约。 |
| `tool/Tool.java` | 可被 Agent 调用的通用工具标记接口。 |
| `tool/ToolProvider.java` | 向 Agent 暴露工具集合，由应用配置组装具体工具。 |

## 使用原则

- 只有至少两个 Agent 模块都需要的稳定抽象才放在这里。
- 论文检索、审计、写作等业务接口应留在对应 Agent 或 `w1_ai-paper` 模块。
- LangChain4j、Redis、模型供应商的具体配置不应进入本模块。

