# EasyOrange — LLM × DDD：Java 架构工程化实战

> **EasyOrange** — 在 DDD 六边形里装 LLM：可换供应商、可降级、可观测的 AI 工程化落地。
>
> **11 模块全解耦 · 2,419 测试守卫 · 32 Port 接口 · 11 RabbitMQ 消费者 · 6 ADR · Domain 层行覆盖 84.1%**
>
> 业务聚焦核心流程（C2C 资产流转：固定价格 + 直发 + 平台不碰货），把复杂度留给架构与 AI 工程化。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-ED8B00)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB)](https://react.dev/)

## 🔑 双岗位适配面板（HR/面试官一眼匹配）

| 🎯 AI Agent / 大模型 Java 后端 | 🏛 Java 架构师 / 高级后端 |
|---|---|
| ✅ LLM/Vision Port/Adapter 隔离，供应商可换 | ✅ DDD 六边形 + 32 Port 编译期隔离 |
| ✅ 轻量级 Agent 编排（4 路并行 Tool Calling） | ✅ Saga 状态机 + 反向补偿（9 状态持久化） |
| ✅ L1 Caffeine + L2 Redis 多级缓存 + stale 降级 | ✅ Spring Modulith Outbox + DLQ 三级重试 |
| ✅ Redisson 令牌桶限流 + Bulkhead 隔离舱 | ✅ Resilience4j 熔断 + 限流 + 降级 + 幂等 |
| ✅ Prompt YAML 版本化 + @TokenBudget 日预算 | ✅ ArchUnit 6 条规则 + ADR 5 条决策记录 |
| ✅ AiMetrics 可观测 + Prometheus 指标 | ✅ JaCoCo + PIT 变异测试双重门禁 |
| ✅ EmbeddingPort + Semantic Rerank（RAG 轻量版） | ✅ 全链路 traceId（HTTP → MQ → @Async → MDC） |
| ✅ 多模态 Vision（拍照识别自动上架） | ✅ SlowSql 检测 + Redis 多级缓存一致性 |

## 四个并列钩子

| 钩子 | 数字锚点 | 一句话 |
|---|---|---|
| **AI 工程化** | 6 决策点 + 7 件套 + 轻量 Agent | Port/Adapter + L1/L2 多级缓存 + Redisson 令牌桶 + stale 降级 + AiMetrics + Prompt YAML + TokenBudget + Bulkhead + 4 路并行 Tool Calling |
| **分布式可靠性** | Saga + Outbox + DLQ 三级重试 + 11 消费者 | 订单跨模块 Saga 编排 + 反向补偿；领域事件走 Spring Modulith Outbox → RabbitMQ；DLQ 指数退避 + terminal 转储 |
| **架构落地** | 11 模块 / 32 Port / DDD+CQRS+事件驱动 / ArchUnit 6 规则 | domain 层零框架依赖，编译期隔离；message/favorite 故意不做 CQRS（ADR-0002） |
| **质量门禁** | 2,419 测试 / JaCoCo + PIT / Biome 0 errors | 后端 ArchUnit + JaCoCo 行≥80% + PIT 变异测试；前端 Vitest + Playwright + Biome |

## 项目定位

**EasyOrange — LLM × DDD 工程化实战**：在 DDD 六边形架构里集成 LLM，让 AI 链路可换供应商、可降级、可观测。一套代码，双岗位叙事——投「AI Agent / 大模型 Java 后端」看 AI 工程化 + 轻量 Agent；投「Java 架构师」看 DDD + 分布式可靠性 + 架构治理。

### 核心矛盾

DDD 铁律要求 domain 层零框架依赖，但 LLM 调用昂贵且不稳定，必须有多级缓存 + 限流降级 + 可观测。解法：Port/Adapter + 装饰器。`LlmPort` / `VisionPort` 在 domain 层，`@Primary` 装饰器 `CachingLlmAdapter` 在 adapter 层包装 DeepSeek 实现 L1 Caffeine + L2 Redis 多级缓存；`AiRateLimitInterceptor` 令牌桶按端点限流，超限返回 stale 缓存而非 429；`AiMetricsService` 暴露缓存命中率 / LLM p99 / 限流计数到 Prometheus；Prompt YAML 版本化 + `@TokenBudget` AOP 日预算 + Resilience4j Bulkhead 隔离舱，6 个 AI 决策点全链路覆盖。

### 轻量级 Agent 编排（AI Agent 岗叙事）

[`AiSearchEnhancerAdapter`](./easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/outbound/AiSearchEnhancerAdapter.java) 是手写轻量 Agent Planner（没用 LangChain4j/Spring AI，避免框架黑盒，面试能讲清每一层实现）：

```
用户自然语言查询
  ├─ Tool 1: LLM 意图识别（找货/比价/问答）→ 路由
  ├─ Tool 2: 商品标签生成（品类/成色/价格区间）
  ├─ Tool 3: 市场分析（基于历史成交统计）
  └─ Tool 4: 建议问题生成（自动追问模糊需求）
```

- 4 路 `CompletableFuture` 并行（虚拟线程 `ForkJoinPool.commonPool()`）
- 单步骤 5s 超时，失败降级不影响整体
- 每路 Tool 复用同一套 LLM 缓存/限流/预算链路

### 架构治理三板斧（Java 架构师岗叙事） + 拒绝项清单

> 加分点：不是列「我用了什么」，是讲「我评估过什么、为什么不用」。

| 层 | 机制 | 解决的问题 |
|---|---|---|
| **决策层** | 6 条 ADR（[`doc/adr/`](doc/adr/)） | 记录「为什么 + 拒绝项」，不让选型沦为偏好 |
| **守卫层** | ArchUnit 6 条（[`ArchitectureRulesTest`](./easyorange-backend/easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java)） | CI 阻断违规：domain 零框架 / port 必配 adapter / CQRS 分离 |
| **验证层** | JaCoCo + PIT 变异测试 | JaCoCo 看「代码跑过」，PIT 注入变异看「测试能否发现缺陷」 |

| 被拒绝方案 | 原因 | 替代方案 | ADR |
|---|---|---|---|
| 2PC / XA / Seata AT | 强一致锁表久 + 连接池代理侵入 | Saga + 9 状态持久化 + 超时检测 + 人工介入 | [ADR-0001](doc/adr/0001-use-saga-over-2pc.md) |
| 全模块 CQRS | message/favorite 读写均衡，收益 < 维护成本 | 仅 product/order/payment/message 4 模块做 | [ADR-0002](doc/adr/0002-cqrs-scope-only-4-modules.md) |
| LangChain4j / Spring AI | Tool 调用反射黑盒 + 升级兼容差 | 手写 AiSearchEnhancer 4 路 Tool Registry，复用缓存限流 | [ADR-0003](doc/adr/0003-ai-port-adapter-with-decorator.md) |
| Seata TCC | 3× try-confirm-cancel 样板 + 业务侵入 | Saga 逆序补偿 + restoreStock/cancelOrder | [ADR-0001](doc/adr/0001-use-saga-over-2pc.md) |
| Milvus / PGVector | SKU < 10 万，向量库 ROI 低 | ES BM25 召回 + LLM semantic rerank（RAG 轻量版） | 隐含决策 |
| **Kafka / Pulsar 作为默认 MQ** | ① Kafka 无原生 DLQ，三级重试要自造；② 1 事件→11 独立消费者模型不匹配；③ Pulsar 本地要 3 容器（Broker+BK+ZK）太重 | **RabbitMQ Topic Exchange + 队列级 DLQ（默认）**；NATS JetStream 留作 `@ConditionalOnProperty` 备选 Adapter | [ADR-0005](doc/adr/0005-messaging-bus-select-rabbitmq-over-kafka-nats-pulsar.md) |

C2C 资产流转业务载体（固定价格 + 直发 + 平台不碰货）详见 [PRODUCT_DIRECTION.md](./PRODUCT_DIRECTION.md)。

## 架构总览

```mermaid
graph TB
    FE["React 19 前端"]
    APP["easyorange-application · Spring Boot 4"]
    USER[user] PROD["product + ES"] ORD["order + Saga"]
    PAY["payment + 幂等"] MSG["message + STOMP"]
    FAV[favorite] ADMIN[admin] AI["ai · 6 决策点 + Agent"]
    MQ[("RabbitMQ · 12c + DLQ")] DB[("MySQL 8.4 · 30 表")]
    REDIS[("Redis 7.4")] ES[("ES 8 可选")] LLM["DeepSeek + 通义 VL"]
    FE --> APP --> USER & PROD & ORD & PAY & MSG & FAV & ADMIN & AI
    AI --> LLM PROD --> ES ORD & PAY -.事件.-> MQ -.事件.-> PROD & PAY
    USER & PROD & ORD & PAY --> DB AI --> REDIS
```

```mermaid
stateDiagram-v2
    [*] --> PENDING --> ORDER_CREATED --> PAYMENT_CREATED --> COMPLETED --> [*]
    ORDER_CREATED --> COMPENSATING:失败 PAYMENT_CREATED --> COMPENSATING:失败
    COMPENSATING --> COMPENSATED:补偿成功 COMPENSATING --> FAILED:补偿异常
    FAILED --> TIMEOUT:30min TIMEOUT --> MANUAL_INTERVENTION:retry≥3
```

**Saga + 事件可靠性 + traceId 全链路**：`CreateOrderSaga` 按 productId 排序加锁防死锁 → 建单 → 扣库存 → 建支付；失败逆序补偿 `restoreStock → cancelOrder`。事件走 Spring Modulith Outbox（业务表 + `EVENT_PUBLICATION` 原子写入）→ 异步 externalize 到 RabbitMQ → `EventConsumerHandler` 统一幂等/metrics/DLQ → `DlqRetryScheduler` 每 5min 重投或转储 terminal。traceId：Brave TracingFilter → MDC → `MdcTaskDecorator` 传 @Async → MQ message header → 消费者 MDC。

> 细节：[架构-系统架构.md](doc/架构/架构-系统架构.md) · [ADR-0001](doc/adr/0001-use-saga-over-2pc.md) · [ADR-0003](doc/adr/0003-ai-port-adapter-with-decorator.md) · [ADR-0004](doc/adr/0004-ai-bulkhead-and-token-budget.md)

## 技术栈

后端 `Java 25 + Spring Boot 4 + MyBatis-Plus 3.5 + Spring Security OAuth2 + JWT` · 前端 `React 19 + TS 5 + Vite 8 + TanStack Query 5 + Zustand 5 + Tailwind 4 + shadcn/ui` · 数据/消息 `MySQL 8.4 + Redis 7.4 + RabbitMQ 3.13 + ES 8(可选)` · AI `DeepSeek + 通义千问 VL` · 可靠性 `Resilience4j + Redisson` · 可观测 `Micrometer + Brave + AiMetrics + SlowSql + StructuredLog` · DevOps `Docker + Flyway 11 + GitHub Actions`

## 快速开始

```bash
git clone https://gitee.com/cartethyia_XLS/easy-orange.git && cd easy-orange
docker compose -f compose.yaml up -d                               # MySQL/Redis/RabbitMQ
./mvnw install -DskipTests && ./mvnw spring-boot:run -pl easyorange-application   # :8080
cd easyorange-frontend && npm install && npm run dev               # :5173
```

> 零配置启动，详见 [AGENTS.md](./AGENTS.md)。后端 11 模块 / 前端 1042 测试 / 32 Port 接口，数字单一来源 [doc/工程指标.md](doc/工程指标.md)

## 模块 · AI 工程化 · 可靠性 · 质量门禁

| 维度 | 详情 |
|---|---|
| **11 Maven 模块** | common / framework / user / product(CQRS+审核+举报+ES) / order(CQRS+Saga) / payment(CQRS) / message(WS) / favorite / ai(Port+Agent) / admin / application(入口+Flyway+ArchUnit) |
| **AI 工程化 8 项** | 供应商 Port 隔离 · L1/L2 多级缓存装饰器 · Redisson 令牌桶 + stale 降级 · @TokenBudget 日预算 · Prompt 6 个 YAML · Resilience4j Bulkhead(8/4/16) · AiSearchEnhancer 4 路并行 Tool Calling · CompletableFuture 单步骤超时降级 |
| **分布式可靠性** | Saga 9 状态机 + 超时检测 · Outbox 原子写 EVENT_PUBLICATION · DLQ 三级重试(指数退避 1/5/15min + terminal) · EventConsumerHandler 统一幂等/metrics · traceId 全链路 · JWT + 黑名单吊销 · RateLimitFilter(GET 本地 / 写 Redisson) · @Idempotent(24h) · AuditLogAspect Outbox · OWASP CVSS≥8 阻断 |
| **质量门禁** | ArchUnit 6 条 · JaCoCo Domain 84.1% / 71.5% · PIT order(70/89/81) product(70/79/92) · Biome 0 errors · Git hooks commit-msg + pre-commit |

前端：暖橙指挥中心 Admin 设计系统 · 120+ Portal/Dialog/Drawer/Sheet · 95 共享 UI + 107 Admin 组件 · 296 a11y 属性 · 171 TanStack Query hooks · 111 测试文件 / 1042 用例

## 面试快速导航

| 你投的岗位 | 先看这几节 | 被追问时跳 |
|---|---|---|
| **AI Agent / 大模型 Java 后端** | 双岗位适配面板 → 轻量级 Agent 编排 → AI 工程化 → 拒绝项清单 | ADR-0003 / ADR-0004 / [工程指标.md §四](doc/工程指标.md) |
| **Java 架构师 / 高级后端** | 架构治理三板斧 → 拒绝项清单 → 架构总览(Saga+Outbox+traceId) | ADR-0001 / ADR-0002 / ArchitectureRulesTest |
| **全栈** | 技术栈 → 快速开始 → 模块总览 | [API-速查.md](doc/集成/API-速查.md) |

## 部署 · 结构 · 贡献

```bash
docker compose up -d elasticsearch                # 可选 ES
cd easyorange-frontend && docker build -t easyorange-fe . && docker run -p 80:80 easyorange-fe
```

项目结构：`easyorange-backend/`(11 Maven) + `easyorange-frontend/`(React) + `doc/架构/` + `doc/集成/` + `doc/adr/`(6 ADR) + `PRODUCT_DIRECTION.md`（业务场景）+ `AGENTS.md`（编码规范）+ `CHANGELOG.md`

贡献：Conventional Commits · `main/develop/feature/*/bugfix/*` · 本地验证 `./mvnw test` + `npm test`，详见 [CONTRIBUTING.md](./CONTRIBUTING.md)

---

<div align="center">

**EasyOrange** · LLM × DDD：Java 架构工程化实战 · Java 25 + Spring Boot 4 · DDD + CQRS + Saga + 事件驱动 + AI 工程化 · [Gitee](https://gitee.com/cartethyia_XLS/easy-orange) · [更新日志](./CHANGELOG.md)

</div>
