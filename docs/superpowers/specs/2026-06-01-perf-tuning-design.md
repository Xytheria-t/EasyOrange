# EasyOrange 性能优化设计文档

| 字段 | 值 |
|---|---|
| 文档版本 | 1.0 |
| 创建日期 | 2026-06-01 |
| 作者 | Brainstorming Session |
| 状态 | 待评审 |
| 实施范围 | 1.3 前端 Bundle / 1.2 缓存 / 1.1 数据库 / 1.4 AI 成本 |
| 实施顺序 | 1.3 → 1.2 → 1.1 → 1.4 |
| 目标覆盖率 | 80% 行覆盖（新增公共类 100%） |
| 架构约束 | ArchUnit 守卫不破、DDD 分层不破 |

---

## 1. 背景与目标

EasyOrange 是 Java 25 + Spring Boot 4 + React 19 的校园二手交易平台，已经具备 2,546 后端 + 947 前端测试用例、11 模块 DDD 架构。本设计针对运行期性能瓶颈，分 4 块共 15 个改动点。每个块单独一个 PR，可独立上线和回滚。

**通用约束**：
- 不破坏 ArchUnit `ArchitectureRulesTest`
- 新增公共类 100% 单测覆盖
- 现有 2,546 后端 + 947 前端测试用例全部保持 PASS
- 不修改 `eo_order_item.product_snapshot`（订单历史数据完整性，移入未来 backlog）
- 保留 Conventional Commits 规范

---

## 2. 模块 1.3 — 前端 Bundle 优化（首个 PR）

### 2.1 目标

| 指标 | 优化前（估算） | 优化后（目标） |
|---|---|---|
| 首屏 vendor chunk（gzip） | ~280 KB | < 180 KB |
| `vendor-icons` chunk | 含全部 lucide-react 图标 | 仅打包用到的图标，体积 ↓ ≥ 40% |
| `recharts` 引入 | 静态打入主 bundle | 独立懒加载 chunk，仅管理端统计页加载 |
| `build.target` | `es2020` | `es2022`（Java 25 时代默认） |

### 2.2 改动清单

#### 改动 1.3-A：recharts 懒加载

**现状**：`recharts` 静态 `import` 进入主 bundle，约 150 KB gzip。

**方案**：用 `React.lazy` + `Suspense` 把管理端统计页图表组件单独 chunk。

**新增/修改文件**：
- `easyorange-frontend/src/components/admin/charts/StatsCharts.lazy.tsx`（新）—— `React.lazy` 包装器
- `easyorange-frontend/src/components/admin/charts/StatsCharts.tsx`（改造）—— 改为默认导出图表组件
- `easyorange-frontend/src/pages/admin/StatsPage.tsx`（改造）—— 使用 `<Suspense fallback={<ChartSkeleton/>}>` 包裹

**关键代码**：
```tsx
// StatsCharts.lazy.tsx
import { lazy, Suspense } from 'react';
const StatsCharts = lazy(() => import('./StatsCharts'));
export default function StatsChartsLazy(props: Props) {
    return <Suspense fallback={<ChartSkeleton/>}><StatsCharts {...props}/></Suspense>;
}
```

#### 改动 1.3-B：lucide-react 按需导入 + ESLint 守卫

**现状**：项目已用 Vite manualChunks 拆出 `vendor-icons`，但若代码用 `import * from 'lucide-react'` 会全量引入。

**方案**：
1. ESLint 加 `no-restricted-imports` 规则禁止默认/全量导入
2. 全项目扫描并修复违规代码

**修改文件**：
- `easyorange-frontend/eslint.config.js`（加规则）
- 修复所有违规文件（`eslint --fix` 自动）

**ESLint 规则**：
```js
{
    files: ['src/**/*.{ts,tsx}'],
    rules: {
        'no-restricted-imports': ['error', {
            paths: [{
                name: 'lucide-react',
                importNames: ['default'],
                message: '请用具名导入以支持 tree-shaking: import { Icon } from "lucide-react"'
            }, {
                name: 'lucide-react',
                importNames: ['*'],
                message: '请用具名导入以支持 tree-shaking'
            }]
        }]
    }
}
```

#### 改动 1.3-C：路由级 code splitting

**现状**：`App.tsx` 路由表已部分使用 `lazy`，需要全量确认。

