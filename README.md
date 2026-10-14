# ClipForge Agent

ClipForge Agent 是一个面向图文和短视频素材的多模态创作规划智能体项目。系统不会直接生成最终视频，而是把用户的创作目标、目标平台、知识库、图片、视频、字幕、记忆偏好等输入，转化为结构化、可执行、可评估的短视频方案或图文方案。

项目包含 Java 后端和 Vue 3 前端，适合作为 Agent 工程、RAG、多模态理解、异步任务编排和创作工作流的综合实践项目。

## 核心能力

- 创作方案生成：支持短视频结构化方案和图文结构化方案，输出分镜、旁白、字幕、正文段落、配图槽位、标题、发布文案、话题标签和改进建议。
- RAG 检索增强生成：支持文档上传、解析、切片、向量化、pgvector 检索、BM25 混合检索、Rerank 重排序。
- 多模态素材理解：支持图片理解、视频元数据读取、抽帧、音频提取、ASR 字幕识别、视频摘要。
- 记忆系统：支持会话上下文、长期偏好记忆、会话滚动摘要和记忆修正。
- 异步处理：使用 Redis 记录处理状态，使用 RocketMQ 承载文档、图片、视频等慢任务。
- 质量评估与观测：记录模型调用、工具调用、运行链路、质量复盘和成本统计。
- 前端工作台：提供首页演示、项目会话、素材中心、知识库、记忆、观测面板等页面。
- 内容产物导出：支持短视频方案、图文方案和 Markdown 导出，让系统从规划阶段进一步形成可交付文档产物。

## 技术栈

后端：

- Java 21
- Spring Boot 3.x
- Maven
- PostgreSQL + pgvector
- Redis
- RocketMQ
- LangChain4j
- Apache Tika
- FFmpeg

前端：

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Axios
- ECharts

## 项目结构

```text
.
├── clipforge-server                  # Spring Boot 启动模块和可运行包
├── clipforge-dependencies            # 第三方依赖版本管理
├── clipforge-common                  # 跨业务通用能力边界
├── clipforge-contract                # 跨模块契约边界
├── clipforge-ai-api                  # 模型 Client、Request/Response 和路由契约
├── clipforge-ai-infrastructure       # LangChain4j、百炼和模型调用基础设施实现
├── clipforge-observability           # Trace、模型用量和执行观测
├── clipforge-messaging               # RocketMQ 消息发送、消费和慢任务消息基础设施
├── clipforge-module-knowledge        # Knowledge/RAG 文档、切片、向量化、检索和 URL 同步
├── clipforge-module-material         # 图片、视频、抽帧、音频、ASR 和素材理解
├── clipforge-module-memory           # 短期上下文、长期记忆、摘要和记忆抽取
├── clipforge-module-creation         # 意图识别、创作工作流、Prompt、评估和导出
├── clipforge-frontend                # Vue 3 前端工程
│   ├── src                           # 前端源码
│   ├── public                        # 前端静态资源
│   └── package.json
├── pom.xml                           # Maven 多模块父工程
├── .gitignore                        # Git 忽略规则
└── 目录说明.md
```

当前后端已经完成从原单模块结构到多模块结构的收口。`clipforge-server` 负责启动、配置装配和
Controller 入口；业务能力分别沉淀在 Knowledge、Material、Memory、Creation 模块；模型调用、
消息队列、观测能力拆到独立基础设施模块。接口路径、请求响应结构、数据库表、Redis Key 和
RocketMQ 消息语义保持与重构前兼容，前端仍按原有 `/api/...` 路径访问后端。

运行时资源统一放在 `clipforge-server/src/main/resources`，所有 Prompt（提示词）文件统一放在
`clipforge-server/src/main/resources/prompts`，避免多模块环境下资源分散和同名覆盖。

说明：开发过程文档、测试素材、演示素材、本地运行产物不会提交到仓库，详见 `.gitignore`。

## 后端启动

准备基础环境：

- JDK 21
- Maven
- PostgreSQL，并启用 pgvector
- Redis
- RocketMQ
- FFmpeg

常用启动方式：

```bash
mvn -pl clipforge-server -am spring-boot:run
```

也可以先打包后运行：

```bash
mvn -DskipTests package
java -jar clipforge-server/target/clipforge-server-0.0.1-SNAPSHOT.jar
```

默认端口可通过 Spring Boot 配置调整。数据库、Redis、RocketMQ、模型服务地址和 API Key 建议通过环境变量或本地配置文件注入，不要提交真实密钥。

## 前端启动

进入前端目录：

```bash
cd clipforge-frontend
npm install
npm run dev
```

如需修改后端地址，请调整前端环境变量或前端 API 基础地址配置，使其指向正在运行的后端服务。

## 主要页面

- 首页演示：用于快速体验虎鲸科普短视频创作规划流程。
- 项目会话：按项目管理多轮创作会话，查看历史对话和最近创作结果。
- 素材中心：上传和查看图片、视频素材，观察素材处理状态和理解结果。
- 知识库：管理知识库、文档、切片、处理日志和 URL 同步。
- 记忆：查看和修正用户偏好、项目偏好和会话摘要。
- 观测面板：查看运行链路、模型调用、质量复盘和成本统计。

## 公开仓库说明

本仓库只提交项目源码和必要配置模板，不提交以下内容：

- 开发阶段文档
- 测试图片、视频、音频、知识库文件
- 本地数据库清理脚本之外的临时数据
- 构建产物
- IDE 配置
- 日志和上传文件
- API Key、数据库密码等敏感信息

如果需要本地演示素材，请自行放入前端或后端约定的数据目录，并通过本地配置引用。
