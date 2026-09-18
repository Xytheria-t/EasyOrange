# easyorange-framework 模块指南

框架基础设施层，为所有业务模块提供技术支撑：Security、Redis、事件发布、AOP、文件、日志。

## 目录结构

```
framework/
├── audit/            # AuditLogAspect + AuditLogService（写操作审计，Outbox 事件化入库）
├── auth/             # TokenService + TokenServiceImpl（JWT 签发/刷新/吊销）
├── config/           # 框架配置：async / cache / database / idgen / jackson / lock / properties / redis / security / web
├── event/            # 事件基础设施：core（EventConsumerHandler）/ dlq / idempotency / metadata / metrics
├── exception/        # GlobalExceptionHandler（统一 Result<T> 信封）
├── file/             # 文件上传下载（FileController / FileService / FileStorage）
├── idgen/            # UuidV7IdGenerator
├── lock/             # DistributedLockPort + DistributedRedissonLockAdapter + LockAcquisitionException
├── messaging/        # RabbitMQ + Spring Modulith：config / core（ModulithDomainEventPublisher）
├── metrics/          # BusinessMetricsService + MetricsConfig
├── mybatis/          # SlowSqlInterceptor（500ms 慢 SQL 指标）
├── util/             # FileUtils / LocalRateLimiter / SecurityContextUtil / TestSecurityUtil
└── web/              # cookie / filter（RateLimitFilter、IdempotencyKeyFilter、TokenRevocationFilter、RefreshCsrfFilter）/ handler / idempotency / ErrorResponseWriter
```

## 核心机制

### JWT 认证

由 Spring Security OAuth2 Resource Server 内置 `BearerTokenAuthenticationFilter` 处理，**无自定义认证 Filter**：

1. 从 `Authorization: Bearer xxx` 提取 Token → `JwtDecoder` 验签（Nimbus）+ issuer
2. `JwtAuthenticationConverter` 检查 token type（拒绝 refresh token），从 `"authorities"` claim 读权限，构造 `AuthUser` 写入 `SecurityContext`
3. `TokenRevocationFilter` 在认证完成后执行黑名单 + 强制登出检查（吊销状态与密码学校验职责分离）
4. 签发用 `JwtEncoder`；登出把 jti 加入 Redis 黑名单（TTL = 剩余有效期，自动过期）
5. `WebSocketAuthInterceptor` 复用同一 `JwtDecoder` bean 做握手认证

> **管理员判定**：`UserType.getDefaultRoles()`（领域枚举，在 `AuthAppService.login()` 调用）决议 `UserType → authorities`，把 `["ROLE_ADMIN","ROLE_USER"]` 或 `["ROLE_USER"]` 写进 JWT 的 `"authorities"` claim，资源服务器直接读 claim，不重新判定。

### 领域事件发布与消费

业务模块只注入 `DomainEventPublisher.publish()`，实际由 `ModulithDomainEventPublisher`（`@Primary`）代理到 `ApplicationEventPublisher`：Spring Modulith 把事件持久化到 `EVENT_PUBLICATION`（与应用事务同原子），提交后异步发布到 `eo.domain.events` Topic Exchange。`@ConditionalOnProperty(matchIfMissing=true)` 支持无 RabbitMQ 环境启动。

消费者统一用 `EventConsumerHandler` 组合，封装五个横切关注点：

1. **幂等去重**：`EventIdempotencyChecker`（Redis `SET NX EX` 一条原子命令领取处理权 + 24h TTL；失败时 `unmark` 撤销标记让重投可重新执行），命名空间 `consumerId + ":" + eventType`；`idempotencyEnabled=false` 构造器关闭投影/广播/指标类消费者
2. **事件元数据**：`EventMetadataMessagePostProcessor` 发布前注入 eventId/timestamp/traceId；`EventMetadata.from(message, event)` 消费端解码
3. **指标埋点**：`EventMetricsService` 上报 `easyorange.events.received{type,outcome}` / `.duration` / `.dlq{queue,reason}`
4. **DLQ 监听**：`DlqAnomalyListener` 单个 `@RabbitListener` 同时监听 11 个 DLQ 队列，提取 x-death header 记指标
5. **组合**：`EventConsumerHandler.handle(event, message, metadata -> ...)`，业务逻辑写在 lambda 里

### Spring Cache 注解式缓存（cache/）

