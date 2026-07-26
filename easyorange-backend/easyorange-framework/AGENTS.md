# easyorange-framework 模块指南

框架基础设施层，为所有业务模块提供技术支撑：Security、Redis、事件发布、AOP、文件、日志。

## 目录结构

```
framework/
├── async/             # AsyncManager（异步任务管理）
├── bloom/             # BloomFilter + RedisBitmapBloomFilter（布隆过滤器）
├── cache/             # CacheLoader + CacheUtils + MultiLevelCache（多级缓存门面）
├── config/            # 框架配置（线程池/Jackson/MDC/缓存/Redis/Security/WebMVC/Properties）
├── event/             # 领域事件基础设施（AbstractDomainEventConsumer / EventMetadata / EventMetricsService / EventIdempotencyChecker / DlqAnomalyListener）
├── exception/         # GlobalExceptionHandler（全局异常处理，RFC 9457 ProblemDetail）
├── file/              # 文件上传下载（FileController/FileService/FileStorage）
├── idgen/             # UuidV7IdGenerator（UUID v7）
├── mybatis/           # CodeEnumTypeHandler（通用枚举 TypeHandler 基类）
├── messaging/         # RabbitMQ + Spring Modulith（EventExternalizationConfig / RabbitMQConfig / ModulithDomainEventPublisher）
├── metrics/           # BusinessMetricsService + MetricsConfig
├── audit/             # AuditLogAspect + AuditLogService（审计日志 AOP）
├── auth/              # TokenService + TokenServiceImpl（JWT 签发/刷新/吊销）
├── util/              # 工具函数（FileUtils/LocalRateLimiter/SecurityContextUtil/TestSecurityUtil）
└── web/               # 过滤器（RateLimitFilter）+ 处理器（CustomMetaObjectHandler）+ 幂等（@Idempotent）
```

## 核心机制

### JWT 认证流程（Spring Security OAuth2 Resource Server）

JWT 认证由 Spring Security OAuth2 Resource Server 内置的 `BearerTokenAuthenticationFilter` 处理，无需自定义 Servlet Filter：

1. `BearerTokenAuthenticationFilter` (Spring Security 内置) 从 `Authorization: Bearer xxx` 提取 Token
2. `JwtDecoder` (SecurityConfig bean) 验证签名 (Nimbus) + issuer 检查；Token 吊销（黑名单 + 强制登出）由 `TokenRevocationFilter` 在认证完成后执行
3. `JwtAuthenticationConverter` (SecurityConfig) 检查 token type (拒绝 refresh token)，从 `"authorities"` claim 读取权限列表，构造 `AuthUser` 并设置 `SecurityContext`
4. `TokenService.createAccessToken()` / `createRefreshToken()` 使用 `JwtEncoder` (NimbusJwtEncoder) 答发
5. 登出时 Token 的 jti 加入 Redis 黑名单（TTL = 剩余有效期，自动过期）
6. `WebSocketAuthInterceptor` 复用 `JwtDecoder` bean 做连接握手认证

> **管理员判定**：后端通过 `UserType.getDefaultRoles()`（领域枚举，在 `AuthAppService.login()` 中调用）决议 `UserType → authorities`，将 `["ROLE_ADMIN", "ROLE_USER"]` 或 `["ROLE_USER"]` 写入 JWT 的 `"authorities"` claim。资源服务器直接读取该 claim，无需重新判定。

### 领域事件发布流程

业务模块注入 `DomainEventPublisher` 调用 `publish()`，实际由 `ModulithDomainEventPublisher`（`@Primary`）代理到 `ApplicationEventPublisher`。Spring Modulith 在数据库 `EVENT_PUBLICATION` 表中持久化事件（与应用事务同原子），事务提交后异步读取并发布到 `eo.domain.events` Topic Exchange。各模块通过 `@RabbitListener` 注解的消费者异步处理事件。`@ConditionalOnProperty(matchIfMissing=true)` 支持无 RabbitMQ 环境启动。

### 事件消费者基础设施

所有消费者继承 `AbstractDomainEventConsumer` 模板基类，统一以下横切关注点：

1. **幂等去重**：`EventIdempotencyChecker`（Redis SETNX + Redisson 锁），命名空间 `consumerId() + ":" + eventType()` 隔离多消费者，`idempotencyEnabled=false` 构造器关闭投影/广播/指标类消费者
2. **事件元数据**：`EventMetadataMessagePostProcessor` 发布前向 message headers 注入 eventId/timestamp/traceId；`EventMetadata.from(message, event)` 在消费端解码
3. **指标埋点**：`EventMetricsService` 自动上报 `easyorange.events.received{type,outcome}` / `easyorange.events.duration{type,outcome}` / `easyorange.events.dlq{queue,reason}`
4. **DLQ 异常监听**：`DlqAnomalyListener` 监听 11 个 DLQ 队列，提取 x-death header 记录指标
5. **模板方法**：`handle(event, message)` 做统一预处理（幂等 → metrics 采样 → 日志 → 业务处理 → 异常兜底），`doHandle(event, metadata)` 由子类实现业务逻辑

