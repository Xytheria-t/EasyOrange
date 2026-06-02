# easyorange-framework 模块指南

框架基础设施层，为所有业务模块提供技术支撑：Security、Redis、事件发布、AOP、文件、日志。

## 目录结构

```
framework/
├── async/                   # 异步任务管理
│   └── AsyncManager.java         # 异步任务管理器
├── bloom/                   # 布隆过滤器 (Redis Bitmap)
│   ├── BloomFilter.java          # 过滤器接口
│   └── RedisBitmapBloomFilter.java # Redis Bitmap 实现 (Lua 原子操作)
├── cache/                   # 缓存抽象 (多级缓存门面 + Redis 缓存实现)
│   ├── CacheLoader.java          # 回源加载器函数式接口
│   ├── MultiLevelCache.java      # L1 Caffeine → L2 Redis → DB 三级串联
│   ├── RedisCache.java           # Redis 缓存接口 (String/Hash/List/Set/ZSet + Lua + 分布式锁 + SCAN)
│   └── impl/
│       └── RedisCacheImpl.java   # Redis 缓存实现 (含 Lua 原子解锁、SCAN 替代 KEYS 防阻塞)
├── config/                  # 框架配置
│   ├── async/                    # 线程池 + Jackson
│   │   ├── ThreadPoolConfig.java
│   │   ├── JacksonConfig.java
│   │   └── LoggingRejectedExecutionHandler.java
│   ├── cache/                    # 本地缓存 (Caffeine)
│   │   └── LocalCacheConfig.java
│   ├── constant/                 # 配置常量 (缓存 key 约定等)
│   │   └── LoginCacheConstants.java
│   ├── database/                 # MyBatis-Plus 配置
│   │   ├── MybatisPlusConfig.java
│   │   └── UuidTypeHandler.java
│   ├── http/                     # RestClient 配置
│   │   └── RestClientConfig.java
│   ├── properties/               # 配置属性类
│   │   ├── CacheProperties.java
│   │   ├── FileUploadProperties.java
│   │   ├── IdGenProperties.java
│   │   ├── ImageProcessingProperties.java
│   │   ├── JwtProperties.java
│   │   ├── OperLogProperties.java
│   │   ├── RateLimitFilterProperties.java
│   │   ├── SecurityProperties.java
│   │   ├── ThreadPoolProperties.java
│   │   └── WebMvcProperties.java
│   ├── redis/                    # Redis 配置
│   │   ├── RedisConfig.java
│   │   └── CacheConfig.java
│   ├── security/                 # Spring Security 配置
│   │   └── SecurityConfig.java
│   └── web/                      # WebMVC 配置
│       ├── WebMvcConfig.java
│       └── ResponseAdvice.java       # 统一响应包装
├── entity/
│   └── BaseDO.java               # 数据对象基类 (id, createTime, updateTime, delFlag, version)
├── event/                    # 领域事件基础设施
│   ├── DomainEventPublisherImpl.java   # 事件发布实现
│   └── idempotency/
│       └── EventIdempotencyChecker.java # 事件幂等性检查
├── exception/
│   ├── GlobalExceptionHandler.java   # 全局异常处理
│   └── CacheTypeMismatchException.java
├── file/                     # 文件上传下载
│   ├── adapter/inbound/web/controller/FileController.java
│   ├── dto/UploadFileVO.java
│   ├── entity/UploadFile.java
│   ├── mapper/UploadFileMapper.java
│   ├── service/
│   │   ├── FileService.java
│   │   ├── ImageProcessingService.java
│   │   └── impl/
│   │       ├── FileServiceImpl.java
│   │       └── ImageProcessingServiceImpl.java
│   └── storage/
│       ├── FileStorage.java
│       └── LocalFileStorage.java
├── hash/                    # 一致性哈希 (分布式路由)
│   ├── Node.java                 # 节点接口
│   └── ConsistentHashRouter.java # 虚拟节点 TreeMap 路由 (MD5 哈希)
├── idgen/                   # 分布式 ID 生成器
│   ├── SnowflakeIdGenerator.java     # 增强版 Snowflake (时钟回拨容忍 + Redis WorkerId)
│   ├── WorkerIdProvider.java         # 工作节点接口
│   └── RedisWorkerIdProvider.java    # Redis 自动注册 + 心跳续期
├── messaging/               # RabbitMQ 消息队列
│   ├── config/                    # RabbitMQ 配置
│   │   ├── RabbitMQConfig.java
│   │   └── RabbitMQProperties.java
│   ├── core/                      # 核心消息发布
│   │   ├── RabbitMQDomainEventPublisher.java
│   │   └── RoutingKeyResolver.java
│   └── reliability/               # 可靠投递 (Confirm/Return)
│       ├── ConfirmCallback.java
│       └── ReturnCallback.java
├── metrics/                 # 业务指标埋点
│   ├── BusinessMetricsService.java
│   └── MetricsConfig.java
├── notification/
│   └── DefaultNotificationServiceImpl.java # 通知默认实现
├── operlog/                 # 操作日志 (含 AOP 切面)
│   ├── aspect/
│   │   └── OperLogAspect.java        # 操作日志切面 (约定式拦截所有写操作, 无需注解)
│   ├── entity/SysOperLog.java
│   ├── mapper/SysOperLogMapper.java
│   └── service/
│       ├── SysOperLogService.java
│       └── impl/SysOperLogServiceImpl.java
├── outbox/                  # Outbox 模式 (事件可靠投递)
│   ├── util/
│   │   └── OutboxEventUtils.java
│   ├── entity/
│   │   ├── OutboxMessage.java
│   │   └── OutboxMessagePO.java
│   ├── mapper/
│   │   └── OutboxMessageMapper.java
│   ├── converter/
│   │   └── OutboxMessageMapper.java
│   ├── publisher/
│   │   └── OutboxPublisher.java
│   └── repository/
│       └── OutboxRepository.java
├── repository/
│   └── BaseRepository.java       # 仓储基类 (lambdaQuery/lambdaUpdate + 常见查询模式)
├── service/                 # Token 服务
│   ├── TokenService.java
│   ├── TokenRefreshResult.java
│   └── impl/TokenServiceImpl.java
├── util/                    # 纯工具函数
│   ├── FileUtils.java            # 文件工具
│   ├── JwtUtil.java              # JWT 工具
│   ├── LocalRateLimiter.java     # 本地固定窗口限流器
│   ├── OperLogUtil.java          # 操作日志工具
│   ├── RequestUtil.java          # 请求工具 (自动识别代理头)
│   ├── SecurityContextUtil.java  # 安全上下文工具
│   └── TestSecurityUtil.java     # 测试安全上下文工具
└── web/                     # Web 层 (过滤器 + 处理器)
    ├── filter/                    # Servlet 过滤器
    │   ├── CachedBodyHttpServletRequestWrapper.java
    │   ├── JwtAuthenticationFilter.java    # JWT 认证过滤器
    │   ├── RateLimitFilter.java            # 限流过滤器
    │   ├── XssFilter.java                  # XSS 过滤
    │   └── XssHttpServletRequestWrapper.java
    └── handler/                    # 处理器
        ├── CustomMetaObjectHandler.java    # MyBatis-Plus 自动填充
        ├── JsonAuthenticationEntryPoint.java # 未认证响应
        └── LoggingInterceptor.java         # 请求日志拦截
```

