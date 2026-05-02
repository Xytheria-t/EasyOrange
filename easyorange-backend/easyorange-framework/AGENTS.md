# easyorange-framework Module Agents

Professional agent configuration for the framework infrastructure module.

## Module Overview

The `easyorange-framework` module provides the technical foundation for all business modules including:
- Spring Security configuration (JWT, CORS, HSTS)
- WebMVC configuration (interceptors, unified response)
- Redis caching abstraction and implementation
- MyBatis Plus configuration
- Thread pool and async configuration
- File upload/download management
- Operation logging (AOP-based)
- Domain event publishing infrastructure
- Rate limiting and repeat submit prevention
- Global exception handling

## Available Agents

### 1. **framework-security-agent**

**Purpose**: Handle security infrastructure

**When to use**:
- Modifying JWT authentication flow
- Adding security filters
- Configuring CORS or HSTS
- Updating token service logic

**Capabilities**:
- JWT filter configuration
- Security context management
- Token lifecycle management
- XSS filtering

**Example**:
```
"Add refresh token rotation"
"Implement JWT blacklist with Redis"
"Add OAuth2 resource server support"
```

### 2. **framework-cache-agent**

**Purpose**: Handle caching infrastructure

**When to use**:
- Adding new cache configurations
- Optimizing Redis operations
- Adding local cache (Caffeine)
- Cache serialization changes

**Capabilities**:
- Redis cache abstraction
- Local cache configuration
- Cache serialization
- Distributed locking

**Example**:
```
"Add multi-level cache (Caffeine + Redis)"
"Implement cache statistics monitoring"
"Add cache key prefix strategy"
```

### 3. **framework-event-agent**

**Purpose**: Handle domain event infrastructure

**When to use**:
- Modifying event publishing mechanism
- Adding event persistence
- Implementing event idempotency
- Adding async event execution

**Capabilities**:
- Event publisher implementation
- Event persistence service
- Idempotency checking
- Async event threading

**Example**:
```
"Add event replay capability"
"Implement event sourcing pattern"
"Add event dead letter queue"
```

### 4. **framework-log-agent**

**Purpose**: Handle operation logging and observability

**When to use**:
- Adding new log aspects
- Modifying log storage
- Adding performance metrics
- Implementing tracing

**Capabilities**:
- AOP logging
- Log storage optimization
- Performance monitoring
- Distributed tracing

**Example**:
```
"Add structured logging with MDC"
"Implement log aggregation"
"Add performance timing aspects"
```

## Agent Usage Patterns

### Standard Development Workflow

```
1. Identify the infrastructure need
   ↓
2. Choose appropriate agent
   ↓
3. Agent analyzes existing patterns
   ↓
4. Agent implements following TDD
   ↓
5. Code review with java-code-reviewer + security-reviewer
   ↓
6. Test and verify across dependent modules
```

### Agent Selection Matrix

| Task Type | Primary Agent | Secondary Agent |
|-----------|--------------|-----------------|
| Security changes | framework-security-agent | framework-cache-agent |
| Cache changes | framework-cache-agent | framework-event-agent |
| Event system | framework-event-agent | framework-log-agent |
| Logging/Observability | framework-log-agent | framework-cache-agent |
| Async/Threading | framework-event-agent | framework-log-agent |
| File handling | framework-log-agent | framework-security-agent |

## Architecture Patterns

### Infrastructure Layer