### Redis 缓存操作

`RedisCache` 薄封装层已移除（2026-07-17），所有缓存操作改为直接注入 `RedisTemplate<Object, Object>`。Spring Data Redis 的 `RedisTemplate` 是标准 API，无需额外学习：

```java
// KV 操作
redisTemplate.opsForValue().set(key, value);
redisTemplate.opsForValue().set(key, value, timeout, unit);
Object obj = redisTemplate.opsForValue().get(key);
// 类型安全转换使用 CacheUtils.cast()
String val = CacheUtils.cast(redisTemplate.opsForValue().get(key), String.class);
redisTemplate.delete(key);
redisTemplate.delete(List.of(key1, key2));

// 键生命周期
redisTemplate.hasKey(key);
redisTemplate.expire(key, timeout, unit);
redisTemplate.getExpire(key, unit);

// 原子操作
redisTemplate.opsForValue().increment(key);       // +1
redisTemplate.opsForValue().increment(key, delta); // +delta

// Hash 操作
redisTemplate.opsForHash().putAll(key, map);

// SCAN 扫描
CacheUtils.scan(redisTemplate, pattern, count);  // 基于 SCAN 的批量扫描
```

> **技巧**: `CacheUtils.cast(obj, clazz)` 支持 Number 跨类型转换（Long ↔ Integer）。`CacheUtils.scan()` 使用 `SCAN` 命令（cursor 迭代），避免 `KEYS *` 阻塞 Redis。需要 Lua 脚本时直接调用 `redisTemplate.execute(redisScript, keys, args)`。

### 分布式锁

分布式锁已迁移到 **Redisson RLock**（2026-07-17），替代旧版 RedisTemplate Lua 方案：

```java
// 通过 RedissonClient 注入
@Autowired
private RedissonClient redissonClient;

RLock lock = redissonClient.getLock("eo:lock:" + key);
if (lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {
    try {
        // 业务逻辑
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

Redisson 自动处理锁续期（Watch Dog）、重入、死锁检测。配置见 `RedissonConfig.java`。

### 日志系统与 MDC 传播

**traceId 自动注入**：项目引入 `micrometer-tracing-bridge-brave`，Spring Boot 4 自动配置 `Slf4jScopeDecorator` 注入 `traceId`/`spanId` 到 MDC。HTTP 请求进入时 Brave 的 `TracingFilter` 自动开启 span，无需手写 UUID。

**异步线程 MDC 传播**：`MdcTaskDecorator` 实现 Spring 的 `TaskDecorator`（类级标注 `@NullMarked` 匹配父接口契约），在 `ThreadPoolConfig` 中注入 `taskScheduler`（唯一的线程池）：

```java
@NullMarked
public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();  // 主线程快照
        return () -> {
            if (context != null) MDC.setContextMap(context);  // 复制到子线程
            try { runnable.run(); }
            finally { MDC.clear(); }  // 防止线程池复用时上下文泄漏
        };
    }
}
```

覆盖范围：`@Scheduled` 定时任务。`@Async` 在虚拟线程模式下由 Micrometer Tracing 自动继承 MDC，无需 `MdcTaskDecorator`。

**AsyncAppender 异步日志写入**：`logback-spring.xml` 配置 `ASYNC_FILE` / `ASYNC_ERROR_FILE` / `ASYNC_JSON_FILE` 包装底层同步 Appender：

- `queueSize=1024` / `discardingThreshold=0`（不丢弃任何级别）
- `neverBlock=true`（队列满不阻塞业务线程）
- 生产环境 JSON 结构化日志使用 Spring Boot 4 内置 `StructuredLogEncoder` + logstash 格式

**业务字段注入**：`LoggingInterceptor` 在 HTTP 请求进入时注入 `clientIp` / `method` / `uri` / `fullUrl` 到 MDC（traceId 由 Micrometer 自动注入）。

### 布隆过滤器 (bloom/)

Redis 位图实现的布隆过滤器，用于缓存穿透防护：

- 默认配置: 100 万预期插入, 1% 假阳性率 → 约 1.2MB 内存, 7 个哈希函数
- 哈希策略: Murmur3 128-bit 拆两个 long → k 个偏移量 (Less Hashing, Same Performance)
- 位操作: `opsForValue().setBit/getBit`（标准 API，无 Lua、无序列化依赖；位图操作本身幂等，无需单次调用原子性）
- 支持自定义预期数据量和假阳性率

```java
// 使用默认配置
bloomFilter.put("eo:bloom:product:id", productId.toString());
boolean exists = bloomFilter.mightContain("eo:bloom:product:id", productId.toString());

