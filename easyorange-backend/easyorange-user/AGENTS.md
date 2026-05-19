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
│   │   │   ├── auth/                    # 认证相关
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── RefreshTokenRequest.java
│   │   │   ├── password/                # 密码管理
│   │   │   │   ├── ForgotPasswordRequest.java
│   │   │   │   └── ChangePasswordRequest.java
│   │   │   └── profile/                 # 用户资料
│   │   │       └── UpdateUserRequest.java
│   │   ├── dto/response/                # 出站 DTO
│   │   │   ├── LoginResponse.java
│   │   │   ├── UserResponse.java
│   │   │   └── UserProfileResponse.java
│   │   └── validation/                  # 自定义校验（纯格式校验，无 I/O 副作用）
│   │       ├── Password.java + PasswordValidator.java
│   │       ├── Phone.java + PhoneValidator.java
│   │       └── Username.java + UsernameValidator.java
│   └── outbound/
│       ├── persistence/                 # 持久化适配器
│       │   ├── UserEntity.java          # 纯数据库实体（不含映射逻辑）
│       │   ├── UserEntityMapper.java    # MapStruct: UserEntity ↔ User 聚合根
│       │   ├── UserMapper.java          # MyBatis-Plus Mapper 接口
│       │   └── UserRepositoryImpl.java   # 仓储实现（注入 entityMapper）
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
│   ├── dto/                             # 应用层 DTO（assembler/service/controller 共用）
│   │   ├── UserResponse.java
│   │   └── UserProfileResponse.java
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
│   │   ├── AuditInfo.java               # 审计信息 (createTime, updateTime, createBy, updateBy, delFlag, version)
│   │   ├── ContactInfo.java              # 联系方式 (email, phone)
│   │   ├── Credentials.java              # 认证凭据 (username, encodedPassword)
│   │   ├── LoginInfo.java                # 登录轨迹 (loginIp, loginDate, pwdUpdateDate)
│   │   └── PersonalInfo.java             # 个人信息+展示 (realName, nickName, sex, studentId, avatar)
│   ├── event/
│   │   ├── UserRegisteredEvent.java
│   │   ├── PasswordChangedEvent.java
│   │   └── ForgotPasswordEvent.java
│   ├── service/                         # 领域服务
│   │   ├── AuthenticationDomainService.java
│   │   ├── LoginSecurityDomainService.java
│   │   ├── SmsCodeDomainService.java
│   │   └── UserRegistrationDomainService.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── port/output/                     # 出站端口
│   │   ├── OutboundPort.java            # 端口标记接口
│   │   ├── AvatarFilePort.java
│   │   ├── LoginAttemptPort.java
│   │   ├── NicknameGenerationPort.java
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

### 对象映射策略

模块内有两层 MapStruct 映射，职责分离：

| Mapper | 方向 | 位置 | 说明 |
|--------|------|------|------|
| `UserEntityMapper` | Entity ↔ Domain | `adapter/outbound/persistence/` | 扁平字段 ↔ 嵌套值对象（record 构造） |
| `UserAssembler` | Domain ↔ Response | `application/assembler/` | 聚合根 ↔ 应用层 DTO（含脱敏、枚举转码） |

`UserEntity` 是纯数据库实体，不含任何 `toDomain()` / `from()` 方法。所有持久化映射逻辑集中在 `UserEntityMapper`。

### 登录策略模式

`LoginMethod` 枚举定义登录方式（用户名/手机号），`AuthenticationDomainService` 根据策略分发认证逻辑。

### 领域事件发布

应用服务注入 `UserEventPort`（domain port），调用 `publishUserRegistered()` / `publishPasswordChanged()` / `publishForgotPassword()` 发布事件。Adapter 层 `UserEventPublisher` 委派给框架的 `DomainEventPublisher`，同步发布到 Spring EventBus。

### 出站端口隔离

domain 层通过 `port/output/` 接口与基础设施解耦：
- `PasswordEncoderPort` → `PasswordEncoderAdapter` (BCrypt)
- `LoginAttemptPort` → `RedisLoginAttemptAdapter` (Redis)
- `AvatarFilePort` → `LocalAvatarFileStorage` (本地文件)
- `NicknameGenerationPort` → `NicknameGenerator` (随机昵称生成)
- `UserEventPort` → `UserEventPublisher` (Spring Events)

### 自定义校验注解 (Jakarta Bean Validation)

validation 包仅包含纯格式校验（无 I/O 副作用），遵循 DDD 分层原则：

- **`@Password`** — 密码强度校验（字段级）。规则来自 `UserConstant.PASSWORD_REGEX`（8-128位，含大小写+数字+特殊字符）；弱密码黑名单通过 `application.yaml` 的 `easy-orange.validation.password.weak-list` 配置注入。使用示例: `@Password String password`
- **`@Phone`** — 手机号格式校验（字段级）。正则来自 `UserConstant.PHONE_REGEX`，支持自定义 `regexp` 参数。使用示例: `@Phone String phone`
- **`@Username`** — 用户名格式校验（字段级）。校验长度（3-50位）和字符集（字母、数字、下划线）。使用示例: `@Username String username`

业务规则校验（如唯一性）在 application / domain 层处理，不在 adapter 层做：
- 注册唯一性 → `UserRegistrationDomainService.validateUsernameNotExists()` + `validateUniqueContactInfo()`
- 更新唯一性 → `UserAppService.validateUniqueFieldsIfChanged()`

## 安全要点

- 密码: BCrypt 加密，禁止明文存储和日志输出
- 登录限流: `@RateLimiter` + Redis 滑动窗口
- 防重提交: `@RepeatSubmit` 防止重复注册
- Token: Access Token 短期 + Refresh Token 长期，登出加入黑名单

## 常见开发任务

### 添加新用户字段

1. 判断字段归属的值对象（Credentials / ContactInfo / PersonalInfo / LoginInfo / AuditInfo）或是否应留在聚合根（id, userType, status）
2. 在对应值对象 record 中新增字段 + 紧凑构造器校验 + `withXxx()` 方法
3. 创建 Flyway 迁移脚本
4. 更新 `UserEntity`（新增字段）
5. 更新 `UserEntityMapper`（toDomain/from 的 @Mapping 或 default 方法）
6. 更新 `application/dto/UserResponse` / `UpdateUserRequest`
7. 更新 `UserAssembler`（如需 MapStruct 显式映射）
8. 更新 `User` 聚合根的相关修改方法
9. 添加测试

### 添加新登录方式

1. `LoginMethod` 枚举新增值
2. `AuthenticationDomainService` 添加对应认证逻辑
3. `LoginRequest` 添加字段
4. 添加测试

### 添加新领域事件

1. 创建事件类继承 `BaseDomainEvent`
2. 在 `UserEventPort` 接口中添加对应 publish 方法
3. 在 `UserEventPublisher` 中实现该方法的委派逻辑
4. 在应用服务中调用 Port 方法发布事件
5. 添加事件监听器（如需）
6. 添加测试