手写多级缓存（`MultiLevelCache` + Pub/Sub 广播）与 `CacheUtils` / `LocalCacheConfig` 已移除，统一为 **Spring Cache 注解 + 纯 Redis 单层**。配置项与使用示例见 [CONFIGURATION.md](./CONFIGURATION.md#缓存配置)。

**行为约定**：

- `RedisCacheConfig`：`@EnableCaching` + `RedisCacheManager`（String key + `GenericJacksonJsonRedisSerializer` value）+ 统一 TTL（`easyorange.cache.default-ttl`）+ `CacheErrorHandler` fail-open（Redis 故障读直查 DB、写放弃缓存）
- 缓存 key 形如 `eo:product:info::<id>`；图片处理缓存 `imageProcessCache`（Caffeine）独立使用
- **防穿透靠缓存 null**：不要给 `@Cacheable` 加 `unless = "#result == null"`；列表类缓存用 `orEmpty` 兜成可变空列表（见 `CategoryCacheAdapter`）
- **序列化坑**：`java.*` 包 final 类型（`Optional`、`List.of()` 的不可变列表）不带类型信息、无法反序列化——缓存值必须是 POJO/record（`com.cartethyia.*`）或可变 `ArrayList`

> 布隆过滤器（`BloomFilter` / `RedisBitmapBloomFilter`）随同一轮缓存简化一并删除，防穿透由 null 缓存承担。原 `Resilience4jConfig` 也在此轮删除（无消费者），Redis 故障降级统一由 `CacheErrorHandler` 承担。

### 日志与 MDC 传播

- **traceId 自动注入**：`micrometer-tracing-bridge-brave` + Spring Boot 4 自动配置 `Slf4jScopeDecorator`，HTTP 请求进入时 Brave `TracingFilter` 开 span，无需手写 UUID
- **异步线程 MDC**：`MdcTaskDecorator`（`TaskDecorator` 实现，类级 `@NullMarked` 匹配父接口契约）在主线程快照 `MDC.getCopyOfContextMap()`、子线程 `setContextMap` + `finally MDC.clear()`。**只覆盖 `@Scheduled`**——`@Async` 在虚拟线程模式下由 Micrometer Tracing 自动继承 MDC
- **业务字段**：`LoggingInterceptor` 注入 `clientIp` / `method` / `uri` / `fullUrl`
- **异步日志写入**：`logback-spring.xml` 用 `ASYNC_FILE` / `ASYNC_ERROR_FILE` / `ASYNC_JSON_FILE` 包装同步 Appender，`queueSize=1024` / `discardingThreshold=0` / `neverBlock=true`；prod 走 Spring Boot 4 内置 `StructuredLogEncoder`（logstash 格式）

### Idempotency-Key 幂等（web/）

客户端传 `Idempotency-Key` 头（UUID v4），服务端缓存**序列化后的 HTTP 响应**，相同 key 的后续请求字节级回放——替代原 `@Idempotent` + `IdempotencyAspect` AOP 方案。首次执行缓存（`eo:idempotency:{key}`）；非 2xx（业务异常/校验失败/未认证）**不缓存**，允许客户端重试；Redis 不可用 fail-open 透传。

| 机制 | 窗口 | 标识 | 缓存响应 | 语义 |
|------|------|------|---------|------|
| `RateLimitFilter` 防重 | 3s | IP + URI + body hash | ❌ | 防快速连点 |
| `IdempotencyKeyFilter` 幂等 | 24h | 客户端提供的 key | ✅ 返回相同结果 | 协议级幂等 |

在 `application.yaml` 的 `idempotency.path-patterns` 配置要保护的写端点，零注解；未传头的请求正常执行。

## 修改注意

- **所有框架配置类统一 `@AutoConfiguration` + `AutoConfiguration.imports`**：新增框架配置类必须①类上改 `@AutoConfiguration`；②在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 追加一行。这是框架 bean 注册的唯一入口，不依赖隐式 `@ComponentScan`
- **`RateLimitFilter` 用 `ObjectProvider<List<HandlerMapping>>` 延迟注入**：直接注入 `List<HandlerMapping>` 会触发 `DelegatingWebSocketMessageBrokerConfiguration` → `WebSocketConfig` → `WebSocketAuthInterceptor` → `JwtDecoder` → `SecurityConfig` → `RateLimitFilter` 的循环依赖。**不要改回 `@RequiredArgsConstructor` + 直接注入**
- **`RateLimitFilter` 支持 `@SkipRateLimit` / `@SkipRepeatSubmit`**：通过 `HandlerMapping` 解析目标 Controller 方法，检查方法或类上的注解（支持 `@Inherited` 类级）；无法解析 handler（如静态资源）时放行默认规则
- **`IdempotencyKeyFilter` 注册**：作为 `@Component` 自动注册为 servlet filter，并在 `SecurityConfig` 用 `addFilterBefore(idempotencyKeyFilter, AnonymousAuthenticationFilter.class)` 置于安全链最外层以抓取最终响应。改 filter 顺序需评估与 `RateLimitFilter` / 安全过滤器的包裹关系
- **`RedisConfig` 必须显式配置序列化器**：Spring Boot 4 `DataRedisAutoConfiguration` 不设任何序列化器（默认 `JdkSerializationRedisSerializer`，二进制 key/value 既破坏 Lua `tonumber` 又不可读）。`RedisConfig` 用 `@AutoConfigureBefore` 注入 `@Bean RedisTemplate<Object,Object>`：`StringRedisSerializer`（key/hashKey）+ `GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(BasicPolymorphicTypeValidator).build()`（value/hashValue）。改序列化策略须同步评估所有使用方
- **`JacksonConfig` 统一 Jackson 3.x**：通过 `JsonMapperBuilderCustomizer` 注册 `ToStringSerializer`（Long→String 防 JS 精度丢失，同时注册 `Long.class` 与 `long`）；保留 `jsonMapper()` bean 供显式注入。不配置 Jackson 2.x `ObjectMapper`；`ParameterNamesModule` 由 Boot 4 自动配置，事件 record 无需 `@JsonCreator`
- **`WebMvcConfig` 不再重写 `extendMessageConverters`**：Boot 4 用 Jackson 3.x 的 HTTP 消息转换器，`MappingJackson2HttpMessageConverter`（Jackson 2.x）配置已无效
- **配置属性类统一 record + `@ConfigurationProperties` + `@ConfigurationPropertiesScan`**（构造器绑定）：不新增 `@Value` 散落配置，类上不加 `@Component`。默认值——标量/字符串/`Duration` 用组件上的 `@DefaultValue`；集合与嵌套 record 在紧凑构造器兜底（顺带 `List.copyOf`/`Map.copyOf` 保证不可变，替代 getter 里的防御性拷贝）。`@Validated` + Jakarta 约束实现启动期 fail-fast，嵌套对象需在父组件加 `@Valid` 才级联。**构造器绑定下属性源无法表达 null，null 兜底只能靠紧凑构造器**。注册：应用侧由 `EasyOrangeApplication` 的 `@ConfigurationPropertiesScan` 统一注册，业务模块 properties 不必逐个 `@EnableConfigurationProperties`；框架模块内的随该模块 auto-config 注册（`MybatisPlusConfig` / `LockConfig`），**不要把 `@EnableConfigurationProperties` 挂到适配器或其它组件上**
- **取值面固定的字段用枚举而非 String**：宽松绑定让 yml 小写值照常生效，非法值在绑定期即失败（`RateLimitFilterProperties.Strategy`、`SlowSqlProperties.LogLevel`；Cookie SameSite 直接用框架 `Cookie.SameSite`）。嵌套组默认值保持单一来源：字段 `@DefaultValue(CONST + "")` 与紧凑构造器的兜底实例共用同一个 `static final` 常量
- **record 组件文档写类级 Javadoc 的 `@param`**：组件上的 `/** … */` 是 dangling doc comment，根 pom 开了 `-Xlint:dangling-doc-comments` 会报警
- **record 无法被 Mockito mock**（本仓库 MockMaker 固定 subclass，record 是 final 类）：测试用 `testsupport/PropertyBindings` 经真实 Binder 构造（`bindOrCreate` + 空属性源即全默认值，覆盖项按相对 prefix 的 kebab-case 键传入）
- **yml 里形如 `x:y` 的值必须加引号**：SnakeYAML 会把裸写的 `1:1` 当六十进制整数解析成 61（`4:3` → 243），绑到 String 上拿到 `"61"`。已踩点 `easyorange.file.image.smart-crop.default-aspect-ratio`
- Redis Key 命名规范：`eo:模块:业务:标识`
- Security 配置变更需同步检查所有模块的接口权限；新增 AOP 切面需评估性能影响
- **AI 重试 / Bulkhead 已随 Spring AI 框架化删除**（ADR-0008）：重试与并发隔离由 `AiModelConfig` 的 `OpenAiSetup.setupSyncClient`（openai-java 客户端内置）承担，AI 模块不再注入本模块的 `Retry` / `Bulkhead` bean