// 自定义参数 (100 万, 1%)
var custom = new RedisBitmapBloomFilter(redisTemplate, 1_000_000L, 0.01);
```

### 多级缓存门面 (cache/)

```java
// L1 Caffeine → L2 Redis → DB 三级串联
ProductDetail detail = multiLevelCache.get(
    "product:detail:" + id,
    ProductDetail.class,
    () -> repository.findDetail(id)
);

// 手动失效 (同时清除 L1 + L2)
multiLevelCache.evict("product:detail:" + id);
```

### Resilience4j Retry (resilience4j/)

`Resilience4jConfig` 提供 `RetryRegistry` Bean（自动绑定 Micrometer 指标）。默认配置：指数退避 500ms 初始间隔 × 2.0 乘数，最多 3 次，重试 `RestClientException` / `ResourceAccessException`，忽略 `IllegalArgumentException`。

`Resilience4jConfig` 预注册两个具名 Retry 实例供 AI 模块注入：

| 名称 | 用途 | 注入方式 |
|------|------|---------|
| `aiLlm` | LLM 文本调用重试（CachingLlmAdapter） | `@Qualifier("aiLlm") Retry` |
| `aiVision` | Vision 图片分析重试（CachingVisionAdapter） | `@Qualifier("aiVision") Retry` |

**使用模式**：AI 适配器构造器注入具名 `Retry`，用 `Retry.decorateSupplier()` 包装实际 LLM/Vision 调用，实现网络瞬断时自动重试。参考 `CachingLlmAdapter` / `CachingVisionAdapter`。

### 分布式 ID 生成器 (idgen/)

`IdGenerator` 接口定义在 `common/idgen/`，`UuidV7IdGenerator`（`@Primary`）作为纯 Java 实现，生成 RFC 9562 UUID v7（毫秒级有序 + 随机后缀）。

已移除 Snowflake 备选（`SnowflakeIdGenerator` / `WorkerIdProvider` / `RedisWorkerIdProvider`），UUID v7 零配置零依赖，无需任何配置属性即可使用。

### 统一响应包装

`ResponseAdvice` 自动将 Controller 返回值包装为 `Result<T>`，无需手动包装。

### Idempotency-Key 幂等 (web/idempotency/)

客户端在请求头中传入 `Idempotency-Key`（UUID v4），服务端缓存成功响应结果。相同 key 的后续请求直接返回缓存，确保操作只执行一次。

**实现原理**：
1. `@Idempotent` 注解标记 Controller 方法 → `IdempotencyAspect`（`@Order(1)`）环绕拦截
2. 从请求头提取 key → 调用 `RedisIdempotencyService.execute()`
3. 先查 Redis 缓存（`eo:idempotency:{key}`），命中直接返回
4. 未命中 → 执行业务操作 → 通过 `SETNX` 原子写入（防止并发覆盖）
5. 执行异常 → 不缓存，重试可重新执行
6. Redis 不可用 → fail-open 透传请求

**与 `RateLimitFilter` 防重的关系**：

| 机制 | 窗口 | 标识 | 缓存响应 | 语义 |
|------|------|------|---------|------|
| `RateLimitFilter` 防重 | 3s | IP + URI + body hash | ❌ | 防快速连点 |
| `@Idempotent` 幂等 | 24h | 客户端提供的 key (UUID) | ✅ 返回相同结果 | 协议级幂等 |

**部署方式**：标注在 Controller 方法上即可，不要求所有客户端使用。未传 `Idempotency-Key` 头的请求正常执行（向后兼容）。

```java
@PostMapping("/orders")
@Idempotent
public Result<String> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return Result.success(commandHandler.handle(request));
}
```

## 修改注意

- **所有框架配置类统一使用 `@AutoConfiguration` + `AutoConfiguration.imports`**：framework 模块的所有 `@Configuration` 类必须使用 `@AutoConfiguration`（而非 `@Configuration`）并列入 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，不依赖隐式 `@ComponentScan` 发现。新增框架配置类时须同时完成两件事：① 类上加 `@AutoConfiguration`；② 在 imports 文件中追加一行。这是框架 bean 注册的唯一入口，确保配置注册无需依赖主应用包路径
- Security 配置变更需同步检查所有模块的接口权限
- Redis Key 命名规范: `eo:模块:业务:标识`
- 新增 AOP 切面需评估性能影响
- **JacksonConfig 统一使用 Jackson 3.x**：不再配置 Jackson 2.x `ObjectMapper`。通过 `JsonMapperBuilderCustomizer` 注册 `ToStringSerializer`（Long→String 防止 JS 精度丢失）到 Spring Boot 4 自动配置的 Jackson 3 `ObjectMapper`；同时保留 `jsonMapper()` Bean 供显式注入。两者均注册 `Long.class` 和 `long` 基本类型序列化。
- **`ParameterNamesModule` 由 Spring Boot 4 自动配置**，领域事件 record 无需 @JsonCreator 注解即可反序列化；JacksonConfig 不再需要手动注册
- **WebMvcConfig 不再重写 `extendMessageConverters`**：Spring Boot 4.0 使用 Jackson 3.x 的 HTTP 消息转换器，`MappingJackson2HttpMessageConverter`（Jackson 2.x）配置已无效
- **RedisWorkerIdProvider 优雅降级**：Redis 不可用时 `afterPropertiesSet()` 自动降级至 workerId=0，不影响应用启动。`DisposableBean.destroy()` 在 Spring 关闭时释放 Redis WorkerId 租约。请勿移除这些异常处理，否则 Redis 故障会导致启动失败
- **RedisBitmapBloomFilter 哈希偏移量**：`hash()` 方法使用 `Math.floorMod()` 计算位偏移量，避免 Java `%` 在负值时产生负数偏移。修改哈希逻辑需保持 `Math.floorMod`，否则 `SETBIT` 会收到非法偏移量
- **RedisConfig 显式配置序列化器（2026-07-23）**：Spring Boot 4 `DataRedisAutoConfiguration` 不设任何序列化器（默认 `JdkSerializationRedisSerializer`，二进制 key/value 破坏 Lua `tonumber` 且不可读）。`RedisConfig` 通过 `@AutoConfigureBefore` 注入自定义 `@Bean RedisTemplate<Object, Object>`：`StringRedisSerializer`（key/hashKey）+ `GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(BasicPolymorphicTypeValidator).build()`（value/hashValue，含默认类型信息以便 `CacheUtils.cast()` 还原为原类型）。修改序列化策略时须同步评估所有 `RedisTemplate` 使用方
- **配置属性类统一使用 `@ConfigurationProperties` + `@ConfigurationPropertiesScan` 模式**（纯 POJO，无需 `@Component`）：新建配置类时优先使用 Properties 类绑定，不新增 `@Value` 散落配置。默认值在 Properties 类中定义，通过 profile-specific yaml 覆盖。主应用类 `EasyOrangeApplication` 已添加 `@ConfigurationPropertiesScan`，自动扫描所有 `@ConfigurationProperties` 类。推荐加 `@Validated` + Jakarta Validation 约束（`@Min`/`@NotBlank`/`@NotNull` 等）实现启动时 fail-fast 验证，替代手写 `@PostConstruct validate()`— 后者仅在需要输出警告而非错误时保留
- **`@Idempotent` 幂等切面（`IdempotencyAspect`）**：`@Order(1)`，在 `RateLimitFilter(0)` 之后、`AuditLogAspect(3)` 之前执行。此顺序确保：① Filter 层先做快速防重；② 幂等拦截命中后不记录审计日志（避免重复日志）；③ 只有未缓存的请求会走到业务逻辑和日志记录。修改 Aspect 的 `@Order` 值时需评估这三层的影响
- **`RateLimitFilter` 支持 `@SkipRateLimit`/`@SkipRepeatSubmit`**：Filter 通过 `HandlerMapping` 解析目标 Controller 方法，检查方法或类上的 Skip 注解后跳过对应检查。支持类级（`@Inherited` 继承）和方法级。无法解析 handler（如静态资源）时放行默认规则
- **`RateLimitFilter` 使用 `ObjectProvider<List<HandlerMapping>>` 延迟注入**：`HandlerMapping` 列表通过 `ObjectProvider` 延迟解析，而非构造器直接注入。原因是直接注入 `List<HandlerMapping>` 会触发 `DelegatingWebSocketMessageBrokerConfiguration` → `WebSocketConfig` → `WebSocketAuthInterceptor` → `JwtDecoder`（`SecurityConfig` 中的 Bean）→ `SecurityConfig` → `RateLimitFilter` 的循环依赖。`ObjectProvider` 在请求时才解析 HandlerMapping，打破循环。修改 `RateLimitFilter` 构造器时不要改回 `@RequiredArgsConstructor` + `List<HandlerMapping>` 直接注入