```
┌─────────────────────────────────────────────────────────────┐
│                    Configuration Layer                       │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  SecurityConfig       │    │  WebMvcConfig            │  │
│  │  - JWT Filter         │    │  - Interceptors          │  │
│  │  - CORS/HSTS          │    │  - Response Advice       │  │
│  ├──────────────────────┤    ├──────────────────────────┤  │
│  │  RedisConfig          │    │  ThreadPoolConfig        │  │
│  │  - Serialization      │    │  - Async Executor        │  │
│  │  - Connection         │    │  - Event Executor        │  │
│  ├──────────────────────┤    ├──────────────────────────┤  │
│  │  MybatisPlusConfig    │    │  LocalCacheConfig        │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                             │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  TokenServiceImpl     │    │  RedisCacheImpl          │  │
│  │  - Create/Refresh     │    │  - String/Hash/List      │  │
│  │  - Blacklist          │    │  - Distributed Lock      │  │
│  ├──────────────────────┤    ├──────────────────────────┤  │
│  │  FileServiceImpl      │    │  SysOperLogServiceImpl   │  │
│  │  - Upload/Download    │    │  - Log persistence       │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Aspect Layer                              │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  OperLogAspect        │    │  RateLimiterAspect       │  │
│  │  - Auto logging       │    │  - Redis + Lua           │  │
│  ├──────────────────────┤    ├──────────────────────────┤  │
│  │  RepeatSubmitAspect   │    │                          │  │
│  │  - Duplicate prevention│   │                          │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Event Infrastructure                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  DomainEventPublisherImpl                              │  │
│  │  - Persistence + Async publishing                      │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  DomainEventPersistenceService                         │  │
│  │  - Event storage                                       │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  EventIdempotencyChecker                               │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
framework/
├── config/
│   ├── async/
│   │   ├── ThreadPoolConfig.java
│   │   └── LoggingRejectedExecutionHandler.java
│   ├── cache/
│   │   └── LocalCacheConfig.java
│   ├── database/
│   │   └── MybatisPlusConfig.java
│   ├── http/
│   │   └── RestClientConfig.java
│   ├── properties/
│   │   ├── JwtProperties.java
│   │   ├── SecurityProperties.java
│   │   └── ...
│   ├── redis/
│   │   └── RedisConfig.java
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   └── JsonLogoutSuccessHandler.java
│   └── web/
│       ├── RequestConfig.java
│       ├── ResponseAdvice.java
│       └── WebMvcConfig.java
├── aspectj/
│   ├── OperLogAspect.java
│   ├── RateLimiterAspect.java
│   └── RepeatSubmitAspect.java
├── entity/
│   └── BaseDO.java
├── event/
│   ├── DomainEventPublisherImpl.java
│   ├── DomainEventPersistenceService.java
│   └── idempotency/
│       └── EventIdempotencyChecker.java
├── exception/
│   └── GlobalExceptionHandler.java
├── file/
│   ├── controller/
│   ├── service/
│   └── ...
├── filter/
│   ├── JwtAuthenticationFilter.java
│   └── XssFilter.java
├── handler/
│   ├── JsonAuthenticationEntryPoint.java
│   ├── LoggingInterceptor.java
│   └── CustomMetaObjectHandler.java
├── operlog/
│   ├── entity/
│   ├── mapper/
│   └── service/
├── redis/
│   ├── RedisCache.java
│   └── impl/
│       └── RedisCacheImpl.java
├── service/
│   ├── TokenService.java
│   └── impl/
│       └── TokenServiceImpl.java
└── util/
    ├── JwtUtil.java
    ├── SecurityContextUtil.java
    └── OperLogUtil.java
```

## Code Conventions

### BaseDO (All entities extend this)

```java
public class BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "2")
    private Integer delFlag;
    @Version
    private Integer version;
}
```

### Redis Cache Abstraction

```java
public interface RedisCache {
    <T> void set(String key, T value, long timeout, TimeUnit unit);
    <T> T get(String key, Class<T> clazz);
    boolean tryLock(String key, String value, long timeout);
    void unlock(String key, String value);
    // ... more operations
}
```

### Rate Limiter Aspect

```java
@Aspect
@Component
public class RateLimiterAspect {
    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint point, RateLimiter rateLimiter) {
        // Redis + Lua sliding window implementation
    }
}
```

## Testing Requirements

- **Unit Tests**: Utility classes, Token service
- **Integration Tests**: Filter chain, Cache operations
- **Security Tests**: JWT validation, XSS filtering
- **Coverage Target**: 80%+

## Integration Points

- **All business modules**: Depend on framework for infrastructure
- **easyorange-common**: Result, PageResult, BaseDO
- **easyorange-common-domain**: DomainEventPublisher
- **External**: Redis, MySQL, JWT libraries