**方案**：所有非首屏页面路由统一 `lazy()` 化。

**修改文件**：
- `easyorange-frontend/src/App.tsx`（路由表 lazy 化）
- `easyorange-frontend/src/router/index.tsx`（如存在，统一处理）

#### 改动 1.3-D：Vite 构建配置升级

**修改文件**：`easyorange-frontend/vite.config.ts`
- `build.target: 'es2020'` → `'es2022'`
- 启用 `cssCodeSplit: true`（按需 CSS 拆分）

### 2.3 测试

- **单元测试**：`StatsCharts.lazy.test.tsx` 验证 fallback、加载完成后正常渲染
- **E2E 测试**：`tests/e2e/admin-stats.spec.ts`（新）—— Playwright 验证管理端统计页正常加载
- **构建验证**：`npm run build:analyze` 输出 `dist/stats.html`，人工核对 chunk 分布

### 2.4 验收标准

- [ ] `npm run build:analyze` 中 `recharts` 出现在独立 chunk（不是 `vendor`）
- [ ] `vendor-icons` chunk 体积下降 ≥ 40%
- [ ] 首屏 vendor chunk（gzip）减小 ≥ 100 KB
- [ ] ESLint 扫描零违规
- [ ] Playwright 管理端统计页 E2E PASS
- [ ] `npm run typecheck` + `npm run test` 全绿

---

## 3. 模块 1.2 — 缓存层加固（第二个 PR）

### 3.1 目标

| 指标 | 优化前 | 优化后 |
|---|---|---|
| 缓存击穿（冷门商品） | 高并发时 N 次 DB 回源 | 1 次 DB 回源（双层锁串行） |
| 缓存预热 | 启动后冷启动 QPS 低 | 启动时预热分类树/热门关键词 |
| Caffeine 监控 | 无指标 | Prometheus 暴露 hit/miss/eviction |
| Redis 大 Key | 收藏列表/图片列表可能成为大 Key | 拆分 Hash/ZSet + 扫描审计 |

### 3.2 改动清单

#### 改动 1.2-A：缓存击穿防护（双层锁）

**现状**：`MultiLevelCache.get()` 在 L1/L2 都 miss 时并发调用 `loader.load()`，N 个并发请求会触发 N 次 DB 查询。

**方案**：
1. `MultiLevelCache` 新增 `getWithLock(key, type, loader)` 方法
2. L1 锁：`synchronized (key.intern())` —— 单 JVM 内串行 loader
3. L2 锁：Redis SETNX 锁（`eo:cache:lock:{key}`），TTL 5 秒，cluster 级串行
4. `loader.load()` 完成后双写 L1 + L2

**修改文件**：
- `easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/cache/MultiLevelCache.java`
- `easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/cache/loader/CacheLock.java`（新，封装 SETNX + Lua 释放）

**关键代码**：
```java
public <T> T getWithLock(String key, Class<T> type, CacheLoader<T> loader, long lockTtlMs) {
    Object l1 = l1Cache.getIfPresent(key);
    if (l1 != null) return (T) l1;

    T l2 = redisCache.get(buildL2Key(key), type);
    if (l2 != null) {
        l1Cache.put(key, l2);
        return l2;
    }

    synchronized (key.intern()) {
        // double-check
        T retry = redisCache.get(buildL2Key(key), type);
        if (retry != null) return retry;
        String lockValue = UUID.randomUUID().toString();
        if (!cacheLock.tryLock("mlc:lock:" + key, lockValue, lockTtlMs)) {
            // 等待 50ms 后重试 L1/L2
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            return get(key, type, loader);
        }
        try {
            T loaded = loader.load();
            if (loaded != null) put(key, loaded);
            return loaded;
        } finally {
            cacheLock.unlock("mlc:lock:" + key, lockValue);
        }
    }
}
```

#### 改动 1.2-B：缓存预热

**方案**：新增 `CacheWarmupRunner implements ApplicationRunner`，启动后异步预热高频缓存。

**预热内容**：
1. 分类树（`eo:product:categories:all`）
2. 商品状态计数（`eo:product:status:count`）
3. 热门搜索关键词 Top 100（近 7 天 MySQL 聚合 + 写缓存）

**新增文件**：
- `easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/runners/CacheWarmupRunner.java`
- `@Profile("!test")` 排除测试环境
- `@Async("domainEventExecutor")` 异步执行

