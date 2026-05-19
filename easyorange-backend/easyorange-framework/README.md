# EasyOrange Framework

框架核心模块 - 提供基础设施和通用功能

## 模块说明

`easyorange-framework` 是 EasyOrange 项目的核心框架模块，提供了以下功能：

### 核心功能

- **安全认证**：JWT 认证、密码加密、CORS 配置
- **缓存抽象**：Redis 缓存封装、本地缓存优化
- **领域事件**：事件发布与订阅机制
- **异常处理**：全局异常处理、友好错误信息
- **AOP 增强**：限流、防重复提交、操作日志
- **线程池管理**：异步任务执行、线程池监控
- **文件服务**：文件上传、存储管理

## 技术栈

- Spring Boot 4.0.3
- Spring Security
- Spring Data Redis
- JWT (jjwt 0.13.0)
- MyBatis-Plus 3.5.16
- Caffeine Cache
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
| `SecurityConfig` | Spring Security 配置 |
| `JwtAuthenticationFilter` | JWT 认证过滤器 |
| `TokenService` | Token 管理服务 |
| `JwtUtil` | JWT 工具类 |

### 缓存组件

| 组件 | 说明 |
|------|------|
| `RedisCache` | Redis 缓存抽象接口 |
| `RedisCacheImpl` | Redis 缓存实现 |
| `LocalCacheConfig` | 本地缓存配置 |

### 事件组件

| 组件 | 说明 |
|------|------|
| `DomainEventPublisher` | 领域事件发布接口 |
| `DomainEventPublisherImpl` | 领域事件发布实现（同步到 Spring EventBus） |

### AOP 组件

| 组件 | 说明 |
|------|------|
| `RateLimiterAspect` | 限流切面 |
| `RepeatSubmitAspect` | 防重复提交切面 |
| `OperLogAspect` | 操作日志切面 |

## 使用示例

### JWT 认证

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final TokenService tokenService;
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 验证用户
        User user = userService.authenticate(request);
        
        // 生成 token
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), user.getType());
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), user.getType());
        
        return Result.ok(new LoginResponse(accessToken, refreshToken));
    }
}
```

### 缓存使用

```java
@Service
public class UserService {
    
    private final RedisCache redisCache;
    
    public User getUserById(Long id) {
        String key = "user:" + id;
        
        // 先查缓存
        User user = redisCache.get(key, User.class);
        if (user != null) {
            return user;
        }
        
        // 查数据库
        user = userRepository.findById(id).orElse(null);
        if (user != null) {
            // 缓存结果
            redisCache.set(key, user, 30, TimeUnit.MINUTES);
        }
        
        return user;
    }
}
```

### 领域事件

```java
// 定义事件
public class UserCreatedEvent extends BaseDomainEvent {
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

### 限流

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    @RateLimiter(key = "api", time = 1, timeUnit = TimeUnit.MINUTES, count = 100)
    @GetMapping("/data")
    public Result<Data> getData() {
        // 限制每分钟最多 100 次请求
        return Result.ok(dataService.getData());
    }
}
```

### 防重复提交

```java
@RestController
@RequestMapping("/api")
public class OrderController {
    
    @RepeatSubmit(interval = 5, timeUnit = TimeUnit.SECONDS, message = "请勿重复提交")
    @PostMapping("/order")
    public Result<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        // 5 秒内防止重复提交
        return Result.ok(orderService.createOrder(request));
    }
}
```

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
- XSS 防护可配置开关

**改进**：

- 统一领域事件发布逻辑
- 优化 JwtAuthenticationFilter 性能
- 改进 Redis 缓存类型转换异常处理

## 相关链接

- [配置指南](./CONFIGURATION.md)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Security 官方文档](https://spring.io/projects/spring-security)
