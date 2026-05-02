# 重构日志 — 从耦合单体到 Agent 框架化架构

## 一、为什么要重构

原来的 6 个模块中，所有业务逻辑挤在 `w1_ai-domain` 一个模块里，存在以下问题：

| 问题 | 旧代码示例 |
|------|----------|
| Agent 没有接口，直接注入具体类 | `@Resource private ResearchAgent researchAgent;` |
| paperTools 和 agent 两个域互相 import | `PaperApplicationService` import `Librarian` |
| Spring 框架类型侵入领域层 | `NotificationService` 接口里有 `SseEmitter` |
| 类型不安全的事件 | `ResearchEvent.data` 是 `Object` 类型 |
| Repository 里生成嵌入向量 | `PaperRepository.saveFullPaper()` 调用 `embeddingModel.embed()` |
| 字段注入不可测试 | 全都用 `@Resource`，无法在单元测试中 mock |

**不重构的后果**：每加一个新 Agent（比如写作 Agent），就要在同一个大包里继续堆代码，耦合越来越深，最终不可维护。

---

## 二、重构做了什么

### 2.1 新建 6 个模块（共 63 个 Java 文件）

```
旧模块结构                          新模块结构
─────────                          ─────────
w1_ai-types (3个类)        ──→     w1_ai-shared (5个类，新增 DomainEvent / ToolResult)
                                   
(无)                       ──→     w1_ai-agent-framework (8个类)
                                   ├── Tool / ToolProvider        ← Agent 工具接口
                                   ├── Agent                      ← Agent 抽象基类
                                   ├── AgentEvent + Type枚举      ← 强类型事件
                                   ├── AgentMemory / Factory      ← 会话记忆接口
                                   └── AgentOrchestrator          ← 编排器接口

w1_ai-domain/agent/        ──→     w1_ai-agent-research (11个类)
  ResearchAgent                    ├── api/IResearchAgent         ← 接口（新增）
  AgentSupervisor                  ├── api/ISupervisor
  ResearchOrchestrator             ├── api/IAiAdapter
                                   ├── api/ISessionLockService
                                   ├── domain/AgentStep
                                   └── internal/ResearchAgentImpl  ← 实现（ReAct循环）
                                        internal/SupervisorImpl
                                        internal/ResearchOrchestratorImpl

w1_ai-domain/agent/        ──→     w1_ai-agent-librarian (10个类)
  Librarian                        ├── api/ILibrarianAgent         ← 接口（新增）
  LibrarianAuditService            ├── api/ILibrarianRepository
  PaperAuditListener               ├── api/ILibrarianAiAdapter
                                   ├── domain/AgentPaperEntity
                                   ├── domain/PaperAuditResult
                                   ├── domain/KnowledgeRelationEntity
                                   └── internal/LibrarianAgentImpl
                                        internal/LibrarianPaperEventListener

(无)                       ──→     w1_ai-agent-writer (5个类，骨架)
                                   ├── api/IWriterAgent
                                   ├── api/IPaperWriterOrchestrator
                                   ├── domain/WritingPlan
                                   └── internal/WriterAgentImpl (抛 UnsupportedOperationException)

w1_ai-domain/paperTools/    ──→     w1_ai-paper (24个类，现在是11个)
  PaperApplicationService           ├── api/IPaperApplication       ← 接口
  ScientificResearchTools           ├── api/IPaperRepository
  HybridRetrieverService            ├── api/IScientificResearchTools
  AgentReaderService                ├── api/IFileStorageService     ← 新提取
  AgentCommonTools                  ├── api/IEmbeddingService       ← 新提取
  entities / VOs                    ├── event/PaperIngestedEvent    ← 事件解耦
                                    └── internal/*Impl
```

### 2.2 关键架构改动

#### 改动 1：每个 Agent 都有了接口

**以前**（直接依赖具体类）：
```java
// ResearchOrchestrator.java 旧代码
@Resource
private ResearchAgent researchAgent;        // 具体类！
@Resource
private AgentSupervisor supervisor;         // 具体类！
```

**现在**（依赖接口）：
```java
// ResearchOrchestratorImpl.java 新代码
public ResearchOrchestratorImpl(
    ISupervisor supervisor,          // 接口
    IResearchAgent researchAgent,    // 接口
    AgentMemoryFactory memoryFactory,// 接口
    ...
) { ... }
```

**好处**：Mock 测试、替换实现、独立部署都变得可能。

#### 改动 2：Paper 和 Librarian 通过事件解耦

**以前**（直接调用）：
```java
// PaperApplicationService.java 旧代码
@Resource
private Librarian librarian;        // paperTools 直接依赖 agent 域
...
librarian.initiateAudit(paperId, title);  // 同步调用
```

