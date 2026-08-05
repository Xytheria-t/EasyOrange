# easyorange-user 模块指南

用户管理模块，完整 DDD 分层架构，处理认证、注册、密码管理、用户信息。

## 目录结构

```
user/
├── adapter/
│   ├── inbound/web/
│   │   ├── assembler/
│   │   │   └── UserAssembler.java       # Domain ↔ Response DTO 转换（含脱敏、枚举转码）
│   │   ├── controller/
│   │   │   ├── AuthController.java          # 认证端点 (登录/注册/刷新/登出)
│   │   │   └── UserController.java          # 用户信息端点
│   │   ├── dto/request/                 # 入站 DTO（Jakarta Bean Validation）
│   │   │   ├── auth/                    # 认证 + 密码管理
│   │   │   │   ├── PasswordLoginRequest.java
│   │   │   │   ├── SmsLoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   ├── PasswordResetRequest.java
│   │   │   │   └── ChangePasswordRequest.java
│   │   │   └── profile/                 # 用户资料
│   │   │       └── UpdateProfileRequest.java
│   │   ├── dto/response/                # 出站 DTO
│   │   │   ├── UserResponse.java
│   │   │   ├── UserProfileResponse.java
│   │   │   └── LoginResult.java
│   │   └── validation/                  # 自定义校验（纯格式校验，无 I/O 副作用）
│   │       ├── Password.java + PasswordValidator.java
│   │       └── Username.java + UsernameValidator.java
│   └── outbound/
│       ├── persistence/                 # 持久化适配器
│       │   ├── UserDO.java              # 纯数据库实体（不含映射逻辑）
│       │   ├── UserEntityMapper.java    # MapStruct: UserDO ↔ User 聚合根
│       │   ├── UserMapper.java          # MyBatis-Plus Mapper 接口
│       │   └── UserRepositoryImpl.java   # 仓储实现（注入 entityMapper）
│       ├── cache/                       # 缓存适配器
│       │   ├── RedisLoginAttemptAdapter.java
│       │   └── RedisSmsCodeAdapter.java
│       ├── mock/                        # 开发环境模拟适配器
│       │   ├── MockSmsCodeAdapter.java  # 内存验证码存储+限流（无Redis依赖）
│       │   └── MockSmsSenderAdapter.java # 控制台日志发送（不真实发短信）
│       ├── security/                    # 安全适配器
│       │   └── PasswordEncoderAdapter.java
│       └── storage/                     # 存储适配器
│           └── LocalAvatarFileStorage.java
├── application/
│   └── service/                         # 应用服务（薄编排，不碰响应格式）
│       ├── AuthAppService.java          # 认证+密码管理（注册/登录/登出/刷新/重置密码/修改密码）
│       └── ProfileAppService.java       # 用户资料（信息/更新/头像）
├── domain/
│   ├── aggregate/
│   │   └── User.java                    # 用户聚合根
│   ├── exception/
│   │   └── AccountLockedException.java  # 登录锁定异常（含 remainingSeconds，不含 UI 文案）
│   ├── event/                           # 领域事件
│   │   ├── UserEvent.java               # 密封接口（extends DomainEvent）
│   │   ├── UserRegisteredEvent.java     # 注册事件
│   │   ├── UserPasswordChangedEvent.java # 密码变更事件
│   │   ├── UserProfileUpdatedEvent.java # 资料更新事件
│   │   └── UserAvatarChangedEvent.java  # 头像变更事件
│   ├── valueobject/
│   │   ├── AuditInfo.java               # 审计信息 (createTime, updateTime, createBy, updateBy, delFlag, version)
│   │   ├── ContactInfo.java              # 联系方式 (email, phone)
│   │   ├── Credentials.java              # 认证凭据 (username, encodedPassword)
│   │   ├── LoginInfo.java                # 登录轨迹 (loginIp, loginDate, pwdUpdateDate)
│   │   └── PersonalInfo.java             # 个人信息+展示 (record + @With + @Builder)
│   ├── service/                         # 领域服务
│   │   ├── AuthenticationService.java   # 认证 + 密码管理（领域逻辑，持久化在 AuthAppService）
│   │   ├── LoginSecurityService.java
│   │   ├── ProfileUpdateService.java    # 用户资料更新 + 唯一性校验
│   │   └── RegistrationService.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── port/                             # 出站端口
│   │   ├── AvatarFilePort.java
│   │   ├── LoginAttemptPort.java
│   │   ├── PasswordEncoderPort.java
│   │   ├── SmsCodePort.java
│   │   └── SmsSenderPort.java
│   ├── constant/
│   │   ├── UserConstant.java
│   │   └── UserSecurityConstant.java
│   ├── enums/
│   │   ├── UserType.java, UserStatus.java, Sex.java
│   │   ├── LoginMethod.java, ClientType.java
│   │   └── UserResultCode.java
└── config/
    └── UserDomainConfig.java            # Port → Bean 绑定
```

