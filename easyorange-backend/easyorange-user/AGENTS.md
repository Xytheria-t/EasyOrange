# easyorange-user 模块指南

用户管理模块，完整 DDD 分层架构，处理认证、注册、密码管理、用户信息。

## 目录结构

```
user/
├── adapter/
│   ├── inbound/web/
│   │   ├── assembler/UserAssembler.java # Domain ↔ Response DTO（含脱敏、枚举转码）
│   │   ├── controller/                  # AuthController（认证/密码端点）/ UserController（用户信息端点）
│   │   ├── dto/request/
│   │   │   ├── auth/                    # PasswordLoginRequest / SmsLoginRequest / RegisterRequest
│   │   │   │                            #   / RefreshTokenRequest / PasswordResetRequest / ChangePasswordRequest
│   │   │   └── profile/UpdateProfileRequest.java
│   │   ├── dto/response/                # UserResponse / UserProfileResponse / LoginResult / CommonUserFields
│   │   └── validation/                  # @Password + PasswordValidator / @Username + UsernameValidator / UserValidationProperties
│   └── outbound/
│       ├── admin/AdminUserManagementAdapter.java
│       ├── cache/                       # RedisLoginAttemptAdapter / RedisSmsCodeAdapter
│       ├── mock/                        # MockSmsCodeAdapter / MockSmsSenderAdapter（dev/test 环境）
│       ├── persistence/                 # UserDO / UserEntityMapper / UserMapper / UserRepositoryImpl
│       ├── query/UserQueryAdapter.java
│       ├── security/PasswordEncoderAdapter.java
│       └── storage/LocalAvatarFileStorage.java
├── application/
│   ├── config/UserDomainConfig.java     # Port → Bean 绑定
│   ├── dto/UserView.java
│   └── service/                         # AuthAppService（认证+会话）/ CredentialAppService（密码）/ ProfileAppService（资料）
└── domain/
    ├── aggregate/                       # User / ContactUpdateSpec / PersonalUpdateSpec
    ├── constant/                        # UserConstant / UserSecurityConstant
    ├── enums/                           # UserType / UserStatus / Sex / UserResultCode
    ├── event/                           # UserEvent（sealed）/ UserRegisteredEvent / UserPasswordChangedEvent
    │                                    #   / UserProfileUpdatedEvent / UserAvatarChangedEvent
    ├── port/                            # AvatarFilePort / LoginAttemptPort / PasswordEncoderPort / SmsCodePort / SmsSenderPort
    │                                    #   / UserQueryPort（ACL）/ AdminUserManagementPort（ACL）
    ├── repository/UserRepository.java
    ├── service/                         # AuthenticationService（登录认证）/ PasswordManagementService（密码生命周期）
    │                                    #   / SmsVerificationService / LoginSecurityService / ProfileUpdateService
    │                                    #   / RegistrationService / AdminUserManagementService
    └── valueobject/                     # AuditInfo / Avatar / ContactInfo / Credentials / LoginCredential
                                         #   / LoginInfo / PersonalInfo
```

## 核心模式

### 登录策略模式

每种登录方式独立请求 DTO + 独立端点，DTO 封装 `toCredential()` 转换为 `LoginCredential`（sealed 接口，子类型 `Password` / `Sms`）。`AuthAppService.login()` 完成认证 + Token 创建 + 登录轨迹记录，返回 `LoginContext(UserView, accessToken, refreshToken)`；Controller 只负责调 `UserAssembler` 组装响应。

```java
// AuthController - 仅组装 DTO，无应用逻辑
@PostMapping("/login")
public Result<LoginResult> login(@Valid @RequestBody PasswordLoginRequest request, HttpServletResponse response) {
    return doLogin(request.toCredential(), response);  // authAppService.login → 写 refresh cookie → userAssembler.toLoginResult
}
```

### 持久化映射

两层 MapStruct 职责分离：`UserEntityMapper`（扁平字段 ↔ 嵌套 record 值对象）承担全部持久化映射，`UserDO` 不含任何 `toDomain()` / `from()` 方法；`UserAssembler` 做聚合根 → 响应 DTO（含脱敏、枚举转码）。

### 出站端口隔离

domain 层通过 `port/` 接口与基础设施解耦：

- `PasswordEncoderPort` → `PasswordEncoderAdapter` (BCrypt)
- `LoginAttemptPort` → `RedisLoginAttemptAdapter` (Redis)
- `AvatarFilePort` → `LocalAvatarFileStorage` (本地文件)
- `SmsCodePort`（验证码生成/存储/限流/发送）→ `MockSmsCodeAdapter` (内存) / `RedisSmsCodeAdapter` (Redis，生产)
- `SmsSenderPort`（实际投递）→ `MockSmsSenderAdapter` (日志，dev/test) / 第三方短信商（生产）
- `UserQueryPort` → `UserQueryAdapter`；`AdminUserManagementPort` → `AdminUserManagementAdapter`（查询经 Mapper，写操作委托 `AdminUserManagementService`）

### 自定义校验注解 (Jakarta Bean Validation)

validation 包仅包含纯格式校验（无 I/O 副作用）：

