# w1_ai-shared

## 模块定位

`w1_ai-shared` 保存所有业务模块都可以安全依赖的少量稳定类型，例如统一异常、响应码、领域事件基类和通用工具结果。

它不是“暂时不知道放哪里”的目录。带有论文、Research 或 Librarian 语义的类型应回到对应业务模块。

## 目录结构

```text
cn.winddol.ai.shared
├─ api/        当前跨模块共享的增强服务契约
├─ common/     全局常量
├─ enums/      全局枚举
├─ exception/  统一异常
└─ model/
   ├─ event/   领域事件基类
   └─ tool/    通用工具结果
```

## 文件说明

| 文件 | 作用 |
| --- | --- |
| `api/ICitationEnrichmentService.java` | 引用增强能力的共享契约。 |
| `api/IPaperEnrichmentService.java` | 论文后处理增强能力的共享契约。 |
| `common/Constants.java` | 全局稳定常量。 |
| `enums/ResponseCode.java` | 跨模块统一响应码。 |
| `exception/AppException.java` | 业务异常，携带错误码和可读消息。 |
| `model/event/DomainEvent.java` | 领域事件通用基类。 |
| `model/tool/ToolResult.java` | Agent 工具调用的统一结果模型。 |

## 使用原则

- 本模块不能依赖任一具体业务模块。
- 新类型必须证明至少被多个模块稳定共享，才能放进来。
- `ICitationEnrichmentService` 与 `IPaperEnrichmentService` 仍带有论文语义，后续可评估迁入 `w1_ai-paper/api`。

