# easyorange-user

**Module:** User auth, registration, profile, password management

## OVERVIEW

Authentication (login/logout), registration, profile CRUD, password change/forgot, custom validators.

## STRUCTURE

```
easyorange-user/src/main/java/com/cartethyia/easyorange/user/
├── config/
│   └── properties/      # User-specific @ConfigurationProperties
├── constant/            # UserConstants
├── controller/          # UserController (login, register, profile, password)
├── dto/
│   ├── request/         # LoginRequest, RegisterRequest, ChangePasswordRequest, etc.
│   ├── response/        # LoginResponse
│   └── vo/              # UserVO
├── entity/              # User.java
├── enums/               # LoginType
├── mapper/              # UserMapper
├── service/
│   └── UserService.java
├── service/impl/        # UserServiceImpl
└── validation/          # Custom JSR-303 validators
```

## WHERE TO LOOK

| Task | File | Notes |
|------|------|-------|
| 登录 | `service/impl/UserServiceImpl.java` | BCrypt 校验 + JWT 生成 |
| 注册 | `UserController.register()` | 用户创建 |
| 密码修改 | `ChangePasswordRequest.java` | 旧密码校验 + 新密码 BCrypt |
| 自定义校验 | `validation/` | JSR-303 自定义 ConstraintValidator |

## CONVENTIONS

- API prefix: `/api/user`
- 登录/注册路径在 `security.ignore-paths` 中配置（无需 JWT）
- 密码：BCrypt 编码，绝不返回或记录明文
- 敏感接口使用 `@RateLimiter` 限流

## ANTI-PATTERNS

- **NEVER** 记录密码（`@Log(excludeParamNames = {"password"})`）
- **NEVER** 在 API 响应中返回密码字段

## DEPENDENCIES

```
easyorange-user → easyorange-framework → easyorange-common
```

## COMMANDS

```bash
mvn clean install -pl easyorange-user
```
