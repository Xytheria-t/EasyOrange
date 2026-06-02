# AI 接口限流 + 缓存（省钱）— 设计文档

- **日期**: 2026-06-02
- **作者**: TraeAI 协作产出
- **目标**: 1-2 天交付，月省 30%+ AI 调用成本
- **范围**: `easyorange-ai` 模块 6 个未缓存的 AI 服务
- **状态**: 设计已确认，待实施

---

## 1. 背景与目标

`easyorange-ai` 模块当前调用 DeepSeek（文本）与通义千问 VL（视觉）共 7 个服务，其中 6 个完全没有缓存与限流保护：

| 服务 | 端点 | 强幂等性 | 当前成本风险 |
|------|------|---------|------------|
| AiPricingService | `POST /api/ai/pricing` | 强 | 中（按需触发） |
| AiReviewService | `POST /api/ai/review` | 强 | **高**（每条发布走一次） |
| AiQaService | `POST /api/ai/qa` | 弱（用户问题多变） | 中 |
| AiCopyGenerationService | `POST /api/ai/generate-copy` | 强 | 中 |
| AutoListingService | `POST /api/ai/auto-listing` | 强（同一组图） | **高**（每次拍照上架） |
| SemanticSearchService | `GET /api/ai/semantic-search` | 强（关键词固定） | 中（每次搜索） |
| AiSearchEnhancer | 内部（搜索增强） | 已有 5min 缓存 | — |

**目标量化指标**：
- 命中率 ≥ 30% → 等价节省 30%+ token 成本
- 任何单用户/IP 滥用不导致 OAI 账单爆掉
- 上线后单服务 P99 延迟不增加（缓存命中 < 5ms）

**非目标**（明确不做）：
- 不做语义向量级缓存（embedding 调用本身也花钱，复杂度过高）
- 不做主动失效（业务可接受 1h 内返回旧结果）
- 不动 `AiSearchEnhancer`（已有 5min 缓存）

---

## 2. 架构总览

横切关注点（缓存 + 限流）通过 **Decorator + HandlerInterceptor** 实现，业务代码（6 个 service）**零修改**：

```
                HTTP Request
                     │
                     ▼
      ┌──────────────────────────────┐
      │  RateLimitFilter (existing)  │  通用 API 限流 (30次/60s/IP)
      │  - 拦截所有 POST/PUT/DELETE  │
      └──────────────┬───────────────┘
                     ▼
      ┌──────────────────────────────┐
      │  AiRateLimitInterceptor (NEW)│  ★ AI 专属限流
      │  - 匹配 /api/ai/**           │  - 按端点独立令牌桶
      │  - userId / IP 双键           │  - 拒绝时查 stale 缓存
      │  - Redis INCR + EXPIRE 60s   │  - 无 stale → 429
      └──────────────┬───────────────┘
                     ▼
      ┌──────────────────────────────┐
      │  AiController                │
      │  POST /api/ai/review ...     │
      └──────────────┬───────────────┘
                     ▼
      ┌──────────────────────────────┐
      │  AiReviewService (未改动)    │
      │  llmPort.generateTextWithJson │
      └──────────────┬───────────────┘
                     ▼
                LlmPort (interface)
                     ▲
        ┌────────────┴─────────────┐
        │                          │
  DeepSeekLlmAdapter        CachingLlmAdapter (NEW, @Primary)
  (existing, 真实调用)        - 装饰 DeepSeekLlmAdapter
                             - L1: Caffeine 5min/10K
                             - L2: Redis tiered TTL
                             - fail-open

                VisionPort (interface)
                     ▲
        ┌────────────┴─────────────┐
        │                          │
  QwenVlVisionAdapter       CachingVisionAdapter (NEW, @Primary)
  (existing, 真实调用)        - 装饰 QwenVlVisionAdapter
                             - 同样两级缓存
                             - 图像 URL 排序后 md5 标准化
```

