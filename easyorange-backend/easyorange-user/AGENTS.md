# easyorange-user 模块指南

用户管理模块，完整 DDD 分层架构，处理认证、注册、密码管理、用户信息。

## 目录结构

```
user/
├── adapter/
│   ├── inbound/web/
│   │   ├── AuthController.java          # 认证端点 (登录/注册/刷新/登出)
│   │   ├── UserController.java          # 用户信息端点
│   │   ├── dto/request/                 # 入站 DTO
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── RefreshTokenRequest.java
│   │   │   ├── ForgotPasswordRequest.java
│   │   │   ├── ChangePasswordRequest.java
│   │   │   └── UpdateUserRequest.java
│   │   ├── dto/response/                # 出站 DTO
│   │   │   ├── LoginResponse.java
│   │   │   ├── UserVO.java
│   │   │   └── UserProfileVO.java
│   │   └── validation/                  # 自定义校验
│   │       ├── Password.java + PasswordValidator.java
│   │       └── Unique.java + UniqueFieldValidator.java
│   └── outbound/
│       ├── persistence/                 # 持久化适配器
│       │   ├── UserEntity.java
│       │   ├── UserMapper.java
│       │   └── UserRepositoryImpl.java
│       ├── cache/                       # 缓存适配器
│       │   ├── RedisLoginAttemptAdapter.java
│       │   └── RedisSmsCodeAdapter.java
│       ├── messaging/                   # 消息适配器
│       │   └── UserEventPublisher.java
│       ├── security/                    # 安全适配器
│       │   └── PasswordEncoderAdapter.java
│       └── storage/                     # 存储适配器
│           └── LocalAvatarFileStorage.java
├── application/
│   ├── service/                         # 应用服务
│   │   ├── UserLoginAppService.java
│   │   ├── UserRegistrationAppService.java
│   │   ├── UserAppService.java
│   │   ├── PasswordResetAppService.java
│   │   └── SmsCodeAppService.java
│   └── assembler/
│       └── UserAssembler.java           # DTO 组装
├── domain/
│   ├── aggregate/
│   │   └── User.java                    # 用户聚合根
│   ├── valueobject/
│   │   ├── AuditInfo.java
│   │   ├── LoginInfo.java
│   │   └── UserProfile.java
│   ├── event/
│   │   ├── UserRegisteredEvent.java
│   │   ├── PasswordChangedEvent.java
│   │   └── ForgotPasswordEvent.java
│   ├── service/                         # 领域服务
│   │   ├── AuthenticationDomainService.java
│   │   ├── LoginSecurityDomainService.java
│   │   ├── PasswordDomainService.java
│   │   ├── SmsCodeDomainService.java
│   │   └── UserRegistrationDomainService.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── port/output/                     # 出站端口
│   │   ├── OutboundPort.java            # 端口标记接口
│   │   ├── AvatarFilePort.java
│   │   ├── LoginAttemptPort.java
│   │   ├── PasswordEncoderPort.java
│   │   ├── SmsCodePort.java
│   │   ├── SmsRateLimitPort.java
│   │   └── UserEventPort.java
│   ├── constants/
│   │   ├── UserConstant.java
│   │   └── UserSecurityConstant.java
│   ├── enums/
│   │   ├── UserType.java, UserStatus.java, Sex.java
│   │   ├── LoginMethod.java, ClientType.java
│   │   └── UserResultCode.java
│   └── exception/
│       └── UserDomainException.java
└── infrastructure/
    ├── config/UserDomainConfig.java
    └── util/NicknameGenerator.java
```

## 核心模式

### 登录策略模式

`LoginMethod` 枚举定义登录方式（用户名/手机号），`AuthenticationDomainService` 根据策略分发认证逻辑。

### 领域事件发布

```java
@PublishEvent(type = "UserRegistered", extractor = "userRegisteredEventExtractor")
@Transactional(rollbackFor = Exception.class)
public Long register(RegisterRequest request) { ... }
```

### 出站端口隔离

domain 层通过 `port/output/` 接口与基础设施解耦：
- `PasswordEncoderPort` → `PasswordEncoderAdapter` (BCrypt)
- `LoginAttemptPort` → `RedisLoginAttemptAdapter` (Redis)
- `AvatarFilePort` → `LocalAvatarFileStorage` (本地文件)
- `UserEventPort` → `UserEventPublisher` (Spring Events)

### 自定义校验注解

- `@Password(minLength=8, requireDigit=true, requireSpecialChar=true)` — 密码强度
- `@Unique(field="username", message="用户名已存在")` — 唯一性校验

## 安全要点

- 密码: BCrypt 加密，禁止明文存储和日志输出
- 登录限流: `@RateLimiter` + Redis 滑动窗口
- 防重提交: `@RepeatSubmit` 防止重复注册
- Token: Access Token 短期 + Refresh Token 长期，登出加入黑名单

## 常见开发任务

### 添加新用户字段

1. `User` 聚合根添加字段
2. 创建 Flyway 迁移脚本
3. 更新 `UserEntity` / `UserVO` / `UpdateUserRequest`
4. 更新 `UserAssembler`
5. 添加测试

### 添加新登录方式

1. `LoginMethod` 枚举新增值
2. `AuthenticationDomainService` 添加对应认证逻辑
3. `LoginRequest` 添加字段
4. 添加测试

### 添加新领域事件

1. 创建事件类继承 `BaseDomainEvent`
2. 创建 EventExtractor 实现
3. 在应用服务方法上标注 `@PublishEvent`
4. 添加事件监听器（如需）
5. 添加测试
