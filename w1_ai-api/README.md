# w1_ai-api

## 模块定位

`w1_ai-api` 定义系统对外 HTTP 契约、请求/响应 DTO 和统一响应结构。它描述“外部调用者能看到什么”，不包含 Controller 实现、业务编排或数据库访问。

`w1_ai-trigger` 实现这里的接口，`w1_ai-app` 负责把实现装配进 Spring Boot 应用。

## 目录结构

```text
cn.winddol.ai.api
├─ IPaperController.java
├─ IResearchController.java
├─ IScientificResearchController.java
├─ dto/       HTTP 边界使用的数据传输对象
└─ response/  统一响应包装
```

## 文件说明

| 文件 | 作用 |
| --- | --- |
| `IPaperController.java` | 论文上传、任务查询、重试、阶段重跑和论文查询的 HTTP 契约。 |
| `IResearchController.java` | Research Agent 会话入口契约。 |
| `IScientificResearchController.java` | 论文检索、章节阅读等科研工具接口契约。 |
| `dto/PaperIngestJobDTO.java` | 向前端返回摄取状态、阶段、错误和产物信息。 |
| `dto/PaperDTO.java` | 论文列表项。 |
| `dto/PaperDetailDTO.java` | 论文详情。 |
| `dto/SymbolDTO.java` | 论文符号展示模型。 |
| `response/Response.java` | 统一成功/失败响应包装。 |
| `package-info.java` | API 包边界说明。 |

## 设计约束

- DTO 只表达 HTTP 数据，不应携带持久化或领域行为。
- API 接口不能依赖 `w1_ai-infrastructure` 的 PO、Mapper 或具体实现。
- 业务状态变化应先在领域模块定义，再映射为 DTO。