**现在**（发布事件，异步监听）：
```java
// PaperApplicationServiceImpl.java 新代码
// 不 import 任何 Librarian 的类
eventPublisher.publishEvent(new PaperIngestedEvent(paperId, title));

// LibrarianPaperEventListener.java 新代码（独立模块）
@Async
@EventListener
public void onPaperIngested(PaperIngestedEvent event) {
    // 异步处理，不阻塞上传响应
}
```

**好处**：两个模块互不 import，各自独立开发。未来可以换成消息队列（Kafka/RabbitMQ）。

#### 改动 3：事件类型安全

**以前**：
```java
ResearchEvent.builder()
    .type("THOUGHT")     // 字符串，容易写错
    .data(someObject)    // Object 类型，无编译检查
    .build();
```

**现在**：
```java
AgentEvent.builder()
    .type(AgentEvent.Type.THOUGHT)  // 枚举，IDE 自动补全
    .data(jsonString)               // String，类型明确
    .build();
```

#### 改动 4：全部改用构造器注入

**以前**：`@Resource private SomeService service;`（字段注入，无法在测试中 mock）

**现在**：
```java
public SomeClass(SomeService service) {
    this.service = service;
}
```
（单元测试中可以直接 `new SomeClass(mockService)`）

#### 改动 5：提取了 3 个新的基础设施接口

这 3 个接口以前不存在，业务代码直接调基础设施：

| 新接口 | 位置 | 取代了 |
|--------|------|--------|
| `IFileStorageService` | `w1_ai-paper/api/` | `PaperApplicationService` 里直接 `new File()`、`file.transferTo()` |
| `IEmbeddingService` | `w1_ai-paper/api/` | `HybridRetrieverService` 里直接 `embeddingModel.embed()` |
| `AgentMemoryFactory` | `w1_ai-agent-framework/` | `ResearchOrchestrator` 里直接 `MessageWindowChatMemory.builder()` |

**注意**：这 3 个接口的 `@Service` 实现类还没写（目前在 `w1_ai-infrastructure` 中需要创建）。

---

## 三、依赖关系图

```
w1_ai-shared              ← 零依赖
    ↑
w1_ai-agent-framework     ← 只依赖 shared
    ↑
    ├── w1_ai-agent-research   ← 依赖 framework + shared
    ├── w1_ai-agent-librarian  ← 依赖 framework + shared + paper + domain(过渡)
    ├── w1_ai-agent-writer     ← 依赖 framework + shared
    └── w1_ai-paper            ← 依赖 framework + shared + domain(过渡) + types(过渡)
            ↑
w1_ai-infrastructure       ← 依赖所有领域模块（实现它们的 port 接口）
            ↑
w1_ai-trigger + w1_ai-api  ← 依赖 domain + types + api 接口
            ↑
w1_ai-app                  ← 依赖所有模块（Spring Boot 入口）
```

**过渡依赖说明**：`w1_ai-paper` 和 `w1_ai-agent-librarian` 目前还依赖旧的 `w1_ai-domain` 和 `w1_ai-types`，因为旧模块中还有 adapter 接口（如 `IPaperParser`、`ISymbolExtractor`）和 `PaperIngestedEvent` 的消费。等 infrastructure 适配器全部迁移到新接口后，旧模块可以删除。

---

## 四、如何继续开发

### 4.1 如果要实现 WriterAgent（自动写论文）

1. 打开 `w1_ai-agent-writer/src/main/java/cn/winddol/ai/agent/writer/internal/WriterAgentImpl.java`
2. 把 `throw new UnsupportedOperationException(...)` 替换为真正的实现
3. 如需调用 LLM，构造器注入 `ChatLanguageModel`
4. 如需工具，注入 `ToolProvider`
5. 通过 `IWriterAgent` 接口暴露能力
6. 在 `AiConfig` 中注册 Bean

### 4.2 如果要添加新的 Agent（比如 ReviewerAgent）

```
1. 新建模块 w1_ai-agent-reviewer（copy w1_ai-agent-writer 的 pom.xml 改 artifactId）
2. 创建目录：api/ domain/ internal/
3. 在 api/ 定义 IReviewerAgent 接口
4. 在 internal/ 写实现类
5. 在父 pom.xml 中加 <module> 和 <dependency>
6. 在 w1_ai-app/pom.xml 中加依赖
```

### 4.3 如果要给 ResearchAgent 加新工具

1. 在 `w1_ai-paper` 的 `IScientificResearchTools` 接口中加新方法
2. 在 `ScientificResearchToolsImpl` 中实现
3. 修改 `ResearchAgentImpl` 的 `SYSTEM_PROMPT` 常量，加入新工具的说明
4. 在 `executeTool` switch 中加新 case（或直接委托给 `ToolProvider`）

### 4.4 需要补写的 infrastructure 适配器