#### 改动 1.2-C：Caffeine Micrometer 指标

**修改文件**：`easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/config/cache/LocalCacheConfig.java`

**关键改动**：
```java
return Caffeine.newBuilder()
    .recordStats()  // 启用统计
    .maximumSize(...)
    .build();

@Bean
public MeterBinder l1CacheMetrics(@Qualifier("l1Cache") Cache<String, Object> l1Cache) {
    return registry -> CaffeineCacheMetrics.monitor(registry, l1Cache, "easyorangeL1Cache");
}
```

**导出指标**：
- `cache_gets_total{result="hit"}`
- `cache_gets_total{result="miss"}`
- `cache_evictions_total`
- `cache_size`

#### 改动 1.2-D：Redis 大 Key 治理

**方案**：
1. **图片 URL 列表拆分**：商品详情里的图片列表从单 Key String 改为 Hash 字段（如 `eo:product:images:{productId}` 存 `idx → url` 映射），用 `HGETALL` 读取
2. **用户收藏列表**：从 Set 改为 ZSet（按 `favoriteTime` 分数），单用户上限 1000 条
3. **大 Key 扫描工具**：新增 `RedisCache.scanLargeKeys(thresholdBytes)`，定期扫描 > 10KB 的 Key 输出到 `bigkey_audit.log`

**修改文件**：
- `easyorange-backend/easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/outbound/cache/CategoryCacheAdapter.java`（用 `getWithLock`）
- `easyorange-backend/easyorange-favorite/src/main/java/com/cartethyia/easyorange/favorite/adapter/outbound/cache/FavoriteCacheAdapter.java`（改 ZSet）
- `easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/redis/impl/RedisCacheImpl.java`（加 `scanLargeKeys`）

### 3.3 测试

- `MultiLevelCacheTest`（已有，加 `getWithLock` 并发用例 + `getWithLock_concurrentAccessOnlyLoadsOnce`）
- `CacheLockTest`（用 Testcontainers Redis，验证 SETNX 互斥）
- `CacheWarmupRunnerTest`（验证预热调用顺序 + `@Profile("!test")` 行为）
- `BigKeyScannerTest`（用 Testcontainers，注入大 Key 验证扫描）

### 3.4 验收标准

- [ ] 1000 并发访问同一冷门商品 → DB 查询次数 = 1
- [ ] actuator/prometheus 可见 `easyorangeL1Cache_*` 指标
- [ ] 启动日志显示 `CacheWarmupRunner` 完成预热
- [ ] `bigkey_audit.log` 存在且无 > 10KB 漏网 Key
- [ ] 现有 `MultiLevelCacheTest` 仍 PASS（向后兼容）

---

## 4. 模块 1.1 — 数据库层调优（第三个 PR）

### 4.1 目标

| 指标 | 优化前 | 优化后 |
|---|---|---|
| `eo_product` 表行数 | 含 del_flag=2 历史 | 仅活跃商品，季度归档到 `eo_product_archive` |
| 慢查询 | 无监控 | > 200ms 自动 WARN 日志 |
| HikariCP 告警 | 无 | pending > 10 / active > 45 时 health=DEGRADED |

### 4.2 改动清单

#### 改动 1.1-A：冷热分离（`eo_product` → `eo_product_archive`）

**新增 SQL 迁移**：

