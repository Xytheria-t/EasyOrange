# EasyOrange — Java AI Agent 工程化实战

> **EasyOrange** — 按生产级标准做 LLM Agent 工程化：多范式工具编排（Workflow 式并行扇出 + 自治式 Agent 工具循环）· RAG 检索增强（两路召回 + RRF 融合）· 评估闭环进 CI · 限流 / Token 预算 / stale 降级 · LLM 专用可观测——AI 链路**可换供应商、可降级、可观测、可评估**。
>
> **Maven 多模块解耦 · Port 接口编译期隔离 · 事件驱动 + DLQ 三级重试 · ADR 决策记录 · 全量测试守卫 · AI 两条主线链路 × 8 项工程化**
>
> 业务载体：C2C 资产流转（固定价格 + 直发 + 平台不碰货），把复杂度留给 AI 工程化与架构落地。

[![License](https://img.shields.io/badge/license-MulanPSL--2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-ED8B00)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-6DB33F)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB)](https://react.dev/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-6DB33F)](https://spring.io/projects/spring-ai)

## 正在迭代（2026 Q4）

- **评估与数字补测**：Agent 循环三口径（平均步数 / 降级率 / 步级延迟 p95）+ trace 覆盖率实测回填，Langfuse 面板演示录屏

## 业务边界（刻意聚焦）

| 维度 | 聚焦决策 | 原因 |
|---|---|---|
| 价格机制 | **固定价格** | 议价 / 阶梯降价会引入复杂状态机，侵蚀架构主线 |
| 物流与资金 | **C2C 直发、不经手资金** | 平台不碰货、不囤货，规避支付牌照与备付金合规复杂度；支付仅是「记账 + 状态推进」 |

> 「资产」是广义概念：实物（数码 3C / 图书 / 服饰）+ 虚拟数字资产（会员 / 游戏账号 / 素材）+ 权益类（健身卡 / 课程兑换码）。平台不议价、不持有库存，资产方按固定价格上架。

## AI 应用工程化（主线）

### 核心矛盾与解法

DDD 铁律要求 domain 层零框架依赖，但 LLM 调用昂贵且不稳定。解法：**AI 基础设施全面框架化为 Spring AI**（[ADR-0008](doc/adr/0008-ai-spring-ai-framework.md)）——LLM / Embedding 调用点直接注入 `ChatModel` / `EmbeddingModel` bean（DeepSeek + Qwen-VL + DashScope，统一 OpenAI 兼容协议），供应商可换只改配置；业务级治理保留：Redisson 令牌桶限流（超限 429）、`@TokenBudget` 日预算 AOP、供应商故障 stale 兜底、Prompt YAML 版本化。

### 两条 AI 主线链路

| 链路 | 端到端流程 |
|---|---|
| 卖家「发布助手」 | 拍照识别单入口（**一次多模态调用**产出属性 + 建议价 + 标题 / 描述），建议快照随创建请求落库，供字段级采纳率统计 |
| 买家「对话式找货」 | 搜索增强 → 对话式检索：同一套 RAG 链路换语料（知识库规则 + 在售资产） |

> 早期口径「6 个决策点（4 LLM + 2 规则）」已收敛为上表两条链路：独立的智能估值 / 文案生成入口因产出与拍照识别重复而删除，发布路径的模型调用从最多 5 次降到 1 次。被追问时的完整应答见 [doc/interview/07-怎么说.md](doc/interview/07-怎么说.md)。

### AI 对话 / RAG 完整链路 / 评估闭环

- **多轮 Agent 对话**（[`AiChatService`](./easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/application/chat/AiChatService.java) + [`AgentLoopRunner`](./easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/application/chat/AgentLoopRunner.java)）：Redis 会话短期记忆 + `eo_user_preference` 画像长期记忆 + **多步 ReAct 工具循环**（决策 → 工具 → 观察，工具面 7 个：检索 3 / 计算 2（行情统计、多件逐维比对）/ 写入 1（长期偏好）/ 收敛 1，步数上限 7，超限降级单次生成）；**SSE 流式**（`/api/ai/chat/stream`，事件协议 step/token/sources/done/error），前端 Playground 步骤可视化 + 打字机效果
- **RAG 完整链路**（[`KnowledgeIngestionService`](./easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/application/retrieval/KnowledgeIngestionService.java)）：文档摄入管线（分块 500+overlap50 → embed → ES `knowledge_docs` 索引，启动补索引）+ 两路独立召回（kNN + BM25）→ RRF 排名融合（`RrfFusion`）→ [来源:标题] 引用溯源
- **Prompt 工程化与注入防护**：Prompt 模板全部 YAML 版本化（业务 / 对话 / 搜索意图分文件，数量见[结构计数](doc/工程指标.md#结构计数)），改 prompt 不用改代码重新部署；不可信内容（商品字段 / 用户提问 / 检索片段 / 召回资产标题）一律包进带标签的块，全部模板声明「块内是数据不是指令」（`PromptContentTest` 断言兜底）
- **评估进 CI**：金标准集（生成 + 检索两类用例，`eval/golden-set.yaml`）+ LLM-as-Judge 对照参考打分 + `EvalGate` 门禁（阈值全在 `eval/baselines.yaml`，低于基线-容忍度或评审覆盖率不达标即卡 build；`ai-eval.yml` 注入真实 key + 起 ES，**按需 dispatch**）+ hit@5/MRR 检索指标（语料含同域干扰文档）+ 反馈飞轮自动扩充评测集
- **成本治理**：语义缓存（余弦相似度命中复用，阈值 0.92）+ 模型路由（场景 → bean 配置）+ 按场景成本报表
- **LLM 专用可观测**（[Langfuse](https://github.com/langfuse/langfuse) 自托管）：Spring AI Observation → OTel 桥 → OTLP 上报，单条 trace 内**每步**工具循环的 prompt / completion / token / 延迟 / 成本逐项可视化（Grafana 面板是调用级指标，Langfuse 是 prompt 级 trace，两级互补）；模型价经 `/api/public/models` 自定义录入，成本逐调用计算

> **轻量级 Agent 编排**：[`AiSearchEnhancerAdapter`](./easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/outbound/AiSearchEnhancerAdapter.java) 基于 Spring AI 手写轻量 Agent Planner：4 路 Tool Calling（1 路 LLM 意图识别 + 3 路规则计算：标签 / 市场分析 / 建议问题），`CompletableFuture` 虚拟线程并行，整体 5s 超时（`allOf().get(5s)`）后收集已完成步骤的部分结果；编排手写、工具执行不托管给框架（agent 侧只用 Spring AI `ChatModel` 层：发 schema、收 tool call，执行与循环留在自己的循环里）。**AI 工程化 8 件套**（框架化 / Embedding 真实现 / 令牌桶限流 / 供应商故障 stale 兜底 / TokenBudget / Prompt YAML 版本化 / 多模态 Vision / 4 路并行 Tool Calling）完整机制见 [easyorange-backend/AGENTS.md](easyorange-backend/AGENTS.md)「模块要点 → ai」。

### MCP 工具面 — 对外开放（streamable HTTP，`/mcp`）

Spring AI `@McpTool` 暴露 4 个**公开只读**工具，外部 MCP client 可实时查平台数据：

| 工具 | 语义 |
|---|---|
| `search_products` | 在售资产检索：kNN + BM25 两路召回 → RRF 融合（与站内搜索同一套 RAG 检索底座） |
| `get_product_detail` | 按资产 ID 查详情（描述 / 成色 / 卖家 / 在售状态） |
| `list_categories` | 类目逐层浏览（含各类目在售资产数） |
| `search_platform_knowledge` | 平台规则知识库检索（交易流程 / 担保支付 / 退换规则） |

**信任边界（两级暴露）**：与 Agent 内部工具（`AgentLoopRunner` 工具面）是两级独立暴露——外部 MCP client 无用户上下文，只挂公开只读数据，**不暴露订单 / 个人信息 / 写路径**；匿名可达但纳入统一限流（60 次/分/IP），防重豁免（JSON-RPC 超时重试复用同一请求体属协议内合法行为）。

**接入**（先启动后端，见「快速开始」）：

- Cursor：`~/.cursor/mcp.json` 加入
  ```json
  { "mcpServers": { "easyorange": { "url": "http://localhost:8080/mcp" } } }
  ```
- Claude Desktop：Settings → Connectors → Add custom connector，URL 填 `http://localhost:8080/mcp`
- 验证：`curl -X POST http://localhost:8080/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"t","version":"0"}}}'`

## 工程底座：架构与可靠性

支撑 AI 主线落地的架构与可靠性底座——工程素养证据。投模型应用 / AI Agent 岗扫过即可，投 Java 后端岗从这里深挖。

### 架构总览

```mermaid
flowchart TB
    FE["React 前端"]
    APP["easyorange-application · Spring Boot"]
    USER["user · 认证/用户"]
    PROD["product · CQRS + ES 搜索"]
    ORD["order · 单事务 + 分布式锁"]
    PAY["payment · CQRS + 幂等"]
    MSG["message · WebSocket"]
    ADMIN["admin · 管理端"]
    AI["ai · Spring AI + Agent"]
    MQ[("RabbitMQ · 事件消费者 + DLQ")]
    DB[("MySQL")]
    REDIS[("Redis · 缓存 / 令牌桶 / 锁")]
    ES[("Elasticsearch · 可选")]
    LLM["DeepSeek / Qwen-VL / DashScope"]

    FE --> APP
    APP --> USER
    APP --> PROD
    APP --> ORD
    APP --> PAY
    APP --> MSG
    APP --> FAV
    APP --> ADMIN
    APP --> AI

    USER --> DB
    PROD --> DB
    ORD --> DB
    PAY --> DB
    PROD --> ES
    ORD --> REDIS
    AI --> REDIS
    AI --> LLM

    PROD -. "Outbox 事件" .-> MQ
    ORD -. "Outbox 事件" .-> MQ
    PAY -. "Outbox 事件" .-> MQ
    MQ -. "异步消费" .-> PROD
    MQ -. "异步消费" .-> ORD
    MQ -. "异步消费" .-> PAY
```

- **前端**：React SPA，C 端 + 管理端（统一设计系统）双布局
- **后端**：Spring Boot 聚合 Maven 多模块，DDD 六边形 + CQRS 分层（CQRS 范围决策见 [ADR-0002](doc/adr/0002-cqrs-scope-4-modules.md)）
- **数据**：MySQL（Flyway 迁移）+ Redis（缓存 / 令牌桶 / 分布式锁 / 会话）+ Elasticsearch（BM25 + kNN）
- **消息**：Spring Modulith Outbox → RabbitMQ Topic Exchange，每个业务模块独占队列的消费者，DLQ 三级重试
- **AI**：DeepSeek（Chat）/ Qwen-VL（Vision）/ DashScope（Embedding），统一 OpenAI 兼容协议

> 组件级细节见 [doc/agents/架构参考.md](doc/agents/架构参考.md)。

### 事件驱动：Outbox → RabbitMQ → DLQ

领域事件与应用事务**同原子**写入 `EVENT_PUBLICATION` → 异步 externalize 到 RabbitMQ Topic Exchange → 消费者各自独占队列 + `EventIdempotencyChecker` 精确一次 → 失败进队列级 DLQ（`DlqRetryScheduler` 5 分钟重投 <3 次，毒消息转储 `eo.dlq.terminal`）。审计日志同样走 Outbox。traceId 经 OTel 桥 → MDC → MQ header 全链路传递。

### 订单创建（拒绝 Saga）

下单在单一本地 `@Transactional` 内完成（订单 / 扣库存 / 支付 / Outbox 原子提交），Redisson 分布式锁按 productId 排序防死锁防超卖；任一步失败整体回滚，**无补偿路径**。取消 / 退款 / 完成等跨模块副作用由订单生命周期事件异步触发。详见 [ADR-0007](doc/adr/0007-order-local-tx-over-saga.md)。

### 架构治理

> 设计理念：不是列「我用了什么」，而是讲「我评估过什么、为什么不用」。

| 层 | 机制 | 解决的问题 |
|---|---|---|
| **决策层** | ADR 决策记录（[doc/adr/](doc/adr/)） | 记录「为什么 + 拒绝项」，不让选型沦为偏好 |
| **守卫层** | ArchUnit 依赖规则（[`ArchitectureRulesTest`](./easyorange-backend/easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java)） | CI 阻断违规：domain 零框架 / CQRS 读写分离 / 模块间端口隔离 / 端口必有适配器 / 禁止 infrastructure 包 |
| **验证层** | 全量测试 + JaCoCo + PIT 变异测试 | JaCoCo 看「代码跑过」，PIT 注入变异看「测试能否发现缺陷」；前端 Biome 0 errors |

### 拒绝项清单

| 被拒绝方案 | 原因 | 替代方案 | ADR |
|---|---|---|---|
| 2PC / XA / Seata AT | 强一致锁表久 + 连接池代理侵入 | 本地单事务 + Redisson 分布式锁 + Outbox | [ADR-0007](doc/adr/0007-order-local-tx-over-saga.md) |
| Saga 编排（跨模块补偿） | 单库下补偿与回滚重复、失败状态随事务回滚丢失 | 本地单事务 + 分布式锁 + Outbox | [ADR-0007](doc/adr/0007-order-local-tx-over-saga.md) |
| 全模块 CQRS | user / admin / ai 等读写比均衡或调用外部 API，收益 < 维护成本 | 仅 product / order / payment / message 4 模块 | [ADR-0002](doc/adr/0002-cqrs-scope-4-modules.md) |
| LangChain4j | 不是「反射黑盒」——两家 `@Tool` 都是运行时反射生成 schema，不是差异点；真实取舍是**循环控制权**：托管的工具执行循环（AI Services / `ChatClient` ToolCallingAdvisor）插不进步数上限、循环中途预算检查、按终止原因分类的降级 | 自持循环 + Spring AI `ChatModel` 层（只发 schema、不执行工具） | — （未评估为候选，无 ADR；选型见 [ADR-0008](doc/adr/0008-ai-spring-ai-framework.md)） |
| Milvus / PGVector | SKU < 10 万，向量库 ROI 低（ANN 建索引 / 调参 / 运维一整套换不来可感知收益） | ES 原生 kNN + BM25 两路独立召回，索引侧 RRF 排名融合（**不做余弦重排**） | [ADR-0012](doc/adr/0012-rag-hybrid-retrieval-rrf.md) |
| Kafka / Pulsar 默认 MQ | Kafka 无原生 DLQ；单事件扇出到多消费者的模型不匹配；Pulsar 本地太重 | RabbitMQ Topic Exchange + 队列级 DLQ | [ADR-0005](doc/adr/0005-messaging-rabbitmq.md) |

## 技术栈

| 层 | 技术 |
|---|---|
| **后端** | Java（容器内显式 G1，非 ZGC）· Spring Boot（虚拟线程默认启用）· MyBatis-Plus（逻辑删除 / 乐观锁 / 分页）· MapStruct · OpenRewrite |
| **安全** | Spring Security OAuth2 Resource Server · **双 Token**：RSA 签名 Access（30min 无状态）+ Opaque Refresh（Redis SHA-256，HttpOnly Cookie，轮换 + 复用检测）· BCrypt |
| **前端** | React · TypeScript · Vite · React Router · TanStack Query · Zustand · Tailwind CSS · shadcn/ui · react-hook-form + Zod · Framer Motion · Biome · Playwright |
| **数据 / 消息** | MySQL（utf8mb4 / InnoDB）· Redis（业务缓存单层，Caffeine 仅用于 stale / 图片处理等专用本地缓存）· RabbitMQ（Topic Exchange + Quorum Queue）· Elasticsearch（dev / prod 默认启用，关掉走 LIKE 兜底；**版本硬锁**见 `infra/elasticsearch/Dockerfile` 注释） |
| **AI** | Spring AI · DeepSeek · Qwen-VL · DashScope Embedding · MCP server（`@McpTool` 公开只读工具面） |
| **可靠性** | Redisson（分布式锁 / 令牌桶）· Spring Modulith Outbox · CacheErrorHandler fail-open · UUID v7 主键 |
| **可观测** | Micrometer + Prometheus · OpenTelemetry（traceId → Langfuse）· Spring AI Observation · StructuredLogEncoder（prod 输出 logstash JSON） |
| **DevOps** | Docker / docker-compose（多阶段构建，非 root 运行）· GitHub Actions · Flyway（DDL / DML 分离） |

> **版本单一来源**：后端依赖见 [`easyorange-backend/pom.xml`](easyorange-backend/pom.xml)、前端见 [`easyorange-frontend/package.json`](easyorange-frontend/package.json)、运行时中间件见 [`compose.yaml`](compose.yaml) 与 `infra/` 镜像 tag——**文档一律不复刻版本号**。选型理由与拒绝项见 [doc/adr/](doc/adr/) 与上文「拒绝项清单」。

## 模块结构

| 模块 | 核心定位 |
|---|---|
| **application** | 启动聚合层：主入口、Flyway、跨模块适配器、ArchUnit 守卫 |
| **common** | 统一响应体 / 异常 / 领域事件接口 / 工具 |
| **framework** | Security（双 Token）/ Redis / MyBatis / RabbitMQ / 限流 / 审计 AOP |
| **user** | 认证（双 Token）、用户资料 |
| **product** | 商品（CQRS）+ 审核 + ES 搜索 + 语义检索 |
| **order** | 订单（CQRS）+ 单事务 + 分布式锁 + 生命周期事件 |
| **payment** | 支付（CQRS）+ 幂等 + Mock 网关 |
| **message** | 消息（CQRS）+ 站内信 + WebSocket / STOMP 实时沟通 |
| **ai** | Spring AI 框架化 + Agent 编排 + 令牌桶 / 预算 / Prompt |
| **admin** | 后台 API（用户 / 商品审核 / 订单 / 统计） |

> 各模块「能放什么」与依赖规则见 [doc/agents/架构参考.md](doc/agents/架构参考.md)。

## 快速开始

```bash
git clone https://github.com/Xytheria-t/EasyOrange.git && cd EasyOrange
docker compose -f compose.yaml up -d                               # MySQL / Redis / RabbitMQ
docker compose --profile search up -d elasticsearch                # ES（IK 分词，首次构建镜像略慢）
                                                                   # dev 默认启用检索，起后端前必须先起 ES，否则启动期建索引失败
cd easyorange-backend && ./mvnw install -DskipTests && ./mvnw spring-boot:run -pl easyorange-application   # :8080
cd easyorange-frontend && npm install && npm run dev               # :5173

# 压测 / 多实例 / 可观测（详见 doc/工程指标.md §2.3；fullstack profile 下裸 up -d 不受影响）
docker compose --profile fullstack up -d --build --scale easyorange-app=2  # 后端多实例（nginx 自动 LB；ES 同 profile 随 app 拉起）
docker compose up -d prometheus grafana                                    # Prometheus :9090 + Grafana :3000
docker compose --profile langfuse up -d                                    # Langfuse LLM 可观测（UI :3001，trace 级 prompt/token/成本）
k6 run --vus 50 --duration 30s load-tests/product-list.js                  # k6 压测（阈值 p95<500ms 内置）
```

> 零配置启动：全部变量在 `application*.yaml` / `compose.yaml` 内都有开发默认值，**不建 `.env` 也能启动**；需要覆盖默认值时复制 `.env.example` → `.env`。完整命令（PIT 变异测试、JaCoCo、OWASP、E2E）见 [doc/agents/常用命令.md](doc/agents/常用命令.md)。

## 项目结构

```
easy-orange/
├── easyorange-backend/     # Spring Boot 后端（Maven 多模块，DDD 六边形）
├── easyorange-frontend/    # React 前端（C 端 + 管理端）
├── doc/                    # 技术栈 / ADR / agents 参考 / DATABASE / 面试
├── compose.yaml            # MySQL + Redis + RabbitMQ + 后端应用（多实例）+ Prometheus + Grafana + Langfuse
├── infra/                  # 基础设施即代码（Prometheus / Grafana provisioning / ES IK 镜像）
├── k8s/                    # K8s 部署（kustomize，无状态应用层）
└── load-tests/             # k6 压测脚本
```

## 文档地图

| 资源 | 内容 |
|---|---|
| [AGENTS.md](./AGENTS.md) | 唯一规范来源 + 参考索引；后端 / 前端编码约定见各自目录下的 [AGENTS.md](./easyorange-backend/AGENTS.md) |
| [doc/adr/](doc/adr/) | 架构决策记录 |
| [doc/agents/](doc/agents/) | 按需读取参考：架构（错误码 / 依赖边 / 异常 / 可观测）/ 领域 / 常用命令 |
| [doc/工程指标.md](doc/工程指标.md) | 数字单一事实来源：测试数 / 覆盖率 / 压测（**收口重测后回填**）+ [结构计数](doc/工程指标.md#结构计数) |
| [doc/DATABASE.md](doc/DATABASE.md) | 数据库全局约定、表清单、Flyway 迁移规范与脚本索引 |
| [doc/interview/](doc/interview/) | 面试脚本 7 册（[入口](doc/interview/README.md)）：备战清单 / 代码走读 / 模拟题库 / 速答 / 设计题与手撕 / 工程底座 / 怎么说 |

## 贡献与许可

- **提交规范**：Conventional Commits（`commit-msg` hook 校验），单人开发直接在 `develop` 上提交——见 [AGENTS.md](./AGENTS.md#提交规范git-工作流)
- **安全**：漏洞报告与已实现的安全特性见 [SECURITY.md](./.github/SECURITY.md)
- **许可**：木兰宽松许可证第 2 版（[Mulan PSL v2](./LICENSE)）

---

<div align="center">

**EasyOrange** · Java AI Agent 工程化实战 · Java + Spring Boot + Spring AI · Agent 编排 + RAG + 评估闭环 + DDD + 事件驱动可靠性 · [GitHub](https://github.com/Xytheria-t/EasyOrange)

</div>
