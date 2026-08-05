# ADR 0004 — AI 调用隔离用 Resilience4j Bulkhead，预算治理用注解 + AOP

- **状态**：接受
- **日期**：2026-07-26
- **决策者**：后端架构
- **标签**：`ai` `resilience4j` `bulkhead` `token-budget` `aop` `observability`

---

## 上下文（Context）

EasyOrange 的 AI 工程化叙事承诺「可降级、可观测」，但 6 个 AI 决策点（估值/审核/文案/客服/语义搜索/自动上架）存在两个未落地的工程缺口：

1. **并发隔离缺失**：LLM/Vision 调用慢且耗资源。若 6 个 AI 场景共享线程池，任一供应商变慢（DeepSeek P99 飙到 30s）会拖垮所有 AI 端点，甚至通过线程池传导到商品/订单等核心链路。项目已引入 Resilience4j 的 CircuitBreaker + Retry（见 [Resilience4jConfig.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/config/resilience4j/Resilience4jConfig.java)），但缺少并发数上限隔离。
2. **Token 预算治理缺失**：`@TokenBudget` 注解已定义但零业务接入，6 个 AI service 直接调 LLM 无日预算上限。DeepSeek 按 token 计费，一次失控循环（如 Prompt 版本回退导致输出暴涨）可能产生数千元账单。这是「已声明未落地」与 AI 工程化叙事的直接冲突。

强制约束：

- **STP（Standard API First）**：优先复用已引入的 Resilience4j（`resilience4j-bulkhead`），不引新框架
- **可观测性**：Bulkhead 并发量、Token 预算用量必须独立暴露到 `/actuator/prometheus`
- **配置驱动**：预算限额运维可热更新，不绑死在代码里
- **YAGNI**：不引入 token 计数器（`LlmPort` 返回 `String`，精确计数成本高于收益）

## 决策（Decision）

AI 调用的并发隔离与预算治理分别采用以下方案：

### 1. Bulkhead 隔离 — Resilience4j `BulkheadRegistry` + 三个具名隔离仓

在 [Resilience4jConfig.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/config/resilience4j/Resilience4jConfig.java) 新增 `BulkheadRegistry`，声明三个隔离仓：

- `aiLlm`（并发 8）— DeepSeek LLM 调用
- `aiVision`（并发 4）— Qwen-VL 视觉调用（更慢更耗资源）
- `dbHeavy`（并发 16）— 预留给重查询场景

`maxWaitDuration=100ms`，超时抛 `BulkheadFullException`，调用方捕获后走 stale 缓存降级。`TaggedBulkheadMetrics` 自动绑定 `MeterRegistry`，暴露 `resilience4j_bulkhead_available_concurrent_calls` / `resilience4j_bulkhead_max_allowed_concurrent_calls` 指标。

[CachingLlmAdapter](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/CachingLlmAdapter.java) / `CachingVisionAdapter` 用 `Bulkhead.decorateSupplier(aiBulkhead, Retry.decorateSupplier(aiRetry, loader))` 包装 LLM/Vision 调用，隔离 + 重试叠加。`BulkheadFullException` 捕获后上报 `AiMetricsService.recordBulkheadRejected`。

### 2. Token 预算治理 — `@TokenBudget` 注解 + AOP 切面 + 配置覆盖

6 个 AI service 的公开方法标注 `@TokenBudget(scenario="...", maxTokensPerCall=2000, dailyTokenLimit=500_000)`。[TokenBudgetAspect](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/budget/TokenBudgetAspect.java) 切点为 `@annotation(tokenBudget)`，逻辑：

- **调用前**：从 `AiProperties.budget.scenarios.<scenario>` 读取配置覆盖值（缺失则回退注解默认值），检查 `累计用量 + maxTokensPerCall > dailyTokenLimit` 时抛 `TokenBudgetExceededException`
- **调用后**：以 `maxTokensPerCall` 作为预估用量记入 `TokenBudgetStore`（dev 用内存，prod 可换 Redis），上报 `easyorange.ai.token.budget.usage` / `easyorange.ai.token.budget.exceeded` 指标