## 核心模式

### 值对象模式

模块内值对象统一使用 `record`：

| 值对象 | 实现方式 | 原因 |
|--------|----------|------|
| `Credentials` | record | 字段少（2个），构造简单 |
| `ContactInfo` | record | 字段少（2个），构造简单 |
| `LoginInfo` | record | 含语义化方法（`recordLogin`, `updatePasswordTime`） |
| `AuditInfo` | record | 含语义化方法（`update`, `markDeleted`） |
| `PersonalInfo` | record + `@With` + `@Builder(toBuilder = true)` | 字段多（5个），用 Lombok 自动生成 `withXxx()` 和 builder |

**PersonalInfo 使用示例**：
```java
// 构建
PersonalInfo info = PersonalInfo.builder()
    .realName("张三")
    .nickName("小张")
    .build();

// 修改（返回新实例）
PersonalInfo updated = info.withNickName("新昵称");

// 空实例
PersonalInfo empty = PersonalInfo.empty();
```

### 对象映射策略

模块内有两层 MapStruct 映射，职责分离：

| Mapper | 方向 | 位置 | 说明 |
|--------|------|------|------|
| `UserEntityMapper` | Entity ↔ Domain | `adapter/outbound/persistence/` | 扁平字段 ↔ 嵌套值对象（record 构造） |
| `UserAssembler` | Domain ↔ Response | `adapter/inbound/web/assembler/` | 聚合根 ↔ 响应 DTO（含脱敏、枚举转码） |

`UserDO` 是纯数据库实体，不含任何 `toDomain()` / `from()` 方法。所有持久化映射逻辑集中在 `UserEntityMapper`。

### 登录策略模式

每种登录方式使用独立的请求 DTO 和 REST 端点，DTO 各自封装自己的 `toCredential()` 方法转换为 `LoginCredential` 密封接口的对应子类型（`Password` / `Sms`）。`AuthAppService.login()` 完成认证 + Token 创建，返回 `LoginContext` (User + Tokens)；Controller 层仅负责调用 `UserAssembler` 组装响应 DTO。

```java
// AuthController - 仅组装 DTO，无应用逻辑
@PostMapping("/login")
public Result<LoginResult> login(@Valid @RequestBody PasswordLoginRequest request) {
    var ctx = authAppService.login(request.toCredential());
    return Result.success(userAssembler.toLoginResult(ctx.user(), ctx.accessToken(), ctx.refreshToken()));
}

// AuthAppService - 认证 + Token 创建 + 登录轨迹记录（完整的应用层编排）
public LoginContext login(LoginCredential credential) {
    User user = authenticationService.authenticate(credential);
    User loggedIn = user.recordLogin(RequestUtil.getClientIp());
    userRepository.update(loggedIn);
    String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), user.getUserType().getDefaultRoles());
    String refreshToken = tokenService.createRefreshToken(user.getId());
    return new LoginContext(user, accessToken, refreshToken);
}
```

### 出站端口隔离

domain 层通过 `port/` 接口与基础设施解耦：
- `PasswordEncoderPort` → `PasswordEncoderAdapter` (BCrypt)
- `LoginAttemptPort` → `RedisLoginAttemptAdapter` (Redis)
- `AvatarFilePort` → `LocalAvatarFileStorage` (本地文件)
- 短信验证码端口：
  - `SmsCodePort`（验证码生成/存储/限流/发送）→ `MockSmsCodeAdapter` (内存) / `RedisSmsCodeAdapter` (Redis，生产)
  - `SmsSenderPort`（实际投递）→ `MockSmsSenderAdapter` (日志，开发) / 第三方短信商（生产）

### 自定义校验注解 (Jakarta Bean Validation)

validation 包仅包含纯格式校验（无 I/O 副作用），遵循 DDD 分层原则：

- **`@Password`** — 密码强度校验（字段级）。规则来自 `UserConstant.PASSWORD_REGEX`（8-128位，最小长度+弱密码黑名单）；弱密码黑名单通过 `application.yaml` 的 `easy-orange.validation.password.weak-list` 配置注入。使用示例: `@Password String password`
- **`@Username`** — 用户名格式校验（字段级）。校验长度（3-50位）和字符集（字母、数字、下划线）。使用示例: `@Username String username`

业务规则校验（如唯一性）在 application / domain 层处理，不在 adapter 层做：
- 注册唯一性 → `RegistrationService.validateUsernameNotExists()` + `validateUniqueContactInfo()`
- 更新唯一性 → `ProfileUpdateService.validateUniqueContact()`（原 `ProfileAppService.checkUnique()` 已下沉至领域层）

