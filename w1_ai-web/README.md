# ScholarBrain Web

阶段 4 的科研阅读工作台，默认连接本地 Java 应用 `http://127.0.0.1:8091`。

## 本地启动

```powershell
npm install
npm run dev
```

访问 `http://127.0.0.1:5173`。Vite 会把 `/api` 请求代理到 Java 应用，因此不需要额外配置 CORS。

## 功能分区

- 深度研究工作台：默认首页，由 ResearchAgent 驱动检索、章节阅读、符号/引用追踪和证据汇总。
- 阅读工作台：论文库、Outline、章节正文、符号表和参考文献联动。
- ResearchAgent：SSE 展示工具执行、最终回答和可回查证据，点击证据定位到原章节。
- 摄取任务：上传 PDF、轮询状态、阶段时间线、失败重试和指定阶段重跑。
- 关系审计：按已确认、待复核和审计历史展示 LibrarianAgent 结果。
- 产物审阅：查看原始 Markdown、归一化 Markdown、结构报告和元数据。

LibrarianAgent 不直接占据主工作流。它维护的关系数据由 ResearchAgent 在跨论文比较、创新性和冲突分析时按需使用，也可以在阅读模式的关系页中复查。

## 生产构建

```powershell
npm run build
```

构建结果位于 `dist/`。
