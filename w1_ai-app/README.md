# w1_ai-app

## 模块定位

`w1_ai-app` 是 Spring Boot 启动和依赖装配模块。它聚合 Trigger、Infrastructure、Paper 和各 Agent 模块，配置模型、线程池、Redis、定时任务所需 Bean，并产出最终可运行 JAR。

这里适合放跨模块装配，不适合放论文解析、Agent 决策或数据库业务规则。

## 目录结构

```text
w1_ai-app
├─ src/main/java/cn/winddol/ai/
│  ├─ Application.java
│  └─ config/                 Spring Bean 与配置属性
├─ src/main/resources/
│  ├─ application.yml
│  ├─ application-dev.yml
│  ├─ application-prod.yml
│  ├─ logback-spring.xml
│  └─ db/manual/              手工执行的数据库变更脚本
└─ src/test/                  单元测试与外部集成测试
```

## Java 文件说明

| 文件 | 作用 |
| --- | --- |
| `Application.java` | Spring Boot 主入口，开启组件扫描和定时能力。 |
| `config/AiConfig.java` | 创建语言模型、Embedding、Agent 工具等 AI 相关 Bean。 |
| `config/AgentFrameworkConfig.java` | 装配 Agent framework 所需实现。 |
| `config/RedisConfig.java` | Redis/Jedis 连接配置。 |
| `config/RedisChatMemoryStore.java` | 将 LangChain4j 会话记忆保存到 Redis。 |
| `config/AsyncConfig.java` | Spring 异步执行基础配置。 |
| `config/ThreadPoolConfig.java` | 通用业务线程池。 |
| `config/ThreadPoolConfigProperties.java` | 通用线程池配置映射。 |
| `config/PaperIngestionExecutorConfig.java` | 论文摄取专用有界线程池，隔离长时间 OCR 任务。 |
| `config/PaperIngestionExecutorProperties.java` | 摄取线程数、队列和关闭等待配置。 |
| `config/GuavaConfig.java` | Guava 相关基础 Bean。 |

## 资源说明

| 路径 | 作用 |
| --- | --- |
| `application.yml` | 公共配置和 Profile 选择。 |
| `application-dev.yml` | 本地开发配置，密钥通过环境变量注入。 |
| `application-prod.yml` | 生产环境配置覆盖。 |
| `db/manual/phase-1-paper-ingestion.sql` | 阶段 1 摄取任务与阶段运行记录表结构。 |
| `logback-spring.xml` | 日志格式、级别和输出配置。 |

## 测试约定

- `*UnitTest.java`：默认由 Surefire 执行，不应依赖真实外部服务。
- `*ExternalIT.java`：手动执行的 MonkeyOCR 或网络集成测试。
- 完整模块测试命令：`mvn test -pl w1_ai-app -am`。

## 运行依赖

- PostgreSQL + pgvector：论文、章节、向量和任务审计数据。
- Redis：会话记忆、锁及部分异步协作能力。
- MonkeyOCR HTTP 服务：PDF 转 Markdown。
- LLM 与 Embedding 服务：由环境变量配置 API Key 和地址。

摄取 Worker 使用独立心跳调度器续租。可通过 `PAPER_INGEST_HEARTBEAT_SECONDS` 调整心跳间隔，但应保持远小于 `PAPER_INGEST_LEASE_MINUTES`。
