# EasyOrange Framework 配置指南

## 目录

- [安全配置](#安全配置)
- [JWT 配置](#jwt-配置)
- [线程池配置](#线程池配置)
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
  
  # XSS 防护开关
  xss-protection-enabled: false
```

### XSS 防护配置说明

**默认值**：`false`（关闭）

**适用场景**：

- **纯 JSON API**：建议关闭（默认）
  - 后端不负责 XSS 防护
  - 前端在渲染时进行转义
  - 保持数据原样存储
  
- **服务端渲染应用**：建议开启
  - 后端负责 XSS 防护
  - 自动转义用户输入
  - 防止 XSS 攻击

**示例配置**：

```yaml
security:
  # 纯 JSON API - 关闭 XSS 防护
  xss-protection-enabled: false
  
  # 服务端渲染应用 - 开启 XSS 防护
  # xss-protection-enabled: true
```

---

## JWT 配置

### 基本配置

```yaml
jwt:
  # JWT 签名密钥（必须配置）
  secret-key: ${JWT_SECRET_KEY}
  
  # JWT 签发者
  issuer: easyorange
  
  # Access Token 过期时间（分钟）
  access-token-expiration: 30
  
  # Refresh Token 过期时间（天）
  refresh-token-expiration: 7
  
  # Token 前缀
  token-prefix: "Bearer "
  
  # 自动续期阈值（分钟）
  auto-renew-threshold-minutes: 5
  
  # 本地缓存配置
  local-cache:
    # 缓存最大容量
    max-size: 10000
    # 缓存过期时间（分钟）
    expire-minutes: 5
```

### JWT 本地缓存配置说明

**作用**：减少 Redis 查询次数，提升认证性能

**工作原理**：

1. 首次验证 token 时，查询 Redis 并缓存结果
2. 后续验证时，先查本地缓存
3. 缓存未命中时，再查 Redis
4. 缓存自动过期（默认 5 分钟）

**性能提升**：

- 减少 Redis 查询次数约 80%
- 降低网络延迟
- 提升认证速度

**配置建议**：

```yaml
jwt:
  local-cache:
    # 高并发场景
    max-size: 20000
    expire-minutes: 10
    
    # 一般场景（默认）
    max-size: 10000
    expire-minutes: 5
    
    # 低并发场景
    max-size: 5000
    expire-minutes: 3
```

---

## 线程池配置

### 基本配置

```yaml
thread-pool:
  # 核心线程数
  core-pool-size: 8
  
  # 最大线程数
  max-pool-size: 16
  
  # 队列容量
  queue-capacity: 100
  
  # 线程存活时间（秒）
  keep-alive-seconds: 60
  
  # 线程名前缀
  thread-name-prefix: "async-"
  
  # 等待任务完成超时时间（秒）
  await-termination-seconds: 60
```

### 线程池拒绝策略

**改进内容**：

- 使用自定义 `LoggingRejectedExecutionHandler`
- 记录详细的拒绝信息
- 支持两种模式：
  - **丢弃模式**：丢弃任务并记录日志
  - **调用线程执行模式**：由调用线程执行任务

**日志示例**：

```
WARN  - 线程池 [async-] 任务被拒绝 - 活跃线程: 16, 队列大小: 100, 已完成: 1234, 是否丢弃: false
WARN  - 线程池 [async-] 由调用线程执行任务
```

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
      
redis:
  # Key 前缀
  key-prefix: "easyorange"
```

### 缓存类型转换异常

**改进内容**：

- 使用自定义 `CacheTypeMismatchException`
- 提供友好的错误信息
- 包含 key、期望类型、实际类型

**异常示例**：

```
CacheTypeMismatchException: 缓存类型不匹配 - Key: user:123, 期望类型: java.lang.String, 实际类型: java.lang.Integer
```

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
  xss-protection-enabled: false

# JWT 配置
jwt:
  secret-key: ${JWT_SECRET_KEY}
  issuer: easyorange
  access-token-expiration: 30
  refresh-token-expiration: 7
  token-prefix: "Bearer "
  auto-renew-threshold-minutes: 5
  local-cache:
    max-size: 10000
    expire-minutes: 5

# 线程池配置
thread-pool:
  core-pool-size: 8
  max-pool-size: 16
  queue-capacity: 100
  keep-alive-seconds: 60
  thread-name-prefix: "async-"
  await-termination-seconds: 60

# Redis 配置
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:easyorange123}
      database: 0

redis:
  key-prefix: "easyorange"
```

---

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `JWT_SECRET_KEY` | JWT 签名密钥 | - |
| `REDIS_HOST` | Redis 主机 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | easyorange123 |

---

## 最佳实践

1. **生产环境配置**
   - 禁止使用 `*` 作为 CORS 允许的源
   - 使用环境变量管理敏感配置
   - 定期轮换 JWT 密钥

2. **性能优化**
   - 根据并发量调整 JWT 本地缓存大小
   - 根据服务器性能调整线程池大小
   - 监控线程池拒绝情况

3. **安全建议**
   - 密码加密强度建议 12-14
   - 纯 JSON API 关闭 XSS 防护
   - 定期更新依赖版本

---

## 更新日志

### v0.0.1-SNAPSHOT (2026-05-01)

**新增功能**：

- JWT 本地缓存优化（Caffeine）
- 自定义线程池拒绝策略
- 自定义缓存类型转换异常
- XSS 防护可配置开关

**改进**：

- 统一领域事件发布逻辑
- 替换 JwtAuthenticationFilter 为 Spring Security OAuth2 Resource Server
- 改进 Redis 缓存类型转换异常处理

**依赖更新**：

- 添加 Caffeine 缓存依赖
