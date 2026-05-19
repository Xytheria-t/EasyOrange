# easyorange-framework 模块指南

框架基础设施层，为所有业务模块提供技术支撑：Security、Redis、事件发布、AOP、文件、日志。

## 目录结构

```
framework/
├── aspectj/                 # AOP 切面
│   ├── OperLogAspect.java       # 操作日志切面 (@Log)
│   ├── RateLimiterAspect.java   # 限流切面 (@RateLimiter, Redis+Lua)
│   └── RepeatSubmitAspect.java  # 防重提交切面 (@RepeatSubmit)
├── config/                  # 框架配置
│   ├── async/                   # 线程池 + Jackson
│   │   ├── ThreadPoolConfig.java
│   │   ├── JacksonConfig.java
│   │   └── LoggingRejectedExecutionHandler.java
│   ├── cache/                   # 本地缓存 (Caffeine)
│   │   └── LocalCacheConfig.java
│   ├── database/                # MyBatis-Plus 配置
│   │   └── MybatisPlusConfig.java
│   ├── http/                    # RestClient 配置
│   │   └── RestClientConfig.java
│   ├── properties/              # 配置属性类
│   │   ├── JwtProperties.java
│   │   ├── SecurityProperties.java
│   │   ├── RateLimiterProperties.java
│   │   ├── OperLogProperties.java
│   │   ├── ThreadPoolProperties.java
│   │   └── WebMvcProperties.java
│   ├── redis/                   # Redis 配置
│   │   ├── RedisConfig.java
│   │   └── CacheConfig.java
│   ├── security/                # Spring Security 配置
│   │   ├── SecurityConfig.java
│   │   └── JsonLogoutSuccessHandler.java
│   └── web/                     # WebMVC 配置
│       ├── WebMvcConfig.java
│       ├── ResponseAdvice.java      # 统一响应包装
│       └── RequestConfig.java
├── entity/
│   └── BaseDO.java              # 数据对象基类 (id, createTime, updateTime, delFlag, version)
├── repository/
│   ├── BaseRepository.java      # 仓储基类，封装 lambdaQuery()/lambdaUpdate() + findOne/findList/findIn 等常见查询模式
├── event/                   # 领域事件基础设施
│   └── DomainEventPublisherImpl.java    # 事件发布实现 (同步到 Spring EventBus)
├── outbox/                  # Outbox 模式 (事件可靠投递，支付模块使用)
│   ├── util/
│   │   └── OutboxEventUtils.java        # 共享工具 (截断+反序列化)
│   ├── entity/
│   │   ├── OutboxMessage.java           # 领域模型
│   │   └── OutboxMessagePO.java         # 持久化实体 (唯一 PO 映射 eo_domain_event)
│   ├── mapper/
│   │   └── OutboxMessageMapper.java     # MyBatis Mapper
│   ├── converter/
│   │   └── OutboxMessageConverter.java  # PO ↔ Domain 转换
│   └── repository/
│       └── OutboxRepository.java        # 仓储实现
├── exception/
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   └── CacheTypeMismatchException.java
├── file/                    # 文件上传下载
│   ├── controller/FileController.java
│   ├── service/FileService.java
│   ├── service/impl/FileServiceImpl.java
│   ├── service/ImageProcessingService.java
│   ├── service/impl/ImageProcessingServiceImpl.java
│   ├── dto/UploadFileResponse.java
│   ├── entity/UploadFile.java
│   └── mapper/UploadFileMapper.java
├── filter/                  # Servlet 过滤器
│   ├── JwtAuthenticationFilter.java   # JWT 认证过滤器
│   ├── XssFilter.java                 # XSS 过滤
│   └── XssHttpServletRequestWrapper.java
├── handler/                 # 处理器
│   ├── CustomMetaObjectHandler.java   # MyBatis-Plus 自动填充
│   ├── JsonAuthenticationEntryPoint.java # 未认证响应
│   └── LoggingInterceptor.java        # 请求日志拦截
├── manager/
│   └── AsyncManager.java         # 异步任务管理器
├── notification/
│   └── DefaultNotificationServiceImpl.java # 通知默认实现
├── operlog/                 # 操作日志
│   ├── entity/SysOperLog.java
│   ├── mapper/SysOperLogMapper.java
│   ├── service/SysOperLogService.java
│   ├── service/impl/SysOperLogServiceImpl.java
│   ├── OperLogArchiveService.java
│   └── dto/LogStorageStats.java
├── redis/                   # Redis 缓存抽象
│   ├── RedisCache.java           # 缓存接口
│   └── impl/RedisCacheImpl.java  # 实现 (String/Hash/List + 分布式锁)
├── service/                 # Token 服务
│   ├── TokenService.java
│   └── impl/TokenServiceImpl.java
└── util/                    # 工具类
    ├── JwtUtil.java              # JWT 工具
    ├── SecurityContextUtil.java  # 安全上下文工具
    ├── OperLogUtil.java          # 操作日志工具
    ├── RequestUtil.java          # 请求工具
    └── FileUtils.java            # 文件工具
```

## 核心机制

### JWT 认证流程

1. `JwtAuthenticationFilter` 拦截请求，从 Header 提取 Token
2. `TokenServiceImpl` 验证 Token，加载用户信息到 SecurityContext
3. Access Token 过期后通过 Refresh Token 刷新
4. 登出时 Token 加入 Redis 黑名单

### 领域事件发布流程

`DomainEventPublisherImpl` 将事件同步发布到 Spring ApplicationEventBus。业务模块可直接注入 `DomainEventPublisher` 调用 `publish()`。需要 Outbox 可靠投递的模块（如支付）通过 `OutboxRepository` 在业务事务内持久化事件，由各模块自行调度发布。

### Redis 缓存抽象

```java
RedisCache.set(key, value, timeout, unit)
RedisCache.get(key, clazz)
RedisCache.tryLock(key, value, timeout)  // 分布式锁
RedisCache.unlock(key, value)
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