- **`@Password`** — 密码强度校验（字段级）。规则来自 `UserConstant.PASSWORD_REGEX`（8-128 位，最小长度 + 弱密码黑名单）；弱密码黑名单通过 `easyorange.validation.password.weak-list` 配置注入
- **`@Username`** — 用户名格式校验（字段级）。校验长度（3-50 位）和字符集（字母、数字、下划线）

业务规则校验（如唯一性）在 application / domain 层处理，不在 adapter 层做：

- 注册唯一性 → `RegistrationService.registerNewUser()`（内部校验用户名）
- 更新唯一性 → `ProfileUpdateService.validateUniqueContact(email, phone, studentId, currentUser)`

## 密码管理

密码操作统一由 domain 层 `PasswordManagementService` 承载，经 `CredentialAppService` 暴露。验证身份方式按操作不同：

| 操作 | 路由 | 身份 | 领域方法 |
|------|------|------|---------|
| 发送验证码 | `POST /api/auth/sms-code` | 匿名 | `SmsCodePort.send(phone)`（AuthAppService） |
| 重置密码（忘记密码） | `POST /api/auth/password/reset` | 匿名 | `PasswordManagementService.resetPassword(phone, verifyCode, newPassword)`（短信验证码校验走 `SmsVerificationService`） |
| 修改密码（已登录） | `PUT /api/auth/password/change` | 登录 | `PasswordManagementService.changePassword(user, oldPassword, newPassword)` |

- 重置密码走 **手机号 + 短信验证码** 验证身份；修改密码走 **旧密码** 验证身份
- 修改密码成功后 `CredentialAppService` 吊销该用户全部会话（`TokenService.revokeAllUserSessions`）并发布 `UserPasswordChangedEvent`，前端同时清理本地 token
- 新密码与旧密码相同直接拒绝（`PASSWORD_SAME_AS_OLD`）
- 仅 admin 端 `PUT /api/admin/users/{id}/reset-password` 保持管理员强制重置（不走短信验证）

## 短信验证码（开发环境）

开发环境使用内存 Mock，**不依赖 Redis**，**不发送真实短信**。启动后调用发送验证码接口会在控制台打印：

```
[MOCK SMS] 验证码发送
  手机号: 138xxxxxxx
  验证码: 482617
  提示:   当前为模拟模式，不会真实发送短信
```

- `MockSmsCodeAdapter` — 基于 `ConcurrentHashMap` 的验证码存储和限流，重启即重置；`@Component` + `@ConditionalOnMissingBean(name = "redisSmsCodeAdapter")`，Redis 适配器存在时自动让位
- `MockSmsSenderAdapter` — 日志输出，不调用第三方 API；`@Profile({"dev", "test", "default", "it"})`，生产环境不注册

## 安全要点

- 密码: BCrypt 加密，禁止明文存储和日志输出
- 登录限流/防重: 由全局 `RateLimitFilter` 约定式拦截（写操作 Redis 分布式限流 + 3 秒防重，Redis 不可用 fail-open），无需模块内注解
- 登录失败锁定: `LoginSecurityService`（domain）按 `LoginAttemptPort` 计数判定，超限抛 `BusinessException.of(UserResultCode.USER_LOCKED)`（B1003）；计数存储走 `RedisLoginAttemptAdapter`
- 登出: `AuthAppService.logout` 吊销 access（黑名单）+ refresh，并清刷新 cookie

## 常见开发任务

### 添加新用户字段

1. 判断字段归属的值对象（Credentials / ContactInfo / PersonalInfo / LoginInfo / AuditInfo）或是否应留在聚合根（id, userType, status）
2. 在对应值对象中新增字段：
   - **record 值对象**（Credentials / ContactInfo / LoginInfo / AuditInfo）：新增字段 + 紧凑构造器校验 + `withXxx()` 方法
   - **PersonalInfo（record）**：在 record 组件中新增字段 + 紧凑构造器校验（`@With` 和 `@Builder` 自动适配新字段）
3. 创建 Flyway 迁移脚本
4. 更新 `UserDO` + `UserEntityMapper`（toDomain 子映射 / from 用 getter + builder）
5. 更新响应 DTO / 请求 DTO 与 `UserAssembler`（如需显式映射）
6. 更新 `User` 聚合根的相关修改方法
7. 添加测试

### 添加新登录方式

1. `LoginCredential` 密封接口新增 record 子类型（含认证所需参数）
2. 新建 `XxxLoginRequest` DTO（独立端点 + `toCredential()`，如 `PasswordLoginRequest`、`SmsLoginRequest`）
3. `AuthController` 添加端点（调 authAppService + 组装响应）
4. `AuthenticationService` 添加对应认证逻辑
5. 添加测试

### 添加新 SMS 发送实现（生产环境）

1. 创建类实现 `SmsSenderPort`，标注 `@Component` + `@Profile("prod")`（`MockSmsSenderAdapter` 只激活 dev/test/default/it，两者互斥）
2. 如需切换验证码存储到 Redis，确保 `RedisSmsCodeAdapter`（`@Component("redisSmsCodeAdapter")`）被扫描到，`MockSmsCodeAdapter` 会自动跳过
3. 添加测试

### 添加新领域事件

1. 创建事件 record 实现 `DomainEvent`
2. 在应用服务中通过 `DomainEventPublisher` 发布事件
3. 添加事件监听器（如需，放置在 `easyorange-application/adapter/event/`）
4. 添加测试
