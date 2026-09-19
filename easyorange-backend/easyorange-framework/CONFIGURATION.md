# EasyOrange Framework 配置指南

> 只放 `easyorange.*` / `spring.*` 配置项与使用示例。各机制的实现要点、改动注意事项见 [easyorange-backend/AGENTS.md](../AGENTS.md)。

## 安全配置

```yaml
security:
  # 忽略认证的路径列表
  ignore-paths:
    - /api/auth/login
    - /api/auth/register
    - /api/public/**

  # 产品路径列表（公开访问）
  # ⚠️ 会跳过 JWT 认证，且支持前缀匹配：/api/products 会匹配 /api/products/my
  # 新增需要认证的商品接口，必须在 SecurityConfig 补
  # .requestMatchers(GET, "/api/products/my/**").authenticated()
  product-paths:
    - /api/products/**
    - /api/categories/**

  # 静态资源路径列表
  static-paths:
    - /static/**
    - /public/**

  # CORS 允许的源列表（生产环境禁止 "*"）
  allowed-origins:
    - https://app.example.com
    - https://admin.example.com

  logout-url: /api/auth/logout
  password-encoder-strength: 12   # BCrypt 强度，建议 12-14，范围 4-31
```

**CSP 安全策略**：JSON API 后端在 `SecurityConfig` 硬编码 `Content-Security-Policy: default-src 'none'; base-uri 'none'; form-action 'none'` 实现浏览器端 XSS 防御（无配置项）；`X-XSS-Protection` 已禁用（主流浏览器已废弃）。前端 SPA 页面自行配置 CSP。

> 输入层 `XssFilter` + `XssHttpServletRequestWrapper` 已移除：对 JSON body 无效，且容易破坏正常业务数据。

## JWT 配置

```yaml
jwt:
  private-key-location: ${JWT_RSA_PRIVATE_KEY}   # PEM 路径；dev 留空则启动时自动生成密钥对
  public-key-location: ${JWT_RSA_PUBLIC_KEY}
  issuer: easyorange
  access-token-expiration: 30     # 分钟
  refresh-token-expiration: 7     # 天
```

生产密钥经环境变量注入且 fail-fast（`ProdSecrets`，缺失或为空即中止启动）；轮换执行 `keys/generate-rsa-keypair.sh`。

## 线程配置（虚拟线程优先）

`spring.threads.virtual.enabled=true`，IO 密集型异步任务全部走虚拟线程——**不要配置 `spring.task.execution.pool.*`**（Spring Boot 会忽略，配置已在 application.yaml 清理）。

| 旧组件 | 原参数 | 替换 |
|--------|--------|------|
| `domainEventExecutor` | core=5, max=10, queue=1000 | 虚拟线程——`@Async` 用 Spring Boot 自动配置的 `SimpleAsyncTaskExecutor` |
| `aiSearchExecutor` | core=4, max=8, queue=100 | 虚拟线程——`supplyAsync` 显式传 `SearchTool.VIRTUAL`（无参形式走 commonPool 平台线程） |
| `webSocketInbound/OutboundExecutor` | core=4, max=10, queue=100 | 虚拟线程——删除自定义 channel 配置，用 Spring 默认线程 |
| `thread-pool.*` | 10 个 YAML 属性 | 已删除（死代码） |

**唯一保留的平台线程池 `taskScheduler`**（`@Scheduled` 用）：poolSize=5 固定，前缀 `scheduled-`，`MdcTaskDecorator` 传播 MDC，拒绝策略 `LoggingRejectedExecutionHandler`（调用线程执行 + WARN），优雅关闭 `waitForTasksToCompleteOnShutdown=true` / `awaitTerminationSeconds=60`。

**虚拟线程注意**：MDC 由 Micrometer Tracing + Brave 自动继承，无需 `MdcTaskDecorator`；`CompletableFuture.cancel(true)` 会 `interrupt()` 并可能泄漏 carrier，统一用 `cancel(false)`；排查 pinning 加 `-Djdk.tracePinnedThreads=short`。

## 缓存配置

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:easyorange123}
      database: 0
      timeout: 5000ms

easyorange:
  cache:
    default-ttl: 30m        # Spring Cache 统一 TTL；一致性靠写路径显式 evict，TTL 只兜底
    ttl-jitter: 0.1         # 写入时按比例加随机抖动，防雪崩
    image:
      max-size: 1000        # Caffeine 图片处理缓存，独立于 Spring Cache
      expire-hours: 24
```

缓存统一走 `RedisCacheConfig`：`@EnableCaching` + `RedisCacheManager`（String key + `GenericJacksonJsonRedisSerializer` value，key 形如 `eo:product:info::<id>`）；手写多级缓存（`MultiLevelCache` + Pub/Sub 广播）与 `CacheUtils` / `LocalCacheConfig` 已移除。

```java
// 读：未命中自动回源。刻意不写 unless —— null 结果一并缓存，用于防穿透
@Cacheable(cacheNames = "productInfoCache", key = "#productId",
           condition = "#productId != null", sync = true)
public ProductVO getProductCache(String productId, Supplier<ProductVO> loader) { ... }

// 失效：写路径显式触发（商品领域事件 / MQ 事件消费）
@CacheEvict(cacheNames = "productInfoCache", key = "#productId", condition = "#productId != null")
public void evictProductCache(String productId) { }
```

**不要给 `@Cacheable` 加 `unless = "#result == null"`**——那会把这套防穿透设计关掉。列表类缓存不缓存 null 的做法是 `orEmpty` 兜成可变空列表（见 `CategoryCacheAdapter`）。

**序列化注意**：`java.*` 包 final 类型（`Optional`、`List.of()` 的不可变列表）不带类型信息、无法反序列化——缓存值必须用 POJO/record（`com.cartethyia.*`）或可变 `ArrayList`。

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `JWT_RSA_PRIVATE_KEY` | RSA 私钥 PEM 文件路径 | 空（dev 自动生成） |
| `JWT_RSA_PUBLIC_KEY` | RSA 公钥 PEM 文件路径 | 空（dev 自动生成） |
| `REDIS_HOST` | Redis 主机 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | easyorange123 |

全部环境变量的单一来源是仓库根 `.env.example`；其余键见 [easyorange-backend/AGENTS.md](../AGENTS.md)「构建 / 启动 / 环境」。
