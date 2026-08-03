# ADR 0003 — AI 集成采用 Port/Adapter + 装饰器模式而非直接调用 LLM SDK

- **状态**：已替代（Superseded by ADR-0008）
- **日期**：2026-07-14
- **决策者**：后端架构
- **标签**：`ai` `hexagonal` `decorator` `cache` `observability`

---

## 上下文（Context）

EasyOrange 在「资产方 / 认领方双端」共 6 个 AI 决策点（智能估值 / 营销文案 / 信用画像×2 / 智能找货 / 物品评估），需要同时集成两类供应商：

- **DeepSeek**（文本 LLM）：估值、文案、找货增强、审核
- **通义千问 VL**（视觉模型）：拍照上架图片识别

强制约束（见 `doc/集成/AI-资产管理.md` 与 `README.md`「AI 工程化」节）：

- **供应商可替换**：项目定位是 LLM × DDD 工程化实战项目，必须能在 DeepSeek / Qwen-VL 之外低成本切换或叠加新供应商
- **LLM 调用昂贵且不稳定**：必须有多级缓存 + 限流降级，否则单次调用的成本与延迟不可控
- **可观测性是核心叙事**：项目对外宣传语明确提到「AiMetrics 可观测」，缓存命中率 / LLM 延迟 / 限流计数必须独立采集
- **领域层零框架依赖**（DDD 铁律，见 `easyorange-backend/AGENTS.md`）：AI 调用入口不能污染 domain 层

业务侧的边界（来自 `PRODUCT_DIRECTION.md` 与 `AGENTS.md`）：平台不议价、不自动调价，AI 走生产级工程实践（Port/Adapter + 多级缓存 + 限流降级 + AiMetrics + Prompt 版本化 + Token 预算）。所以 AI 调用结果**可缓存、可降级、可观测**比「实时精准」更重要。

6 个 AI 决策点对应不同的 `AiCallScope`（缓存分桶）：

| 决策点 | 触发模块 | Scope | 供应商 |
|--------|---------|-------|--------|
| 智能估值 | ai / product | PRICING | DeepSeek LLM |
| AI 营销文案 | ai / product | COPY | DeepSeek LLM |
| AI 信用画像（双端） | user | QA | DeepSeek LLM |
| AI 智能找货 | ai / product | SEARCH | DeepSeek LLM + ES |
| AI 物品评估 | ai / product | VISION | 通义千问 VL |
| 内容审核 | ai / admin | REVIEW | DeepSeek LLM |

每个 scope 独立配置 TTL，避免高频估值调用与低频审核共享同一缓存窗口。

## 决策（Decision）

AI 集成采用 **六边形 Port/Adapter + `@Primary` 装饰器模式**，业务侧只依赖 Port 接口，装饰器叠加缓存 / 限流 / 指标，**不直接调用 LLM SDK**。

层次结构：

1. **Port（领域契约，零框架依赖）**：[LlmPort.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/port/LlmPort.java)、[VisionPort.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/port/VisionPort.java)
2. **底层 Adapter（供应商实现）**：`DeepSeekLlmAdapter`、`PythonLlmAdapter`（均 `@Qualifier("rawLlm")` + `@ConditionalOnProperty` 二选一）、`QwenVlVisionAdapter`，直接调 RestClient + 厂商 API
3. **装饰器 Adapter（`@Primary`）**：[CachingLlmAdapter.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/CachingLlmAdapter.java)、`CachingVisionAdapter`，包裹底层 Adapter，叠加 L1 Caffeine + L2 Redis + stale 降级缓存
4. **横切关注点**：`AiRateLimitInterceptor`（Redis 令牌桶，fail-open）、[AiMetricsService.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/metrics/AiMetricsService.java)（4 类 Micrometer 指标）

关键实现要点：

- `CachingLlmAdapter` 注入 `@Qualifier("rawLlm") LlmPort`（供应商实现按 `easyorange.ai.provider` 二选一注册），自身 `@Primary @Component`，Spring 自动让业务侧拿到的是装饰器而非底层 Adapter
- 缓存键按 `AiCallScope`（PRICING / COPY / REVIEW / QA / SEARCH / VISION）分桶，每个 scope 独立 TTL
- stale 缓存（独立 Caffeine）在限流降级时返回旧值，避免直接 429
- `AiMetricsService` 暴露 `easyorange.ai.cache`（hit/miss/stale）、`easyorange.ai.llm.duration`、`easyorange.ai.vision.duration`、`easyorange.ai.ratelimit`（rejected/stale_served/fail_open）四类指标到 `/actuator/prometheus`

