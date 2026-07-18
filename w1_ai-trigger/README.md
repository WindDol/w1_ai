# w1_ai-trigger

## 模块定位

`w1_ai-trigger` 是系统的入站适配层，把 HTTP 请求和定时触发转换成应用接口调用。它负责参数映射、DTO 转换和协议状态码，不负责实现论文摄取、检索或 Agent 推理。

## 目录结构

```text
cn.winddol.ai.trigger
├─ http/  HTTP Controller 实现
└─ job/   定时任务和恢复触发器
```

## 文件说明

| 文件 | 作用 |
| --- | --- |
| `http/PaperController.java` | 实现论文上传、任务状态、失败重试、阶段重跑和论文查询接口。 |
| `http/ResearchController.java` | 接收研究问答请求并调用 ResearchOrchestrator。 |
| `http/ScientificResearchController.java` | 暴露论文检索、Outline、章节和引用查询能力。 |
| `job/PaperIngestRecoveryJob.java` | 周期扫描中断或遗留的摄取任务并重新分发。 |
| `job/package-info.java` | 定时触发包说明。 |

## 主要调用关系

```text
HTTP Client
  -> w1_ai-api 接口
  -> trigger/http Controller
  -> paper 或 agent 的应用接口

Spring Scheduler
  -> trigger/job
  -> IPaperApplication.recoverInterruptedJobs()
```

## 设计约束

- Controller 不直接调用 Mapper、Repository 实现或 MonkeyOCR。
- HTTP DTO 与领域对象的转换在边界完成。
- 长任务只提交并返回 `jobId`，不能占用 HTTP 线程等待 OCR 完成。

