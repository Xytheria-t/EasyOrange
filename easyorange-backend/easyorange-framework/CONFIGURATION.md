# EasyOrange Framework 配置指南

## 目录

- [安全配置](#安全配置)
- [JWT 配置](#jwt-配置)
- [线程池配置（虚拟线程优先）](#线程池配置虚拟线程优先)
- [缓存配置](#缓存配置)

---

## 安全配置

### 基本配置

```yaml
security:
  # 忽略认证的路径列表
  ignore-paths:
    - /api/auth/login
    - /api/auth/register
    - /api/public/**
  
  # 产品路径列表（公开访问）
  # ⚠️ 警告：配置项会跳过 JWT 认证，且支持前缀匹配
  # 例如 /api/products 会匹配 /api/products/my，导致需要认证的接口被公开访问
  # 如果新增需要认证的商品接口，需在 SecurityConfig 中添加 .requestMatchers(GET, "/api/products/my/**").authenticated()
  product-paths:
    - /api/products/**
    - /api/categories/**
  
  # 静态资源路径列表
  static-paths:
    - /static/**
    - /public/**
  
  # CORS 允许的源列表
  allowed-origins:
    - https://app.example.com
    - https://admin.example.com
    # ⚠️ 生产环境禁止使用 "*"
  
  # 登出 URL 路径
  logout-url: /api/auth/logout
  
  # BCrypt 密码加密强度（4-31）
  password-encoder-strength: 12
  
```

### CSP 安全策略说明

JSON API 后端通过 `Content-Security-Policy` 响应头实现浏览器端 XSS 防御：

- 策略：`default-src 'none'; base-uri 'none'; form-action 'none'`
- 在 `SecurityConfig` 中硬编码（无需配置项）
- `X-XSS-Protection` 头已禁用（主流浏览器已废弃该特性）
- 前端 SPA 页面应自行配置适当的 CSP 策略

> 去掉了 `XssFilter` + `XssHttpServletRequestWrapper` 输入层转义，因为它对 JSON body 无效且容易破坏正常业务数据。

---

## JWT 配置

### 基本配置

```yaml
jwt:
  # RSA 私钥 PEM 文件路径（生产环境必须配置）
  private-key-location: ${JWT_RSA_PRIVATE_KEY}
  
  # RSA 公钥 PEM 文件路径（生产环境必须配置）
  public-key-location: ${JWT_RSA_PUBLIC_KEY}
  
  # JWT 签发者
  issuer: easyorange
  
  # Access Token 过期时间（分钟）
  access-token-expiration: 30
  
  # Refresh Token 过期时间（天）
  refresh-token-expiration: 7

---

## 线程池配置（虚拟线程优先）

> **架构变更（2026-07-26）**：项目已全面启用 Java 21+ 虚拟线程（`spring.threads.virtual.enabled=true`）。自定义 `ThreadPoolTaskExecutor` 已移除，IO 密集型异步任务由虚拟线程自动管理。仅保留 `taskScheduler`（`ThreadPoolTaskScheduler`）用于 `@Scheduled` 定时任务，固定 poolSize=5。

### 为什么移除自定义线程池

| 旧组件 | 原参数 | 替换方案 |
|--------|--------|---------|
| `domainEventExecutor` | core=5, max=10, queue=1000 | 虚拟线程 — `@Async` 使用 Spring Boot 自动配置的 `SimpleAsyncTaskExecutor` |
| `aiSearchExecutor` | core=4, max=8, queue=100 | 虚拟线程 — `CompletableFuture.supplyAsync()` 无参形式用 `ForkJoinPool.commonPool()` |
| `webSocketInbound/OutboundExecutor` | core=4, max=10, queue=100 | 虚拟线程 — 删除自定义 channel 配置，Spring 默认线程 |
| `thread-pool.*` 配置项 | 10 个 YAML 配置属性 | 已删除（死代码） |

### 保留的线程池：taskScheduler

`@Scheduled` 定时任务（消息归档、缓存清理等）保留平台线程池：

- PoolSize: **5**（固定）
- 前缀：`scheduled-`
- MDC 传播：`MdcTaskDecorator`
- 拒绝策略：`LoggingRejectedExecutionHandler`（调用线程执行 + WARN 日志）
- 优雅关闭：`waitForTasksToCompleteOnShutdown=true`, `awaitTerminationSeconds=60`

### 虚拟线程注意事项

- 虚拟线程由 JVM 管理，无需配置 core/max/queue —— **Spring Boot 忽略 `spring.task.execution.pool.*` 配置**（这些配置已在 application.yaml 中清理）
- MDC 传播：虚拟线程自动继承父线程的 MDC 上下文（Micrometer Tracing + Brave），无需 `MdcTaskDecorator`
- 调试：启动时加 `-Djdk.tracePinnedThreads=short` 检测虚拟线程钉在 carrier 线程上的情况
- `CompletableFuture.cancel(true)` 发送 `Thread.interrupt()` 可能导致 carrier 线程泄漏，统一使用 `cancel(false)`

---

## 缓存配置

### Redis 配置

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
    # Redis Key 前缀
    key-prefix: "easyorange"
```

### 缓存类型转换

`RedisCache` 薄封装层已移除（2026-07-17），类型安全转换通过 `CacheUtils.cast()` 实现：

```java
// 类型安全转换 (支持 Number 跨类型)
String val = CacheUtils.cast(redisTemplate.opsForValue().get(key), String.class);
```

`CacheUtils.cast()` 在类型不匹配时抛出 `ClassCastException`（同标准 Java 语义），不再使用自定义异常。

---

## 领域事件配置

### 事件发布（RabbitMQ）

**使用示例**：

```java
domainEventPublisher.publish(new UserCreatedEvent(userId));
```

事件通过 `RabbitMQDomainEventPublisher` 发布到 `eo.domain.events` Topic Exchange，各模块 `@RabbitListener` 消费者异步处理。

**注意事项**：

- 确保事件 record 实现 `DomainEvent`（仅含 `eventType()` 默认方法，由类名自动派生）
- 路由键由事件类名自动派生（`ProductCreatedEvent` → `product.created`），无需手动注册
- 每个消费者独占队列（`eo.{name}`），失败消息路由到 DLQ（`eo.{name}.dlq`）+ 指数退避重试
- 多方法消费者使用类级 `@RabbitListener` + 方法级 `@RabbitHandler`（类型分发）

---

## 完整配置示例

```yaml
# 安全配置
security:
  ignore-paths:
    - /api/auth/login
    - /api/auth/register
    - /api/public/**
  product-paths:
    - /api/products/**
  static-paths:
    - /static/**
  allowed-origins:
    - https://app.example.com
  logout-url: /api/auth/logout
  password-encoder-strength: 12
  # CSP 由 SecurityConfig 硬编码配置，无需额外配置项

# JWT 配置
jwt:
  private-key-location: ${JWT_RSA_PRIVATE_KEY}
  public-key-location: ${JWT_RSA_PUBLIC_KEY}
  issuer: easyorange
  access-token-expiration: 30
  refresh-token-expiration: 7

# 虚拟线程配置
spring:
  threads:
    virtual:
      enabled: true
# （所有 IO 密集型异步任务使用虚拟线程，无需配置线程池参数）
# 仅 taskScheduler 保留平台线程（poolSize=5，硬编码在 ThreadPoolConfig 中）
# 旧 thread-pool.* 配置项已移除

# Redis 配置
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:easyorange123}
      database: 0

easyorange:
  cache:
    key-prefix: "easyorange"
```

---

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `JWT_RSA_PRIVATE_KEY` | RSA 私钥 PEM 文件路径 | - |
| `JWT_RSA_PUBLIC_KEY` | RSA 公钥 PEM 文件路径 | - |
| `REDIS_HOST` | Redis 主机 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | easyorange123 |

---

## 最佳实践

1. **生产环境配置**
   - 禁止使用 `*` 作为 CORS 允许的源
   - 使用环境变量管理敏感配置
   - 定期轮换 JWT 密钥对（运行 `keys/generate-rsa-keypair.sh` 重新生成）

2. **性能优化（虚拟线程）**
   - IO 密集型任务使用虚拟线程，无需调整线程池参数
   - 启动时加 `-Djdk.tracePinnedThreads=short` 检测虚拟线程钉住 carrier 线程
   - 避免在虚拟线程中使用 `synchronized` 块或 `Thread.interrupt()`（改用 `cancel(false)`）

3. **安全建议**
   - 密码加密强度建议 12-14
   - REST API 禁用 `X-XSS-Protection`（已废弃），使用 CSP 头
   - 定期更新依赖版本

---

## 更新日志

### v0.0.1-SNAPSHOT (2026-05-01)

**新增功能**：

- JWT 本地缓存优化（Caffeine）
- 虚拟线程迁移（移除自定义线程池，仅保留 taskScheduler）
- 自定义缓存类型转换异常
- 移除 `XssFilter`，改用 `Content-Security-Policy` 响应头

**改进**：

- 统一领域事件发布逻辑
- 替换 JwtAuthenticationFilter 为 Spring Security OAuth2 Resource Server
- 改进 Redis 缓存类型转换异常处理

**依赖更新**：

- 添加 Caffeine 缓存依赖