### 2.1 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 装饰位置 | `LlmPort` / `VisionPort` 接口层 | 6 service 零修改；新增 AI 自动受益 |
| 缓存键 | `ai:llm:v1:{scope}:{md5(model+system+sha256(user))}` | 精确到 prompt；scope 区分 TTL |
| L1 缓存 | Caffeine 5min / 10K entries | 进程内毫秒级，10K 足够校园场景 |
| L2 缓存 | Redis tiered TTL | 跨实例共享；fail-open 不阻塞 |
| 限流算法 | Redis INCR + EXPIRE 60s | 实现简单；原子；与项目已有 `RedisCache` 复用 |
| 超限降级 | 优先返回 stale 缓存，再 429 | 用户体验最佳；零额外 LLM 调用 |
| Stale 数据存储 | Caffeine 独立段 24h TTL | 不参与正常 L1 命中；专供降级 |

### 2.2 与项目现有模式的一致性

- **`@Primary` 装饰器** — 与 `PasswordEncoderAdapter` 模式完全一致（参见 AGENTS.md "Port/Adapter IntelliJ 误报"）
- **`RedisCache` 抽象复用** — L2 缓存直接调用现有 `RedisCache` 接口，零新依赖
- **`RateLimitFilter` 互补不冲突** — 通用限流保留；新增 Interceptor 只针对 `/api/ai/**` 子集
- **Fail-open 风格** — Redis 不可用时放行，与项目所有缓存读写一致

---

## 3. 组件设计

### 3.1 新增文件清单（6 个新文件 + 2 个配置改动）

```
easyorange-backend/easyorange-ai/
├── adapter/
│   ├── CachingLlmAdapter.java          ★ NEW - @Primary implements LlmPort
│   └── CachingVisionAdapter.java       ★ NEW - @Primary implements VisionPort
├── interceptor/
│   └── AiRateLimitInterceptor.java     ★ NEW - HandlerInterceptor
├── enums/
│   └── AiCallScope.java                ★ NEW - 6 个枚举值
├── service/
│   └── AiCacheService.java             ★ NEW - L1/L2/Stale 三段缓存管理
└── config/
    └── AiCacheConfig.java              ★ NEW - Caffeine + Interceptor 注册

改动文件：
- AiConfig.java           - 在 @Configuration 内注入 AiCacheConfig
- AiProperties.java       - 新增 cache/limit 配置段
- application.yaml        - 加 easyorange.ai.cache.* + easyorange.ai.rate-limit.*
```

### 3.2 `AiCallScope` 枚举

```java
public enum AiCallScope {
    PRICING       (1, 3600,  10),   // TTL 秒, 每分钟限流
    REVIEW        (1, 3600,  10),
    COPY          (1, 3600,  20),
    AUTO_LISTING  (1, 3600,   5),   // 视觉贵，少量
    SEMANTIC      (1, 3600,  30),
    QA            (1,  900,  20);   // Q&A 短 TTL

    private final int version;       // 缓存键版本，未来调 prompt 时 ++
    private final int ttlSeconds;
    private final int ratePerMinute;
    // ...
}
```

驱动 cache key 前缀 + Redis 限流桶名 + Caffeine 段名 + stale TTL（统一 24h），单一信息源。

### 3.3 `AiCacheService` 接口

```java
public interface AiCacheService {
    /** 正常路径：L1 → L2 → compute → 回填 */
    <T> T getOrCompute(AiCallScope scope, String promptFingerprint,
                       Class<T> type, Supplier<T> loader);

    /** 限流降级路径：仅查 L1 stale 段 */
    <T> Optional<T> getStale(AiCallScope scope, String promptFingerprint,
                             Class<T> type);
}
```

实现要点：
- **promptFingerprint** 由装饰器传入 `SHA-256(model + system + userMessage)`，截前 32 字符
- **L1 失败** → fallback L2，**L2 失败** → 不抛异常，返回 `null` 走 compute
- **回填顺序** 先 L2（保证跨实例可见）再 L1（保证下次本地命中）
- **Stale 段** 是独立 `Cache<String, Object>`，`expireAfterWrite=24h`，**只写不主动清理**