## 核心机制

### JWT 认证流程

1. `JwtAuthenticationFilter` 拦截请求，从 Header 提取 Token
2. `JwtUtil` 解析签名验证，`TokenServiceImpl` 检查 Redis 黑名单（仅登出 token 有 Redis 开销）
3. Access Token 过期后通过 Refresh Token 刷新（轮换：作旧 + 颁新对）
4. 登出时 Token 的 jti 加入 Redis 黑名单（TTL = 剩余有效期，自动过期）

### 领域事件发布流程

`DomainEventPublisherImpl` 将事件同步发布到 Spring ApplicationEventBus。业务模块可直接注入 `DomainEventPublisher` 调用 `publish()`。需要 Outbox 可靠投递的模块（如支付）通过 `OutboxRepository` 在业务事务内持久化事件，由各模块自行调度发布。

### Redis 缓存抽象

```java
// 基础操作
RedisCache.set(key, value, timeout, unit)
RedisCache.get(key, clazz)
RedisCache.delete(key)

// 位图操作 (Bloom Filter 使用)
RedisCache.setBit(key, offset, value)
RedisCache.getBit(key, offset)
RedisCache.bitCount(key)

// 分布式锁 (Lua 脚本原子释放, UUID 防误删)
RedisCache.tryLock(key, value, timeout, unit)
RedisCache.unlock(key, value)

// 键扫描 (SCAN 替代 KEYS, 不阻塞 Redis)
RedisCache.keys(pattern)

// Lua 脚本执行
RedisCache.executeLuaScript(script, keys, args)

// 批量操作、Hash/List/Set/ZSet 等完整 API 见 RedisCache.java 接口
```

> **重要实现细节**: `keys()` 使用 `SCAN` 命令（cursor 迭代, count=1000），避免生产环境 `KEYS *` 阻塞 Redis。`unlock()` 和 `unlockIfValueMatches()` 使用 Lua 脚本原子 compare-and-delete。新增 `setBit`/`getBit`/`bitCount` 位图操作用于支持布隆过滤器。