配置优先级：`application.yaml` 的 `easyorange.ai.budget.scenarios` > 注解默认值。注解是编译期契约，配置是运行期调优旋钮。

## 后果（Consequences）

### 正向后果

- **故障隔离**：LLM 供应商变慢不会通过线程池传导到核心链路，AI 端点间互不干扰
- **成本可控**：日预算上限硬性拦截，失控循环最多消耗到 `dailyTokenLimit` 即停
- **可观测**：Bulkhead 并发量 + Token 用量/exceeded 独立指标，Prometheus 可直接告警
- **配置驱动**：预算限额运维改 yaml 即生效，无需发版；注解提供编译期可见的兜底契约

### 负向后果

- **预估不精确**：用 `maxTokensPerCall` 估算而非真实 token 数（`LlmPort` 返回 `String`），实际用量可能低于预算，导致预算用尽但实际还有余量
- **Bulkhead 容量需调优**：并发上限（8/4/16）是经验值，需结合压测调整；过低导致正常请求被拒，过高失去隔离意义
- **AOP 自调用陷阱**：`@TokenBudget` 必须加在 service 公开方法上（非内部私有方法），否则 Spring AOP 代理不生效

### 缓解措施

- 预估不精确：YAGNI，不引入 token 计数器；后续若接入返回 token 用量的供应商 API（如 OpenAI `usage` 字段），再升级为精确计数
- Bulkhead 调优：指标暴露后可观测实际并发，配合压测调整
- AOP 陷阱：注解只加在 service 公开方法，`@Around("@annotation(tokenBudget)")` 切点明确

## 备选方案（Alternatives Considered）

- **Sentinel**：拒绝。功能更全（流控 + 熔断 + 热点参数），但需引入独立 Dashboard（Sentinel Console）+ 规则存储，对当前项目规模过重；项目已用 Resilience4j 做 CircuitBreaker + Retry，Bulkhead 同属 Resilience4j 生态，零新增框架成本。
- **Hystrix**：拒绝。已停止维护（Netflix 2018 年进入维护模式），Spring Cloud 2024+ 不再集成。
- **Tomcat 线程池隔离**：拒绝。粒度太粗——Tomcat 线程池隔离的是整个 HTTP 请求，无法区分「LLM 调用」与「DB 查询」在同一请求内的并发占用；且会大幅降低吞吐。
- **Token 预算用拦截器而非 AOP**：拒绝。拦截器只能拦截 Controller 层，但 AI 调用发生在 service 层（`AiPricingService.suggestPrice` 等），且拦截器无法获取方法级 `scenario` 语义；AOP `@annotation` 切点天然与方法绑定，场景语义清晰。
- **Token 预算用 Spring `@RateLimiter`**：拒绝。`@RateLimiter` 控制的是 QPS 而非 token 用量，语义不符；且无法表达「日累计预算」。
- **引入 token 计数器（tiktoken-java）**：拒绝（YAGNI）。`LlmPort` 返回 `String`，需额外解析 LLM 响应计算 token；DeepSeek/Qwen 各有自己的 tokenizer，引入 `tiktoken-java` 对 GPT 系列准确但对国产模型不准，收益低于成本。

## 备注（Notes）

- 相关 ADR：[ADR 0003](./0003-ai-port-adapter-with-decorator.md)（AI Port/Adapter + 装饰器，本 ADR 的 Bulkhead 装饰在 `CachingLlmAdapter` 装饰器内部）
- 相关代码：[Resilience4jConfig.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/config/resilience4j/Resilience4jConfig.java)、[TokenBudgetAspect.java](file:///home/cartethyia/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/budget/TokenBudgetAspect.java)、[CachingLlmAdapter.java](file:///home/cartethyia/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/CachingLlmAdapter.java)
- 后续演进触发：当供应商返回 `usage` 字段时，升级 TokenBudget 为精确计数；当 AI 场景 > 10 个时，考虑按 `AiCallScope` 自动派发 Bulkhead 而非硬编码具名实例。