### 3.4 `CachingLlmAdapter` 实现骨架

```java
@Primary
@Component
@RequiredArgsConstructor
public class CachingLlmAdapter implements LlmPort {

    private final DeepSeekLlmAdapter delegate;       // 注入具体实现（@Primary 装饰）
    private final AiCacheService cache;
    private final ObjectMapper objectMapper;         // 用于 LLM 字符串响应的暂存

    @Override
    public String generateText(String system, String user) {
        return cached(scopeOfCaller(), fingerprint(system, user),
                      String.class, () -> delegate.generateText(system, user));
    }

    @Override
    public String generateTextWithJson(String system, String user) {
        return cached(scopeOfCaller(), fingerprint(system, user),
                      String.class, () -> delegate.generateTextWithJson(system, user));
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        // embedding 不缓存（每条 query 都应取最新向量）
        return delegate.generateEmbedding(text);
    }

    private <T> T cached(AiCallScope scope, String fp, Class<T> type, Supplier<T> loader) {
        return cache.getOrCompute(scope, fp, type, loader);
    }

    private static AiCallScope scopeOfCaller() {
        // StackWalker 取直接调用方类名，映射到枚举
        return AiCallScope.fromCallerClass(
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                       .getCallerClass());
    }
}
```

**重要**：`CachingLlmAdapter` 通过构造器注入 `DeepSeekLlmAdapter` 具体实现，避免循环依赖。`StackWalker` 是 JDK 21+ 标准 API，Java 25 直接可用。

### 3.5 `AiRateLimitInterceptor` 实现骨架

```java
@Component
@RequiredArgsConstructor
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private final RedisCache redis;
    private final AiCacheService cache;
    private final ObjectMapper objectMapper;
    private final SecurityUtil securityUtil;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        if (!req.getRequestURI().startsWith("/api/ai/")) {
            return true;  // 非 AI 端点放行
        }

        AiCallScope scope = AiCallScope.fromUri(req.getRequestURI());
        String userKey = resolveUserKey(req);   // userId>IP
        String bucketKey = "ai:rl:" + scope.name().toLowerCase() + ":" + userKey;

        long count = redis.incrWithTtl(bucketKey, 60);  // 自定义原子操作
        if (count > scope.getRatePerMinute()) {
            // 尝试 stale 降级
            String fp = fingerprintFromRequest(req);
            Optional<String> stale = cache.getStale(scope, fp, String.class);
            if (stale.isPresent()) {
                writeJson(resp, 200, stale.get());   // 200 透明降级
                return false;
            }
            writeJson(resp, 429, errorJson("AI 服务繁忙，请稍后重试"));
            return false;
        }
        return true;
    }
}
```

**注册位置**：`AiCacheConfig` 实现 `WebMvcConfigurer.addInterceptors()`，interceptor order 设为 `0`（在通用 `JwtAuthenticationFilter` 之后）。

### 3.6 配置项（`application.yaml` 增量）

```yaml
easyorange:
  ai:
    cache:
      enabled: true
      l1:
        max-size: 10000
        expire-after-write-seconds: 300       # 5min
      stale:
        max-size: 5000
        expire-after-write-seconds: 86400     # 24h
    rate-limit:
      enabled: true
      fail-open: true                         # Redis 不可用时放行
      user-header: X-User-Id                  # 优先从 header 拿
```

---

## 4. 数据流

### 4.1 缓存命中（最优路径）

```
User → POST /api/ai/review
   → AiRateLimitInterceptor.preHandle
       INCR ai:rl:review:user:123 = 5/10 ✓
   → AiReviewService.reviewProduct
       → llmPort.generateTextWithJson(...)
           → CachingLlmAdapter (decorator)
               L1.get("ai:llm:v1:review:abc123") → HIT
               return cached
   → Response (2ms)
```

### 4.2 缓存未命中 → LLM 调用 → 回填

```
User → POST /api/ai/review
   → Interceptor: INCR = 1/10 ✓
   → Service → Adapter
       L1 miss → L2.get → miss
       → delegate.generateTextWithJson(...)   [800ms]
       → cache.set (L2 + L1)
   → Response (800ms)
```