以下接口定义了但还没有 `@Service` 实现类，需要在 `w1_ai-infrastructure` 中创建：

| 接口 | 需要实现的类 | 功能 |
|------|------------|------|
| `IEmbeddingService` | `EmbeddingServiceImpl` | 调用 `EmbeddingModel` 生成向量 |
| `IFileStorageService` | `FileStorageServiceImpl` | 保存/删除临时 PDF 文件 |
| `AgentMemoryFactory` | `RedisAgentMemoryFactory` | 基于 Redis 创建 `AgentMemory` |
| `ToolProvider` | `ResearchToolProvider` | 把 `ScientificResearchToolsImpl` 包装为框架的 `Tool` 列表 |

---

## 五、每个模块的关键文件速查

### w1_ai-shared（共享内核）

| 文件 | 作用 |
|------|------|
| `shared/enums/ResponseCode.java` | 统一响应码（从旧 types 迁移） |
| `shared/exception/AppException.java` | 统一异常类（从旧 types 迁移） |
| `shared/common/Constants.java` | 全局常量 |
| `shared/model/event/DomainEvent.java` | 领域事件基类（泛型，不依赖 Spring） |
| `shared/model/tool/ToolResult.java` | Agent 工具调用返回值 |

### w1_ai-agent-framework（Agent 框架）

| 文件 | 作用 |
|------|------|
| `framework/tool/Tool.java` | 工具接口：name() / description() / execute() |
| `framework/tool/ToolProvider.java` | 工具提供者：getTools() / executeTool() |
| `framework/agent/Agent.java` | Agent 抽象基类，持有 name + ToolProvider |
| `framework/event/AgentEvent.java` | 强类型事件（THOUGHT/ACTION/OBSERVATION/ANSWER/ERROR） |
| `framework/event/AgentEventListener.java` | 事件监听器接口 |
| `framework/memory/AgentMemory.java` | 会话记忆接口 + asChatMemory() 桥接 |
| `framework/memory/AgentMemoryFactory.java` | 记忆工厂接口 |
| `framework/orchestrator/AgentOrchestrator.java` | 编排器接口 |

### w1_ai-agent-research（研究 Agent）

| 文件 | 作用 |
|------|------|
| `api/IResearchAgent.java` | 研究 Agent 接口 |
| `api/ISupervisor.java` | 导师接口（改写/翻译用户问题） |
| `api/IResearchOrchestrator.java` | 编排器接口 |
| `api/ISessionLockService.java` | 会话锁接口 |
| `api/IAiAdapter.java` | AI 适配器接口 |
| `internal/ResearchAgentImpl.java` | **核心**：ReAct 循环实现 |
| `internal/SupervisorImpl.java` | 导师实现 |
| `internal/ResearchOrchestratorImpl.java` | 编排器实现 |

### w1_ai-agent-librarian（馆员 Agent）

| 文件 | 作用 |
|------|------|
| `api/ILibrarianAgent.java` | 馆员 Agent 接口 |
| `api/ILibrarianRepository.java` | 馆员数据访问接口 |
| `api/ILibrarianAiAdapter.java` | AI 适配器接口 |
| `internal/LibrarianAgentImpl.java` | 知识审核逻辑（查询相似论文→LLM 分析→保存关系） |
| `internal/LibrarianPaperEventListener.java` | **事件监听器**：收到 PaperIngestedEvent → 异步执行完整管线 |

### w1_ai-agent-writer（写作 Agent，骨架）

| 文件 | 作用 |
|------|------|
| `api/IWriterAgent.java` | 写作 Agent 接口（generateOutline / writeSection / formatPaper） |
| `api/IPaperWriterOrchestrator.java` | 写作编排器接口 |
| `domain/WritingPlan.java` | 写作计划模型 |
| `internal/WriterAgentImpl.java` | 骨架实现，待开发 |

### w1_ai-paper（论文管线）

| 文件 | 作用 |
|------|------|
| `api/IPaperApplication.java` | 论文应用接口（上传/列表/详情） |
| `api/IPaperRepository.java` | 论文数据访问接口 |
| `api/IScientificResearchTools.java` | 科研工具接口（搜索/大纲/读章节/查引用） |
| `api/IFileStorageService.java` | 文件存储接口（新提取） |
| `api/IEmbeddingService.java` | 嵌入向量接口（新提取） |
| `event/PaperIngestedEvent.java` | 论文入库事件（解耦 paper 和 librarian） |
| `internal/PaperApplicationServiceImpl.java` | 论文上传编排 |
| `internal/ScientificResearchToolsImpl.java` | 科研工具实现 |
| `internal/HybridRetrieverServiceImpl.java` | 混合检索（向量 + 关键词） |
| `internal/AgentReaderServiceImpl.java` | 论文阅读服务 |
| `internal/AgentCommonToolsImpl.java` | 通用工具实现 |