## 密码管理

密码操作统一通过 `AuthenticationService`（domain 层）处理。验证身份方式按操作不同：

| 操作 | 路由 | 身份 | 领域方法 |
|------|------|------|---------|
| 发送验证码 | `POST /api/auth/sms-code` | 匿名 | `SmsCodePort.send(phone)` |
| 重置密码（忘记密码） | `POST /api/auth/password/reset` | 匿名 | `resetPassword(phone, verifyCode, newPassword)` |
| 修改密码（已登录） | `PUT /api/auth/password/change` | 登录 | `changePassword(user, oldPassword, newPassword)` |

- 重置密码走 **手机号 + 短信验证码** 验证身份
- 修改密码走 **旧密码** 验证身份（不再依赖 SMS 模块）
- 修改密码后前端登出（清除 token + 重定向登录页），后端不做 token 主动失效
- 仅 admin 端 `PUT /api/admin/users/{id}/reset-password` 保持管理员强制重置（不走短信验证）

## 短信验证码（开发环境）

开发环境使用内存 Mock，**不依赖 Redis**，**不发送真实短信**。启动后调用发送验证码接口会在控制台打印：

```
[MOCK SMS] 验证码发送
  手机号: 138xxxxxxx
  验证码: 482617
  提示:   当前为模拟模式，不会真实发送短信
```

- `MockSmsCodeAdapter` — 基于 `ConcurrentHashMap` 的验证码存储和限流，重启即重置
- `MockSmsSenderAdapter` — 日志输出，不调用第三方 API
- `MockSmsCodeAdapter` 和 `MockSmsSenderAdapter` 均使用 `@Component` + `@ConditionalOnMissingBean` 组件扫描自动注册

## 安全要点

- 密码: BCrypt 加密，禁止明文存储和日志输出
- 登录限流: `@RateLimiter` + Redis 固定窗口（生产） ; 开发环境限流走内存 Mock
- 防重提交: `@RepeatSubmit` 防止重复注册
- Token: Access Token 短期 + Refresh Token 长期，登出加入黑名单

## 常见开发任务

### 添加新用户字段

1. 判断字段归属的值对象（Credentials / ContactInfo / PersonalInfo / LoginInfo / AuditInfo）或是否应留在聚合根（id, userType, status）
2. 在对应值对象中新增字段：
   - **record 值对象**（Credentials / ContactInfo / LoginInfo / AuditInfo）：新增字段 + 紧凑构造器校验 + `withXxx()` 方法
   - **PersonalInfo（record）**：在 record 组件中新增字段 + 紧凑构造器校验（`@With` 和 `@Builder` 自动适配新字段）
3. 创建 Flyway 迁移脚本
4. 更新 `UserDO`（新增字段）
5. 更新 `UserEntityMapper`（toDomain 方向的抽象子映射方法 / from 使用 getter + builder）
6. 更新 `adapter/inbound/web/dto/response/UserResponse` / `UpdateProfileRequest`
7. 更新 `adapter/inbound/web/assembler/UserAssembler`（如需 MapStruct 显式映射）
8. 更新 `User` 聚合根的相关修改方法
9. 添加测试

### 添加新登录方式

1. `LoginMethod` 枚举新增值
2. `LoginCredential` 密封接口新增 record 子类型（含认证所需参数）
3. 新建 `XxxLoginRequest` DTO（独立端点 + `toCredential()`，如 `PasswordLoginRequest`、`SmsLoginRequest`）
4. `AuthController` 添加 `POST /api/auth/xxx-login` 端点（调 authAppService + 组装响应）
5. `AuthAppService.login()` 委托 `AuthenticationService`（无需额外分发逻辑）
6. `AuthenticationService` 添加对应认证逻辑
7. 添加测试

### 添加新 SMS 发送实现（生产环境）

1. 创建类实现 `SmsSenderPort`（如 `AliyunSmsSenderAdapter`），标注 `@Component`
2. `MockSmsSenderAdapter` 的 `@ConditionalOnMissingBean(SmsSenderPort.class)` 会自动跳过日志模拟实现
3. 如需切换验证码存储到 Redis，确保 `RedisSmsCodeAdapter` 的 `@Component` 被扫描到（`MockSmsCodeAdapter` 的 `@ConditionalOnMissingBean(name = "redisSmsCodeAdapter")` 会自动跳过内存实现）

### 添加新领域事件

1. 创建事件 record 实现 `DomainEvent`
2. 在应用服务中通过 `DomainEventPublisher` 发布事件
3. 添加事件监听器（如需，放置在 `easyorange-application/adapter/event/`）
4. 添加测试