### 4.3 限流 + Stale 降级

```
User (10次/分内) → POST /api/ai/review
   → Interceptor: INCR = 11/10 ✗
       → cache.getStale("ai:llm:v1:review:abc123") → HIT
       → writeJson(200, staleResult)
   → Response (5ms)  ← 透明降级，用户无感
```

### 4.4 限流 + 无 Stale → 429

```
User (10次/分内) → POST /api/ai/review
   → Interceptor: INCR = 11/10 ✗
       → cache.getStale → miss
       → writeJson(429, "AI 服务繁忙，请稍后重试")
   → Response (5ms)
```

---

## 5. 错误处理

| 失败场景 | 行为 | 设计依据 |
|---------|------|---------|
| Redis 不可用（限流） | 放行（fail-open） | 与项目 `RateLimitFilter` 一致 |
| Redis 不可用（缓存 L2） | 跳过 L2 走 compute | fail-open，不阻塞业务 |
| Caffeine 不可用（L1） | 极小概率（进程内） | 直接走 L2，等同 Redis-only |
| LLM 调用超时 | 不写缓存，下次重试 | 避免污染缓存 |
| LLM 返回 null | 不写缓存，下次重试 | 避免污染缓存 |
| `StackWalker` 解析调用方失败 | 默认 `GENERAL` scope，TTL 30min | 防御性 fallback |
| 限流桶 INCR 但 EXPIRE 未设 | 桶被永久占用 → 监控告警 | RedisTemplate 原子操作保证 |

**关键原则**：所有失败**不阻塞业务**，不抛异常。LLM 调用本身失败照原逻辑返回。

---

## 6. 可观测性

复用项目 `slf4j` + `Micrometer`（`spring-boot-starter-actuator` 已引入）：

| 指标名 | 类型 | 标签 |
|--------|------|------|
| `ai_cache_hits_total` | Counter | `scope`, `level` (L1/L2) |
| `ai_cache_misses_total` | Counter | `scope` |
| `ai_cache_stale_served_total` | Counter | `scope` |
| `ai_rate_limit_rejected_total` | Counter | `scope` |
| `ai_rate_limit_failopen_total` | Counter | `scope` |

所有指标通过 `MeterRegistry` 在装饰器 / 拦截器中打点。**不引入新依赖**。

---

## 7. 测试策略

### 7.1 单元测试（不依赖外部服务）

| 测试类 | 覆盖场景 |
|--------|---------|
| `AiCacheServiceTest` | L1 命中/L2 命中/全 miss/fail-open/回填顺序 |
| `CachingLlmAdapterTest` | 装饰器委托正确性、scope 推断、stale 路径 |
| `CachingVisionAdapterTest` | 图像 URL 排序后 md5 一致性 |
| `AiRateLimitInterceptorTest` | 命中/未命中/超限/stale 降级/429/fail-open |
| `AiCallScopeTest` | URI → scope 映射、版本号 |

**Mock 工具**：Mockito（项目已有）
**断言**：AssertJ（项目已有）

### 7.2 集成测试（Testcontainers，@Tag("integration")）

| 测试类 | 验证 |
|--------|------|
| `CachingLlmAdapterIT` | 真实 Redis 写入/读取；L1+L2 协同 |
| `AiRateLimitInterceptorIT` | MockMvc + 真实 Redis 桶；计数器正确性 |

### 7.3 架构守卫

`easyorange-application/src/test/.../ArchitectureRulesTest.java` 新增 1 条规则：
- `AiCallScope` 枚举值数量 ≥ 实际 AI 端点数（防止新增端点忘记注册 scope）

### 7.4 手动验证（上线前必跑）

| 场景 | 期望 | 验证方法 |
|------|------|---------|
| 同一商品 2 次 review | 第 2 次 < 10ms 返回 | curl + time |
| 11 次/分钟 review | 第 11 次返回 stale 或 429 | bash 循环 |
| Redis 停机 | AI 服务仍可用 | `docker stop redis` 后调用 |
| Cache key 不冲突 | 6 个 scope key 前缀独立 | Redis CLI `KEYS ai:llm:*` |