### 布隆过滤器 (bloom/)

Redis 位图实现的布隆过滤器，用于缓存穿透防护：

- 默认配置: 100 万预期插入, 1% 假阳性率 → 约 1.2MB 内存, 7 个哈希函数
- 哈希策略: Murmur3 128-bit 拆两个 long → k 个偏移量 (Less Hashing, Same Performance)
- Lua 脚本保证 SETBIT/GETBIT 原子性 (单次 Redis 调用完成 k 次位操作)
- 支持自定义预期数据量和假阳性率

```java
// 使用默认配置
bloomFilter.put("eo:bloom:product:id", productId.toString());
boolean exists = bloomFilter.mightContain("eo:bloom:product:id", productId.toString());

// 自定义参数 (100 万, 1%)
var custom = new RedisBitmapBloomFilter(redisCache, 1_000_000L, 0.01);
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

### 分布式 ID 生成器 (idgen/)

增强版 Snowflake，通过 Redis 自动分配 WorkerId：

- 支持最多 32 个工作节点 (5 bit WorkerId)
- Redis WorkerId 自动注册 + 心跳续期防过期
- 容忍 10ms 内时钟回拨 (等待 + 自旋)
- 可配置 DataCenterId

```properties
# 启用 Snowflake ID 生成器 (默认关闭)
easyorange.idgen.enabled=true
easyorange.idgen.data-center-id=1
```

### 一致性哈希 (hash/)

```java
List<RedisNode> nodes = List.of(new RedisNode("node-1"), new RedisNode("node-2"));
var router = new ConsistentHashRouter<>(nodes, 200);  // 200 虚拟节点/物理节点
RedisNode target = router.route("some-cache-key");
```

### 统一响应包装

`ResponseAdvice` 自动将 Controller 返回值包装为 `Result<T>`，无需手动包装。

## 修改注意

- Security 配置变更需同步检查所有模块的接口权限
- Redis Key 命名规范: `eo:模块:业务:标识`
- 新增 AOP 切面需评估性能影响
- **JacksonConfig 同时配置了 Jackson 2.x `ObjectMapper` 和 Jackson 3.x `JsonMapper`**：Spring Boot 4.0 默认使用 Jackson 3.x 作为 HTTP 消息转换器，两者都必须注册 `ToStringSerializer` 才能防止 Long 类型精度丢失。`JsonMapperBuilderCustomizer` 用于自动配置，`jsonMapper()` Bean 直接构建时也需添加模块
- **JacksonConfig 的 ObjectMapper 注册了 ParameterNamesModule**，领域事件类无需 @JsonCreator 注解即可反序列化；修改 JacksonConfig 时勿遗漏此模块
- **WebMvcConfig 不再重写 `extendMessageConverters`**：Spring Boot 4.0 使用 Jackson 3.x 的 HTTP 消息转换器，`MappingJackson2HttpMessageConverter`（Jackson 2.x）配置已无效
- **RedisWorkerIdProvider 优雅降级**：Redis 不可用时 `afterPropertiesSet()` 自动降级至 workerId=0，不影响应用启动。`DisposableBean.destroy()` 在 Spring 关闭时释放 Redis WorkerId 租约。请勿移除这些异常处理，否则 Redis 故障会导致启动失败
- **RedisBitmapBloomFilter 哈希偏移量**：`hash()` 方法使用 `Math.floorMod()` 计算位偏移量，避免 Java `%` 在负值时产生负数偏移。修改哈希逻辑需保持 `Math.floorMod`，否则 `SETBIT` 会收到非法偏移量
- **RedisConfig 复用 JacksonConfig 的 ObjectMapper**：Redis 序列化使用与 HTTP 相同的 Jackson 配置（Long→String 序列化、ParameterNamesModule）。修改 JacksonConfig 时需考虑对 Redis 序列化的影响
- **配置属性类统一使用 `@ConfigurationProperties` + `@Component` 模式**：新建配置类时优先使用 Properties 类绑定，不新增 `@Value` 散落配置。默认值在 Properties 类中定义，通过 profile-specific yaml 覆盖
- **`@Component` 多构造器必须标注 `@Autowired`**：`MultiLevelCache` 和 `RedisBitmapBloomFilter` 都有多个构造器（默认参数 + 自定义参数），且无默认无参构造器。`@Component` 扫描时 Spring 无法自动选择构造器，必须在主构造器上加 `@Autowired` 明确指示。新增 `@Component` 类时有多个构造器时遵循此模式
