# EasyOrange Framework

框架核心模块 - 提供基础设施和通用功能

## 模块说明

`easyorange-framework` 是 EasyOrange 项目的核心框架模块，提供了以下功能：

### 核心功能

- **安全认证**：JWT 认证、密码加密、CORS 配置
- **缓存**：Spring Cache 注解式 + Redis 单层（统一短 TTL，一致性靠写路径显式 evict；图片处理缓存保留独立 Caffeine）
- **缓存故障降级**：`CacheErrorHandler` fail-open —— Redis 不可用时读直查 DB、写放弃本次缓存，注解侧零改动
- **分布式 ID**：UUID v7 (RFC 9562) 生成器，零协调零依赖，时间有序
- **领域事件**：事件发布与订阅机制（Outbox 模式，与应用事务同原子）
- **异常处理**：全局异常处理、友好错误信息
- **Web 增强**：限流与防重（`RateLimitFilter` 配置驱动）、Idempotency-Key 幂等、审计日志（Outbox 异步入库）
- **线程池管理**：异步任务执行、线程池监控
- **文件服务**：文件上传、存储管理

## 技术栈

- Spring Boot 4.0.7
- Spring Security
- Spring Data Redis
- JWT (Spring Security OAuth2 Resource Server + Nimbus JOSE)
- MyBatis-Plus 3.5.16
- Caffeine Cache 3.x
- AspectJ

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.cartethyia</groupId>
    <artifactId>easyorange-framework</artifactId>
</dependency>
```

### 2. 配置文件

参考 [CONFIGURATION.md](./CONFIGURATION.md) 进行配置。

### 3. 启用功能

```java
@SpringBootApplication
@EnableAsync
@EnableCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 主要组件

### 安全组件

| 组件 | 说明 |
|------|------|
| `SecurityConfig` | Spring Security 配置（含 JwtDecoder + JwtEncoder + JwtAuthenticationConverter） |
| `TokenService` | Token 管理服务（签发/刷新/吊销，使用 JwtEncoder + JwtDecoder） |

### 缓存组件

| 组件 | 说明 |
|------|------|
| `RedisCacheConfig` | Spring Cache 配置（`@EnableCaching` + `RedisCacheManager`：String key + JSON value，统一 TTL 由 `easyorange.cache.default-ttl` 控制，`CacheErrorHandler` fail-open）。手写多级缓存已移除（2026-08-13） |
| `imageProcessCache` | Caffeine 图片处理缓存（`easyorange.cache.image.*`，expireAfterAccess 24h） |
| `RedisTemplate<Object, Object>` | Spring Data Redis 标准模板（`RedisConfig` 显式配置 StringRedisSerializer + GenericJacksonJsonRedisSerializer） |
| `RedissonClient` | Redisson 分布式锁（RLock，替代旧版 Lua 自旋锁） |

### 分布式基础设施

| 组件 | 说明 |
|------|------|
| `UuidV7IdGenerator` | UUID v7 (RFC 9562)，`@Primary` 主实现，零配置 |


### 事件组件

| 组件 | 说明 |
|------|------|
| `DomainEventPublisher` | 领域事件发布接口 |
| `ModulithDomainEventPublisher` | `@Primary` 发布实现：Spring Modulith Outbox（`EVENT_PUBLICATION` 表与应用事务同原子）→ 异步外发 RabbitMQ Topic Exchange |

### Web 过滤器组件

| 组件 | 说明 |
|------|------|
| `RateLimitFilter` | 限流 + 防连点统一过滤器（配置驱动：GET 走本地 200/60s/IP，写走 Redisson 分布式令牌桶，fail-open） |
| `IdempotencyKeyFilter` | Idempotency-Key 幂等（配置驱动，24h 响应缓存，非 2xx 不缓存） |
| `AuditLogAspect` | 审计日志切面（@Around + Builder + @Async，Outbox 模式异步入库） |

## 使用示例

### JWT 认证

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final TokenService tokenService;
    
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        // 验证用户
        User user = userService.authenticate(request);
        
        // access 为 RSA JWT；refresh 为 opaque，经 HttpOnly Cookie 下发（不进 JSON body）
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        refreshCookie.write(response, refreshToken);
        return Result.ok(new LoginResult(accessToken, userVO));
    }
}
```

### 缓存使用

```java
// 读：缓存未命中自动执行方法体回源（null 返回值不落缓存），TTL 由 easyorange.cache.default-ttl 统一控制
@Cacheable(cacheNames = "productInfoCache", key = "#productId", condition = "#productId != null", unless = "#result == null")
public ProductVO getProductCache(String productId, Supplier<ProductVO> loader) {
    return productId == null ? null : loader.get();
}

// 失效：写路径显式触发（商品领域事件 / MQ 事件消费），Redis 故障由 CacheErrorHandler fail-open 降级
@CacheEvict(cacheNames = "productInfoCache", key = "#productId", condition = "#productId != null")
public void evictProductCache(String productId) {
}
```

> 序列化注意：`java.*` 包 final 类型（`Optional`、`List.of()` 不可变列表）不带类型信息、无法反序列化 —— 缓存值必须用 POJO/record（`com.cartethyia.*`）或可变 `ArrayList`。

### 领域事件

```java
// 定义事件
public record UserCreatedEvent(String userId, String username) implements DomainEvent {
    private final Long userId;
    
    public UserCreatedEvent(Long userId) {
        super("user.created");
        this.userId = userId;
    }
}

// 发布事件
@Service
public class UserService {
    
    private final DomainEventPublisher eventPublisher;
    
    public void createUser(CreateUserRequest request) {
        User user = // ... 创建用户
        eventPublisher.publish(new UserCreatedEvent(user.getId()));
    }
}

// 监听事件
@Component
public class UserEventListener {
    
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        // 处理事件
    }
}
```

### 限流与防重（过滤器，配置驱动）

```yaml
easyorange:
  ratelimit:
    rules:            # 规则列表（按路径前缀匹配，GET 本地限流 / 写走 Redisson 分布式令牌桶）
      - pattern: /api/**
        ...
    repeat:           # 防连点：3s 间隔 + 请求体 hash
      interval-seconds: 3
```

> 原 `@RateLimiter` / `@RepeatSubmit` AOP 注解已移除，统一收敛到 `RateLimitFilter`（见 `RateLimitFilterProperties`）。幂等请用 `Idempotency-Key` 头（`IdempotencyKeyFilter`）。

## 配置说明

详细配置请参考 [CONFIGURATION.md](./CONFIGURATION.md)。

## 最佳实践

1. **安全配置**
   - 生产环境禁止使用 `*` 作为 CORS 允许的源
   - 使用环境变量管理敏感配置
   - 定期轮换 JWT 密钥

2. **性能优化**
   - 合理配置 JWT 本地缓存
   - 根据业务需求调整线程池大小
   - 监控缓存命中率

3. **异常处理**
   - 使用全局异常处理器
   - 提供友好的错误信息
   - 记录详细的错误日志

## 更新日志

### v0.0.1-SNAPSHOT (2026-05-01)

**新增功能**：

- JWT 本地缓存优化（Caffeine）
- 自定义线程池拒绝策略
- 自定义缓存类型转换异常
- 移除 `XssFilter`，改用 `Content-Security-Policy` 响应头

**改进**：

- 统一领域事件发布逻辑
- 替换 JwtAuthenticationFilter 为 Spring Security OAuth2 Resource Server
- 改进 Redis 缓存类型转换异常处理

## 相关链接

- [配置指南](./CONFIGURATION.md)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Security 官方文档](https://spring.io/projects/spring-security)