---

## 8. 风险与缓解

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| 缓存 key 冲突（不同 service 同 prompt） | 低 | 低 | scope 名前缀隔离 + version=v1 |
| Stale 数据返回导致业务异常 | 中 | 中 | 监控 stale_served 占比；>20% 告警 |
| Caffeine OOM（误配置 max-size） | 低 | 高 | 默认 10K，可配置，配置错误启动失败 |
| 限流误伤合法用户 | 中 | 中 | 默认 10次/分，对校园场景宽松；监控 rejected 占比 |
| `StackWalker` 性能开销 | 极低 | 极低 | 实测 < 1μs/调用 |
| LLM 返回大 JSON 占用缓存空间 | 低 | 中 | L1 限制单 entry 1MB（拒绝回填），L2 走 Redis 序列化 |

---

## 9. 实施计划（1-2 天）

按 `writing-plans` skill 拆任务，每任务 1-4 小时：

| 任务 | 产出 | 估时 |
|------|------|------|
| 1. `AiCallScope` 枚举 + `AiProperties` 配置段 | 配置驱动基础 | 1h |
| 2. `AiCacheService` + 单元测试 | L1/L2/Stale 基础 | 2h |
| 3. `CachingLlmAdapter` + 单元测试 | 文本 LLM 装饰 | 2h |
| 4. `CachingVisionAdapter` + 单元测试 | 视觉 LLM 装饰 | 1h |
| 5. `AiRateLimitInterceptor` + 单元测试 | 限流 + 降级 | 2h |
| 6. `AiCacheConfig` + 拦截器注册 + 启动验证 | 装配完成 | 1h |
| 7. 应用配置 + 启动 + 手动验证 | 上线就绪 | 2h |
| 8. 集成测试 + 架构守卫规则 | 自动化保障 | 2h |

总计 ~13h，1-2 天可控。

---

## 10. 验收标准

- [ ] 6 个 AI 端点全部接入（不改 service 业务代码）
- [ ] 同一商品 2 次 review：第 2 次 < 10ms
- [ ] 连续 11 次 review/min：第 11 次返回 200 stale 或 429
- [ ] Redis 停机：所有 AI 端点仍可用
- [ ] 单元测试覆盖 ≥ 80%
- [ ] ArchUnit 测试通过
- [ ] 上线后 24h 监控：`ai_cache_hits_total / (ai_cache_hits_total + ai_cache_misses_total) ≥ 30%`

---

## 附录 A：与现有 `RateLimitFilter` 的边界

| 维度 | `RateLimitFilter` (existing) | `AiRateLimitInterceptor` (NEW) |
|------|------------------------------|--------------------------------|
| 粒度 | 通用 API（POST 全拦截） | AI 专属（按端点独立桶） |
| 频率 | 30次/60秒/IP | 5-30次/60秒/用户（按端点） |
| 降级 | 拒绝 | stale 缓存 → 拒绝 |
| 配置 | `rate-limit-filter.rules` | `easyorange.ai.rate-limit.*` |

两者**串联工作**：先过 Filter（粗粒度）再过 Interceptor（细粒度）。同一请求 2 次计数（Filter 算 1 次通用配额，Interceptor 算 1 次 AI 配额），互不冲突。

## 附录 B：缓存键规范

```
Cache key:  ai:llm:v1:{scope}:{fp32}
Stale key:  ai:llm:stale:v1:{scope}:{fp32}
Rate key:   ai:rl:{scope}:{userKey}

其中：
- scope     = AiCallScope 枚举名（小写）
- fp32      = SHA-256(model + system + userMessage)[:32]
- userKey   = "user:{userId}" 或 "ip:{remoteAddr}"
```

**模型升级时**：`version` 从 `v1` 升 `v2`，自然失效旧 key，无需手动清理。
**Prompt 调整时**：同版本下 fingerprint 变化，自动失效。
