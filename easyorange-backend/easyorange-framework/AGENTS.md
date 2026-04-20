# easyorange-framework

**Module:** Core infrastructure — Security, Redis, MyBatis-Plus, AOP, file handling

## OVERVIEW

Infrastructure layer: JWT security chain, Redis config, MyBatis-Plus plugins, AOP aspects (@Log, @RepeatSubmit), file upload, operation logging, thread pool.

## STRUCTURE

```
easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/
├── aspectj/             # LogAspect (@Log AOP), RepeatSubmitAspect
├── config/              # SecurityConfig, RedisConfig, MybatisPlusConfig, CorsConfig, JacksonConfig, ThreadPoolConfig
│   └── properties/      # SecurityProperties, etc. (@ConfigurationProperties)
├── context/             # SecurityContextHolder, UserContext
├── exception/           # GlobalExceptionHandler (@RestControllerAdvice)
├── file/                # File upload/download (controller, service, entity, mapper)
├── filter/              # JwtAuthenticationFilter, XssFilter
├── handler/             # AuthenticationEntryPointImpl, LogoutSuccessHandlerImpl, CustomMetaObjectHandler
├── manager/             # Async manager (operation log queue)
├── operlog/             # Operation log persistence (entity, mapper, service)
├── redis/               # RedisTemplate config, Redis utils
├── service/             # Framework-level shared services
└── util/                # SecurityUtils, ServletUtils, etc.
```

## WHERE TO LOOK

| Task | File | Notes |
|------|------|-------|
| JWT filter chain | `config/SecurityConfig.java` | HttpSecurity 配置，放行路径 |
| Token 解析 | `filter/JwtAuthenticationFilter.java` | 从 Header 提取 JWT |
| Redis 序列化 | `config/RedisConfig.java` | Jackson2JsonRedisSerializer |
| MyBatis 插件 | `config/MybatisPlusConfig.java` | PaginationInnerInterceptor, 自动填充 |
| 操作日志 AOP | `aspectj/LogAspect.java` | 异步写入 operlog 表 |
| 全局异常 | `exception/GlobalExceptionHandler.java` | @RestControllerAdvice |
| XSS 过滤 | `filter/XssFilter.java` | 请求参数清洗 |
| 自动填充 | `handler/CustomMetaObjectHandler.java` | createTime, updateTime 自动填充 |

## CONVENTIONS

- Security 放行路径在 `application.yaml` → `security.ignore-paths` 配置
- Redis key 前缀统一使用 `CacheConstants` 中定义的常量
- 操作日志通过 `@Async` 异步写入，不要同步调用
- XSS 过滤作用于所有 POST 请求参数

## DEPENDENCIES

```
easyorange-framework → easyorange-common
```

## COMMANDS

```bash
mvn clean install -pl easyorange-framework
```