## 后果（Consequences）

### 正向后果

- 供应商可替换：新增供应商只需新增 `XxxLlmAdapter implements LlmPort`，业务侧零改动
- 横切能力可叠加：缓存 / 限流 / 指标都在装饰器与拦截器层，业务服务只感知 Port
- 测试友好：`CachingLlmAdapterTest` 直接 mock `@Qualifier("rawLlm") LlmPort`，无需启动真实 HTTP
- 可观测性完整：缓存命中率、LLM 延迟、限流计数独立采集，符合「AiMetrics 可观测」叙事
- DDD 边界守住：Port 在 `ai/port/` 下，domain 层不依赖任何 AI SDK

### 负向后果

- 类数量增加：每个供应商需要 Port + 底层 Adapter + 装饰器，对简单场景略重
- 装饰器链顺序敏感：缓存必须在限流之后（否则限流拒绝时缓存已查），需在配置中明确
- `@Primary` 与 IntelliJ 误报：项目踩坑记录中已说明，Adapter 实现类加 `@Primary` 解决（见 `easyorange-backend/AGENTS.md`「Port/Adapter / MapStruct IntelliJ 误报」）

### 缓解措施

- 装饰器构造器显式注入 `delegate`，顺序在代码中可见
- `AiCacheConfig` 集中声明 L1/L2/stale 三类缓存的容量与 TTL，单一配置入口
- 测试覆盖：`CachingLlmAdapterTest` / `CachingVisionAdapterTest` / `AiMetricsServiceTest` / `AiRateLimitInterceptorTest` 守卫关键路径

## 备选方案（Alternatives Considered）

- **直接在业务 Service 里调 LLM SDK**：拒绝。供应商耦合死，无法切换；缓存 / 限流 / 指标逻辑会散落到各 Service，无法统一管控；domain 层会被 SDK 污染，破坏六边形边界。
- **Spring Cache 抽象（`@Cacheable`）+ AOP 切面**：拒绝。`@Cacheable` 只能做单级缓存且语义是「方法返回值缓存」，无法表达「L1 miss 查 L2」「L2 miss 调 delegate」「限流时返回 stale」这类多级 + 降级组合；AOP 切面的执行顺序难以稳定控制，限流 / 缓存 / 指标的先后依赖在切面里调试成本高。
- **只做 Port/Adapter，不做装饰器**：拒绝。缓存与限流逻辑会落入底层 Adapter（`DeepSeekLlmAdapter`），导致供应商切换时缓存能力丢失；或落入业务 Service，污染 domain。装饰器是让横切能力与供应商实现解耦的最小成本方案。
- **用 Spring AI Starter**：拒绝（截至决策日）。Spring AI 1.0 在项目启动时（2025-11）尚未稳定，且其抽象更偏「ChatClient」统一调用，对多级缓存 + stale 降级 + 按 scope 分桶的细粒度控制支持不足；引入会与项目已有的 `MultiLevelCache` / `AiMetricsService` 重复造抽象。

## 备注（Notes）

- 相关文档：[doc/集成/AI-资产管理.md](file:///home/cartethyia/projects/Java/easy-orange/doc/集成/AI-资产管理.md)、[doc/架构/架构-系统架构.md](file:///home/cartethyia/projects/Java/easy-orange/doc/架构/架构-系统架构.md)「可观测性」表
- 相关代码：[LlmPort.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/port/LlmPort.java)、[CachingLlmAdapter.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/CachingLlmAdapter.java)、[AiMetricsService.java](file:///home/cartethyia/projects/Java/easy-orange/easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/metrics/AiMetricsService.java)
- 相关 ADR：[ADR 0002](./0002-cqrs-scope-only-4-modules.md)（ai 模块不上 CQRS，用 Port/Adapter + 装饰器替代）；**本 ADR 已被 [ADR 0008](./0008-ai-migrate-to-spring-ai-framework.md) 替代（2026-08-03，Spring AI 2.0 全面框架化）**
- 重评估触发：Spring AI 进入稳定版且能覆盖 L1/L2 + stale 降级 + 按 scope 指标时，重新评估是否迁移；或当供应商数量 > 3 时考虑引入策略路由替代 `@Primary` 单选。