**`V4__create_eo_product_archive.sql`**：
```sql
CREATE TABLE `eo_product_archive` (
    -- 与 eo_product 同结构
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    -- ... 省略其他字段
    `archived_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`id`),
    KEY `idx_archive_user_time` (`user_id`, `create_time` DESC),
    KEY `idx_archive_status` (`status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品归档表';
```

**`V5__archive_old_products.sql`**（可重复，季度执行）：
```sql
-- 归档 del_flag=2 且 update_time < DATE_SUB(NOW(), INTERVAL 90 DAY) 的商品
-- 注：eo_product_archive 的列结构与 eo_product 完全一致 + archived_at 列在末尾
-- 实际 SQL 在 Java 端使用分批(每批 10000 行) + Service 拼装实现，避免单条大 SQL
-- 此处仅展示核心逻辑
INSERT INTO eo_product_archive
    (id, user_id, category_id, name, price, original_price, stock, status,
     view_count, condition_level, location, contact_method, tags, search_text,
     price_update_time, create_time, update_time, create_by, update_by,
     del_flag, version, archived_at)
SELECT id, user_id, category_id, name, price, original_price, stock, status,
       view_count, condition_level, location, contact_method, tags, search_text,
       price_update_time, create_time, update_time, create_by, update_by,
       del_flag, version, NOW()
FROM eo_product
WHERE del_flag = 2
  AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)
LIMIT 10000;

-- 对应的 DELETE 在 Java 端按 id 列表执行
```

**实现细节（重要）**：
- 不直接用单条 INSERT/DELETE 大 SQL（生产环境会锁表）
- Java 端 `ProductArchiveService.archive()` 方法：
  - 分批 SELECT id 列表（每批 10000 行）
  - 对每批：先 INSERT INTO archive ... SELECT ... WHERE id IN (...) → 再 DELETE FROM product WHERE id IN (...)
  - 批次间 sleep 100ms 让出锁
  - 全程记录 archive_count 指标（Micrometer）

**新增 Java 类**：
- `easyorange-application/.../runners/ProductArchiveService.java` —— 业务逻辑包装
- `easyorange-application/.../runners/ProductArchiveScheduler.java` —— `@Scheduled(cron="0 0 3 1 */3 *")` 季度任务
- 归档前 7 天发 MQ 通知（`ProductArchiveNoticeEvent`），给运维预留窗口

#### 改动 1.1-B：慢查询监控

**方案**：自定义 AOP 切面拦截 MyBatis-Plus `BaseMapper` 执行，记录 > 阈值耗时。

**新增文件**：
- `easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/config/database/SlowQueryMonitor.java`（AOP）
- `easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/config/properties/SlowQueryProperties.java`（`@ConfigurationProperties`）

**配置（application.yaml）**：
```yaml
easyorange:
  slow-query:
    enabled: true
    threshold-ms: 200
    sample-rate: 1.0
```

**关键代码**：
```java
@Aspect
@Component
@ConditionalOnProperty(name = "easyorange.slow-query.enabled", havingValue = "true")
public class SlowQueryMonitor {
    private static final Logger log = LoggerFactory.getLogger("SLOW_SQL");
    @Around("execution(* com.baomidou.mybatisplus.core.mapper.BaseMapper.*(..))")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > properties.getThresholdMs()) {
                log.warn("SLOW_SQL mapper={} method={} cost={}ms",
                    pjp.getTarget().getClass().getSimpleName(),
                    pjp.getSignature().getName(), cost);
            }
        }
    }
}
```

#### 改动 1.1-C：HikariCP 指标 + 健康告警

**方案**：
1. Spring Boot 4 默认已开启 `HikariCP → Micrometer` 桥接（`management.metrics.enable.hikaricp=true`）
2. 新增 `HikariPoolAlertPublisher implements HealthIndicator`，监控 pending/active 连接数

**新增文件**：
- `easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/health/HikariPoolAlertPublisher.java`

**告警规则**：
- `pending > 10` OR `active > 45` → health=DEGRADED
- `pending > 20` OR `active > 48` → health=DOWN

**不动 `eo_order_item.product_snapshot`**：因订单历史数据完整性要求，本期仅在设计文档中记录为"未来 backlog"。

### 4.3 测试

- `ProductArchiveServiceTest`（Testcontainers 集成测试）：准备 1000 条 del_flag=2 数据，验证归档后 eo_product 行数 ↓、eo_product_archive 行数 ↑
- `SlowQueryMonitorTest`：注入 mock 慢 Mapper，验证日志输出
- `HikariPoolAlertPublisherTest`：mock HikariDataSource，验证不同连接数下 health 状态
- 迁移脚本验证：`mvn flyway:info` + 本地启动验证 V4/V5 可执行

### 4.4 验收标准

- [ ] V4 迁移可重复执行
- [ ] 季度归档任务 < 5 分钟完成（100 万行级）
- [ ] 主表 `EXPLAIN` 命中索引（idx_eo_product_status_del_create_time）
- [ ] 慢 SQL 日志在 `logging.level.com.cartethyia.easyorange.config.database.SlowQueryMonitor=WARN` 可见
- [ ] 高并发时 actuator/health 返回 DEGRADED/DOWN
- [ ] 现有 2,546 后端测试 + Flyway 集成测试全 PASS

---

## 5. 模块 1.4 — AI 调用成本治理（第四个 PR）

### 5.1 目标

| 指标 | 优化前 | 优化后 |
|---|---|---|
| 同 prompt 重复调用 | N 次外部计费 | 1 次外部计费 + N-1 次 Redis 缓存 |
| Embedding 批量 | 单条调用 N 次 | 20 条/批，延迟 ↓ 50% |
| AI 失败 | 异常冒泡 | 3 次重试 + 价格中位数降级 |
| AI 滥用 | 无 | 每用户 10 次/小时 |

### 5.2 改动清单

#### 改动 1.4-A：Prompt Fingerprint 缓存（Redis）

**方案**：在 DeepSeek / Qwen-VL Adapter 入口/出口加缓存层。

**Fingerprint 算法**：
```java
public String fingerprint(String model, List<Message> messages, double temperature, String businessTag) {
    String joined = model + "|" + temperature + "|" + businessTag + "|" +
        messages.stream().map(m -> m.role() + ":" + m.content()).collect(Collectors.joining("||"));
    return DigestUtils.sha256Hex(joined);
}
```

**新增文件**：
- `easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/cache/LlmCacheService.java`
- `easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/adapter/cache/PromptFingerprint.java`

**缓存 key**：`eo:ai:cache:{fingerprint}`，TTL 7 天

**改造 Adapter**（以 DeepSeek 为例）：
```java
public String chat(String systemPrompt, String userMessage, String businessTag) {
    String fp = PromptFingerprint.of("deepseek-chat",
        List.of(new Message("system", systemPrompt), new Message("user", userMessage)),
        0.7, businessTag);
    return llmCacheService.getOrLoad(fp, () -> {
        // 原有 deepseekRestClient.post() 逻辑
    }, Duration.ofDays(7));
}
```

#### 改动 1.4-B：Embedding 批量

**新增文件**：`easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/service/BatchEmbeddingService.java`

**关键改动**：
```java
public List<float[]> embedBatch(List<String> texts) {
    if (texts.isEmpty()) return List.of();
    List<List<String>> chunks = Lists.partition(texts, 20);  // 20 条/批
    List<float[]> results = new ArrayList<>(texts.size());
    for (List<String> chunk : chunks) {
        DeepSeekEmbeddingResponse resp = deepSeekLlmAdapter.embed(chunk);  // 改造原 embed 支持 List<String>
        results.addAll(resp.toVectors());
    }
    return results;
}
```

**修改**：`DeepSeekLlmAdapter.embed(List<String> texts)` 重载支持批量

**调用方改造**：`SemanticSearchService` 把循环单条改为调用 `batchEmbeddingService.embedBatch()`

#### 改动 1.4-C：失败重试

**新增文件**：`easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/config/RetryConfig.java`

**配置**：
```java
@Configuration
@EnableRetry
public class RetryConfig {}

@Retryable(
    retryFor = {RestClientException.class, SocketTimeoutException.class, ConnectException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, multiplier = 2, maxDelay = 5000)
)
public String chat(...) { ... }

@Recover
public String chatRecover(Exception ex, ...) {
    log.warn("AI chat failed after retries, falling back", ex);
    return pricingFallbackService.calculateByMedian(...);
}
```

#### 改动 1.4-D：降级开关（价格中位数）

**新增文件**：`easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/service/PricingFallbackService.java`

**关键逻辑**：
```java
public BigDecimal calculateByMedian(Long categoryId, Integer conditionLevel) {
    // 1. 查询同分类近 30 天已售商品价格
    List<BigDecimal> prices = orderItemRepository.findSoldPricesByCategory(categoryId, 30);
    if (prices.isEmpty()) return BigDecimal.valueOf(50);  // 兜底默认值
    // 2. 按 conditionLevel 调整（±15%）
    BigDecimal median = MathUtils.median(prices);
    BigDecimal factor = BigDecimal.valueOf(0.85 + (conditionLevel - 5) * 0.06);
    return median.multiply(factor).setScale(2, RoundingMode.HALF_UP);
}
```

**依赖**：注入 `OrderItemQueryPort`（已有，order 模块）通过跨模块端口获取价格历史

#### 改动 1.4-E：限流

**修改文件**：`easyorange-application/src/main/resources/application.yaml`

**新增规则**：
```yaml
rate-limit-filter:
  rules:
    - path-pattern: /api/ai/**
      method: POST
      strategy: redis
      max-requests: 10
      window-seconds: 3600
      message: "AI 调用过于频繁，请稍后重试"
```

**注**：
- `RateLimitFilter` 默认按 `IP + path` 做 key。若已登录用户通过 `JwtAuthenticationFilter` 后能拿到 userId，复用 `RateLimitFilter` 已有 `userId` 维度（如不支持则扩展 `KeyResolver` 接口）。
- 限流是用户级 + IP 级双层：登录用户用 userId（从 SecurityContext 取，已登录走 userId 维度，未登录走 IP），保证同一用户多设备共享配额。
- Redis 不可用时按现有 fail-open 策略放行（已有约定）。

### 5.3 测试

- `LlmCacheServiceTest`：Mock Redis，验证同 fingerprint 第二次调用不打 DeepSeek
- `BatchEmbeddingServiceTest`：验证 20 条/批切分 + 结果合并顺序
- `DeepSeekLlmAdapterRetryTest`：注入 mock 抛 503，验证 3 次重试 + 降级
- `PricingFallbackServiceTest`：mock OrderItemQueryPort 返回价格列表，验证中位数计算
- 限流测试：复用现有 `RateLimitFilterTest`，加新规则用例

### 5.4 验收标准

- [ ] 同 prompt 100 次调用 → DeepSeek API 实际被调用 ≤ 2 次（1 次 miss + 1 次 TTL 过期）
- [ ] 50 条 embedding → 网络请求次数 ≤ 3（20+20+10）
- [ ] Mock DeepSeek 抛 503 → 验证 3 次重试 + 最终返回中位数价格
- [ ] 模拟用户 1 分钟发 11 次 `/api/ai/pricing` → 第 11 次返回 429
- [ ] 现有 AI 模块测试全 PASS

---

## 6. 实施时间线

| 周 | PR | 任务 |
|---|---|---|
| W1 | PR-1.3 | 前端 Bundle 优化 |
| W2 | PR-1.2 | 缓存层加固 |
| W3 | PR-1.1 | 数据库调优（Flyway 迁移需在 dev 环境先 dry-run） |
| W4 | PR-1.4 | AI 成本治理 |

每个 PR 单独走：`feature/perf-{block}` 分支 → 测试 → code review → 合并 → 生产灰度

---

## 7. 风险评估与回滚

| 风险 | 概率 | 影响 | 缓解措施 |
|---|---|---|---|
| recharts 懒加载导致统计页白屏 | 低 | 中 | Suspense fallback + Playwright E2E |
| Caffeine 同步锁导致请求排队 | 中 | 中 | `synchronized (key.intern())` 锁粒度到 key |
| Redis 锁 TTL 过期 → 重复回源 | 低 | 低 | 锁内 double-check + 5s TTL 充裕 |
| 归档任务锁表影响在线 | 中 | 高 | 分批 1 万行/批 + 业务低峰 03:00 执行 |
| AI 缓存污染（错误响应被缓存） | 中 | 中 | TTL 7 天上限 + 业务 tag 区分 |
| 降级价格偏离实际 | 低 | 低 | 价格中位数 + ±15% 区间 + 监控偏差 |

**回滚方案**：
- 前端：revert PR 即可
- 后端：保留 backward-compatible 旧 API，配置开关控制新逻辑
- 归档：Flyway `clean-disabled=true`，归档任务可 disable

---

## 8. 未来 Backlog（不在本期范围）

- `eo_order_item.product_snapshot` 改为只存 product_id + 价格快照
- 全文搜索迁移到 Elasticsearch（基础设施已就位 `easyorange.search.elasticsearch.enabled: false`）
- AI 调用 A/B 切流（DeepSeek vs Qwen 效果对比）
- 商品图片 CDN + WebP 自动转码

---

## 9. 评审清单

设计阶段需确认：
- [ ] 4 个 PR 拆分粒度是否合理
- [ ] 1.1 冷热分离归档策略是否符合合规要求
- [ ] 1.4 限流阈值 10次/小时是否过严
- [ ] AI 缓存 TTL 7 天是否合适

实施阶段需确认：
- [ ] 每个 PR 合并前测试覆盖率 ≥ 80%
- [ ] ArchUnit 守卫持续 PASS
- [ ] 生产环境灰度方案
