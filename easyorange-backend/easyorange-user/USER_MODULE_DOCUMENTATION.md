# EasyOrange 用户模块 (easyorange-user) 详细说明文档

## 目录

1. [模块概述](#1-模块概述)
2. [技术架构](#2-技术架构)
3. [项目结构](#3-项目结构)
4. [核心功能详解](#4-核心功能详解)
5. [数据模型](#5-数据模型)
6. [API接口文档](#6-api接口文档)
7. [安全机制](#7-安全机制)
8. [缓存策略](#8-缓存策略)
9. [配置说明](#9-配置说明)
10. [依赖关系](#10-依赖关系)

---

## 1. 模块概述

### 1.1 模块定位

`easyorange-user` 是 EasyOrange 校园二手交易平台的核心用户管理模块，负责处理所有与用户相关的业务逻辑，包括：

- 用户注册与登录认证
- 用户信息管理
- 密码安全（修改密码、忘记密码）
- 账户安全保护

### 1.2 设计原则

- **安全性**：采用 JWT 无状态认证、密码加密存储、登录失败锁定机制
- **可扩展性**：基于 Spring Boot 分层架构，便于功能扩展
- **高可用性**：Redis 缓存加速、限流保护、防重复提交

---

## 2. 技术架构

### 2.1 技术栈

| 技术/框架 | 版本 | 用途 |
|-----------|------|------|
| Java | 25 | 编程语言 |
| Spring Boot | 4.0.3 | 应用框架 |
| MyBatis-Plus | 3.5.x | ORM 框架 |
| Spring Security | 6.x | 安全框架 |
| JWT | 0.12.x | 令牌认证 |
| Redis | - | 缓存与限流 |
| Maven | - | 构建工具 |

### 2.2 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller 层                          │
│              (REST API 接口，参数校验)                        │
├─────────────────────────────────────────────────────────────┤
│                      Service 层                             │
│              (业务逻辑，事务管理)                             │
├─────────────────────────────────────────────────────────────┤
│                      Mapper 层                              │
│              (数据访问，MyBatis-Plus)                        │
├─────────────────────────────────────────────────────────────┤
│                      Entity 层                              │
│              (数据实体，与数据库表映射)                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 项目结构

```
easyorange-user/
├── pom.xml                                    # Maven 配置文件
├── src/
│   └── main/
│       ├── java/
│       │   └── com/cartethyia/easyorange/user/
│       │       ├── constant/                  # 常量定义
│       │       │   └── UserConstants.java
│       │       ├── controller/                # 控制器层
│       │       │   └── UserController.java
│       │       ├── dto/                       # 数据传输对象
│       │       │   ├── request/               # 请求 DTO
│       │       │   │   ├── ChangePasswordRequest.java
│       │       │   │   ├── ForgotPasswordRequest.java
│       │       │   │   ├── LoginRequest.java
│       │       │   │   ├── RegisterRequest.java
│       │       │   │   └── UpdateUserRequest.java
│       │       │   ├── response/              # 响应 DTO
│       │       │   │   └── LoginResponse.java
│       │       │   └── vo/                    # 视图对象
│       │       │       └── UserVO.java
│       │       ├── entity/                    # 实体类
│       │       │   └── User.java
│       │       ├── enums/                     # 枚举类
│       │       │   └── UserStatus.java, UserType.java, LoginType.java
│       │       ├── mapper/                    # 数据访问层
│       │       │   └── UserMapper.java
│       │       ├── service/                   # 服务层
│       │       │   ├── UserService.java
│       │       │   └── impl/
│       │       │       └── UserServiceImpl.java
│       │       └── validation/                # 自定义校验
│       │           ├── Unique.java
│       │           └── UniqueFieldValidator.java
│       └── resources/
│           └── application.yaml               # 应用配置文件
```

---

## 4. 核心功能详解

### 4.1 用户注册

#### 功能描述

用户通过填写用户名、密码、手机号完成注册。

#### 注册流程

```
1. 客户端提交注册请求
   ↓
2. 参数校验（JSR-303）
   - 用户名格式、唯一性
   - 密码强度
   - 手机号格式、唯一性
   ↓
3. 密码加密（BCrypt）
   ↓
4. 保存用户信息到数据库
   ↓
5. 返回注册成功
```

#### 代码实现

**RegisterRequest.java**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Unique(field = "username", message = "用户名已存在")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,20}$",
             message = "密码必须包含大小写字母和数字")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Unique(field = "phone", message = "手机号已被注册")
    private String phone;
}
```

**注册约束**

| 字段 | 约束条件 |
|------|----------|
| 用户名 | 3-20位，字母/数字/下划线，唯一 |
| 密码 | 6-20位，必须包含大小写字母和数字 |
| 手机号 | 11位，1开头，唯一，必填 |

### 4.2 用户登录

#### 功能描述

使用用户名作为登录账号进行登录认证，采用 JWT 无状态认证。

> **说明**：登录账号即用户名，用户名即登录账号。

#### 登录流程

```
1. 客户端提交登录请求（使用用户名作为登录账号）
   ↓
2. 检查登录失败次数（Redis）
   - 超过5次则锁定30分钟
   ↓
3. 根据用户名查找用户
   ↓
4. 密码比对（BCrypt）
   ↓
5. 验证账户状态
   - 0: 正常
   - 1: 禁用
   - 2: 锁定
   ↓
6. 生成 JWT Token
   ↓
7. 更新登录时间
   ↓
8. 清除失败记录
   ↓
9. 返回 Token 和用户信息
```

#### 登录安全机制

**登录失败锁定**

```java
// 检查登录失败次数
private void checkLoginAttempts(String username) {
    String key = "login:attempts:" + username;
    Object attempts = redisCache.get(key);
    if (attempts != null && Integer.parseInt(attempts.toString()) >= 5) {
        throw BusinessException.of("登录失败次数过多，账户已锁定30分钟");
    }
}

// 记录失败次数（使用 Redis INCR 原子操作）
private void recordFailedAttempt(String username) {
    String key = "login:attempts:" + username;
    Long count = redisCache.increment(key);
    if (count != null && count == 1) {
        redisCache.expire(key, 30, TimeUnit.MINUTES);
    }
}
```

**JWT Token 生成**

```java
String token = jwtUtil.generateToken(user.getId());
```

Token 包含用户ID，有效期可配置。

### 4.3 用户信息管理

#### 获取用户信息

```java
@GetMapping("/info")
public Result<UserVO> getUserInfo() {
    UserVO userVO = userService.getUserInfo();
    return Result.success(userVO);
}
```

**缓存策略**

- 优先从 Redis 获取用户信息
- 缓存不存在则查询数据库并写入缓存
- 缓存有效期：30分钟

#### 更新用户信息

```java
@PutMapping("/info")
public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
    UserVO userVO = userService.updateUserInfo(request);
    return Result.success(userVO);
}
```

**可更新字段**

- 昵称
- 手机号（需验证唯一性）
- 性别

### 4.4 密码管理

#### 修改密码

```
1. 验证旧密码
2. 验证新密码强度
3. 更新密码（加密存储）
4. 强制登出（清除缓存）
```

**限流保护**

```java
@RateLimiter(key = "change_password", time = 3600, count = 5, limitType = LimitType.USER)
```

每小时最多修改5次密码。

#### 忘记密码

```
1. 验证手机号格式
2. 查找用户
3. 更新密码
```

---

## 5. 数据模型

### 5.1 用户实体 (User)

**数据库表**: `sys_user`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| user_id | BIGINT | 主键，自增 |
| user_name | VARCHAR(30) | 用户名，唯一 |
| password | VARCHAR(100) | 密码（加密） |
| nick_name | VARCHAR(30) | 昵称 |
| user_type | VARCHAR(2) | 用户类型（01普通用户） |
| phonenumber | VARCHAR(11) | 手机号（唯一） |
| sex | CHAR(1) | 性别（0女 1男 2未知） |
| status | CHAR(1) | 状态（0正常 1禁用 2锁定） |
| del_flag | CHAR(1) | 删除标志（0正常 1删除） |
| login_ip | VARCHAR(128) | 最后登录IP |
| login_date | DATETIME | 最后登录时间 |
| pwd_update_date | DATETIME | 密码更新时间 |
| create_by | VARCHAR(64) | 创建者 |
| create_time | DATETIME | 创建时间 |
| update_by | VARCHAR(64) | 更新者 |
| update_time | DATETIME | 更新时间 |
| remark | VARCHAR(500) | 备注 |

**实体类定义**

```java
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@TableName("sys_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO, value = "user_id")
    private Long id;

    @TableField("user_name")
    private String username;

    @JsonIgnore
    private String password;

    @TableField("nick_name")
    private String nickname;

    @TableField("user_type")
    private String userType;

    @TableField("phonenumber")
    private String phone;

    private String sex;

    private String status;

    @TableField("del_flag")
    private String delFlag;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_date")
    private LocalDateTime loginDate;

    @TableField("pwd_update_date")
    private LocalDateTime pwdUpdateDate;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    private String remark;
}
```

### 5.2 枚举定义

**用户状态 (UserStatus)**

```java
public enum UserStatus {
    NORMAL("0", "正常"),
    DISABLED("1", "禁用"),
    LOCKED("2", "锁定");
}
```

**用户类型 (UserType)**

```java
public enum UserType {
    NORMAL("01", "普通用户");
}
```

**登录类型 (LoginType)**

> 注：当前版本仅支持用户名登录，登录账号即用户名。

```java
public enum LoginType {
    USERNAME(1, "用户名登录");
}
```

---

## 6. API接口文档

### 6.1 接口概览

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户注册 | POST | /api/user/register | 新用户注册 |
| 用户登录 | POST | /api/user/login | 用户登录 |
| 用户登出 | POST | /api/user/logout | 用户登出 |
| 获取信息 | GET | /api/user/info | 获取当前用户信息 |
| 更新信息 | PUT | /api/user/info | 更新用户信息 |
| 修改密码 | POST | /api/user/changePassword | 修改密码 |
| 忘记密码 | POST | /api/user/forgotPassword | 重置密码 |

### 6.2 接口详情

#### 1. 用户注册

```
POST /user/api/v1/api/user/register
Content-Type: application/json

Request:
{
    "username": "zhangsan",
    "password": "Abc123456",
    "phone": "13800138000"
}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

**注解说明**

```java
@PostMapping("/register")
@Log(title = "用户注册", type = BusinessType.ADD, 
     excludeParamNames = {"password", "confirmPassword"})  // 操作日志，排除敏感字段
@RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")  // 防重复提交
```

#### 2. 用户登录

```
POST /user/api/v1/api/user/login
Content-Type: application/json

Request:
{
    "username": "zhangsan",
    "password": "Abc123456"
}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIs...",
        "user": {
            "id": 1,
            "username": "zhangsan",
            "nickname": null,
            "phone": "13800138000"
        }
    }
}
```

**限流配置**

```java
@RateLimiter(key = "login", count = 10, limitType = LimitType.IP)
```

每个 IP 每分钟最多登录 10 次。

#### 3. 获取用户信息

```
GET /user/api/v1/api/user/info
Authorization: Bearer {token}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": 1,
        "username": "zhangsan",
        "nickname": null,
        "phone": "13800138000",
        "status": 0,
        "createTime": "2026-03-31 10:00:00",
        "updateTime": "2026-03-31 10:00:00"
    }
}
```

#### 4. 更新用户信息

```
PUT /user/api/v1/api/user/info
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "email": "newemail@example.com",
    "phone": "13900139000",
    "avatar": "https://example.com/avatar.jpg"
}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        // 更新后的用户信息
    }
}
```

#### 5. 修改密码

```
POST /user/api/v1/api/user/changePassword
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "oldPassword": "Abc123456",
    "newPassword": "Xyz789012"
}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

**限流配置**

```java
@RateLimiter(key = "change_password", time = 3600, count = 5, limitType = LimitType.USER)
```

每小时最多修改 5 次密码。

#### 6. 忘记密码

```
POST /user/api/v1/api/user/forgotPassword
Content-Type: application/json

Request:
{
    "phone": "13800138000",
    "newPassword": "NewPass123"
}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

**限流配置**

```java
@RateLimiter(key = "forgot_password", time = 3600, count = 3, limitType = LimitType.IP)
```

每小时最多重置 3 次密码。

---

## 7. 安全机制

### 7.1 密码安全

**加密算法**: BCrypt

```java
@Autowired
private PasswordEncoder passwordEncoder;

// 加密
String encodedPassword = passwordEncoder.encode(plainPassword);

// 验证
boolean matches = passwordEncoder.matches(plainPassword, encodedPassword);
```

**密码强度要求**

- 长度：6-20位
- 必须包含：大写字母、小写字母、数字

### 7.2 认证机制

**JWT 认证流程**

```
1. 用户登录成功 → 生成 JWT Token
2. 客户端存储 Token（LocalStorage/Cookie）
3. 后续请求携带 Token（Authorization: Bearer {token}）
4. 服务端验证 Token 有效性
5. 从 Token 解析用户ID
```

**Token 结构**

```
Header.Payload.Signature

Header: { "alg": "HS256", "typ": "JWT" }
Payload: { "sub": "userId", "iat": 1234567890, "exp": 1234571490 }
Signature: HMACSHA256(base64Url(header) + "." + base64Url(payload), secret)
```

### 7.3 登录保护

**失败次数锁定**

| 失败次数 | 处理 |
|----------|------|
| 1-4次 | 记录失败次数 |
| 5次 | 锁定账户30分钟 |

**实现原理**

使用 Redis 原子操作 `INCR` 记录失败次数，避免并发问题。

### 7.4 限流保护

| 接口 | 限流策略 |
|------|----------|
| 登录 | IP 限流，10次/分钟 |
| 修改密码 | 用户限流，5次/小时 |
| 忘记密码 | IP 限流，3次/小时 |

### 7.5 防重复提交

```java
@RepeatSubmit(interval = 5000, message = "请勿重复提交")
```

基于 Token + 请求路径生成唯一标识，Redis 存储防止重复提交。

---

## 8. 缓存策略

### 8.1 缓存键规范

| 缓存类型 | Key 格式 | 有效期 |
|----------|----------|--------|
| 用户信息 | `user:info:{userId}` | 30分钟 |
| 登录失败次数 | `login:attempts:{account}` | 30分钟 |

### 8.2 缓存操作

**获取用户信息**

```java
String cacheKey = CacheConstants.USER_INFO_KEY + userId;
UserVO cachedUser = redisCache.get(cacheKey);
if (cachedUser != null) {
    return cachedUser;
}
// 查询数据库并缓存
```

**清除缓存**

```java
private void clearUserCache(Long userId) {
    redisCache.delete(CacheConstants.USER_INFO_KEY + userId);
}
```

**缓存更新时机**

- 用户信息更新时清除缓存
- 密码修改后清除缓存
- 登出时清除缓存

---

## 9. 配置说明

### 9.1 应用配置 (application.yaml)

```yaml
spring:
  application:
    name: user

server:
  servlet:
    context-path: /user/api/v1
```

### 9.2 常量配置 (UserConstants.java)

```java
public final class UserConstants {
    // 用户名长度限制
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 20;

    // 密码长度限制
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 20;

    // 手机号正则
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    // 登录失败锁定配置
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_LOCK_MINUTES = 30;
}
```

---

## 10. 依赖关系

### 10.1 Maven 依赖

```xml
<dependencies>
    <!-- Framework 模块 -->
    <dependency>
        <groupId>com.cartethyia</groupId>
        <artifactId>easyorange-framework</artifactId>
    </dependency>

    <!-- Common 模块 -->
    <dependency>
        <groupId>com.cartethyia</groupId>
        <artifactId>easyorange-common</artifactId>
    </dependency>
</dependencies>
```

### 10.2 依赖模块说明

| 模块 | 功能 |
|------|------|
| easyorange-framework | JWT 工具、安全配置、Web 配置 |
| easyorange-common | 统一响应、异常处理、Redis 工具、日志注解、限流注解 |

### 10.3 外部依赖

| 依赖 | 用途 |
|------|------|
| Spring Security | 密码加密、安全上下文 |
| MyBatis-Plus | ORM 框架 |
| Redis | 缓存、限流、会话 |
| JWT | 令牌生成与验证 |
| Validation | 参数校验 |

---

## 11. 自定义校验注解

### 11.1 @Unique - 唯一性校验

**用途**: 校验字段值在数据库中是否已存在

**使用示例**

```java
@Unique(field = "username", message = "用户名已存在")
private String username;
```

**实现原理**

```java
@Component
public class UniqueFieldValidator implements ConstraintValidator<Unique, Object> {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 查询数据库检查唯一性
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, value);
        return userMapper.selectCount(wrapper) == 0;
    }
}
```

---

## 12. 异常处理

### 12.1 业务异常

```java
throw BusinessException.of("用户不存在");
throw BusinessException.of("密码错误");
throw BusinessException.of("账户已被禁用");
```

### 12.2 校验异常

JSR-303 校验失败自动返回错误信息：

```json
{
    "code": 400,
    "message": "参数校验失败",
    "data": {
        "username": "用户名不能为空",
        "password": "密码长度必须在 6-20 位之间"
    }
}
```

---

## 13. 日志记录

### 13.1 操作日志

使用 `@Log` 注解记录操作日志：

```java
@Log(title = "用户注册", type = BusinessType.ADD, 
     excludeParamNames = {"password", "confirmPassword"})
```

**日志内容**

- 操作标题
- 业务类型（ADD/UPDATE/DELETE/OTHER）
- 请求参数（可排除敏感字段）
- 操作人
- 操作时间
- 执行时长

---

## 14. 扩展建议

### 14.1 待实现功能

1. **短信服务集成**
   - 接入阿里云短信服务

2. **第三方登录**
   - 微信登录
   - QQ登录
   - 支付宝登录

3. **用户权限**
   - 角色管理
   - 权限控制

4. **安全增强**
   - 登录设备管理
   - 异地登录提醒
   - 操作日志审计

### 14.2 性能优化

1. **数据库优化**
   - 添加索引（username, email, phone）
   - 读写分离

2. **缓存优化**
   - 本地缓存（Caffeine）
   - 多级缓存

---

## 15. 总结

EasyOrange 用户模块采用现代化的技术栈和架构设计，提供了完整的用户管理功能：

- **注册登录**：手机号登录，JWT 无状态认证
- **安全管理**：密码加密、登录锁定、限流保护
- **信息管理**：用户信息查询与更新
- **密码管理**：修改密码、忘记密码

模块设计遵循 Spring Boot 最佳实践，代码结构清晰，易于维护和扩展。
