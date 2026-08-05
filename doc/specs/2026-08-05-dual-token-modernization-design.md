# 双 Token 现代化改造设计（access/refresh 职责分离）

日期：2026-08-05
状态：已获批（User: "就要这个"）
范围：easyorange-backend（framework / user / admin / application）+ easyorange-frontend

## 1. 目标与决策

把现有"双 JWT + 全 JS 可见"的认证改造为现代化档：**refresh token 从 JS 存储与 JSON body 里彻底移除**，堵住 XSS 窃取长效凭证这条最致命的泄露面。

核心决策（已确认）：

| 主题 | 决策 | 说明 |
|------|------|------|
| refresh 传输 | **HttpOnly Cookie** | `Secure + SameSite=Lax + Path=/api/auth` |
| refresh 形式 | **opaque token 落 Redis** | 32 字节随机 → base64url，存 SHA-256 哈希 |
| 复用检测 | 复用即可能吊销 | 复用 token 且超宽限期 → `revokeAllUserSessions` |
| access 存储 | **仅内存** | 前端 zustand 不持久化，刷新靠 cookie 恢复 |
| 刷新重验 | 查库校验用户存在 + 启用 | 不存在/禁用 → 吊销该用户会话 |
| 改密吊销 | 改密后吊销全部会话 | 直接内联，替代无订阅者的事件 |
| 登出 | 吊销 access(黑名单) + refresh + 清 cookie | logout 改为需认证 |
| CSRF | 自定义头 `X-Client-Type` 防护 | refresh / logout 强制要求 |

不采用：BFF（记录的更高档方案，超出本次范围）。

## 2. 架构总览

Access 与 refresh 彻底分离：

- **Access Token**：保持 JWT（RSA 2048，`type=access`，30 分钟）。密码学验证 + `TokenRevocationFilter`（黑名单 jti / iat 强制下线）。仍在 JSON body 返回（短时，JS 需要）。
- **Refresh Token**：opaque 随机串，存 Redis。`HttpOnly` cookie 传输，JS 完全不可见。吊销/复用检测/按用户吊销全在存储层。

`TokenService` 仍是唯一门面（`AuthAppService`、`AdminUserSecurityService` 调用点不变），内部新增 `RefreshTokenStore` 端口 + Redis 实现。

## 3. Redis 数据模型（3 种 key）

前缀 `eo:user:refresh:`。token 存 SHA-256 哈希（Redis 泄露不直接暴露可用 token）。

| Key | 类型/值 | TTL |
|-----|---------|-----|
| `SESSION:{tokenHash}` | STRING `userId` | 7 天（refresh 生命周期） |
| `USED:{tokenHash}` | STRING `userId:epochMillis`（旋转时间戳） | 10 分钟 |
| `USER:{userId}` | SET of `tokenHash` | 7 天 |

操作语义：

- **create(userId)**（登录）：`SET SESSION:{hash}=userId` + `SADD USER:{userId} {hash}`。
- **rotate(token)**（刷新）：
  - `SESSION` 存在 → `DEL` 旧 + `SET USED:{oldHash}=userId:now` + 生成新 token → `SET SESSION:{new}` + `SADD/延长 USER`。返回新 token + userId。
  - `SESSION` 不存在但 `USED` 存在 → **复用**：
    - USED 年龄 < `REUSE_GRACE_MS`（30 秒）→ 视为多标签页并发（良性），仅 401，不吊销。
    - USED 年龄 ≥ 宽限 → 凭证被盗 → `revokeAllSessions(userId)` → 401。
  - 两者都不在 → 无效/过期 → 401。
- **revoke(token)**（登出）：`SESSION` 在 → `DEL` + `SET USED` + `SREM USER`。
- **revokeAllSessions(userId)**（强制下线 / 改密 / 盗用）：遍历 `USER` 集合 → 逐个 `DEL SESSION` + `SET USED` → `DEL USER`。

复用的宽限期解决多标签页并发刷新误杀：客户端单飞只覆盖单标签页内并发，跨标签页并发由服务端宽限期兜底。

## 4. Cookie 契约（API 变更）

Cookie 名 `eo_refresh_token`，`HttpOnly + Secure + SameSite=Lax`，`Path=/api/auth`（只发给 auth 端点，收窄暴露面），maxAge = 7 天。`Secure` 由配置开关控制（生产 true，本地 http false）。

| 端点 | 变更 |
|------|------|
| `POST /auth/login`、`/auth/sms-login` | **Set-Cookie**；body 只返 `{accessToken, user}`（refresh 不再进 JSON） |
| `POST /auth/refresh` | 从 **Cookie** 读（不再传 body）；返 `{accessToken}` + 新 Set-Cookie；保持公开 |
| `POST /auth/logout` | **改为需认证**；黑名单 access + 吊销 refresh + 清 Cookie |

DTO 变更：
- `LoginResult(accessToken, user)` —— 去掉 `refreshToken`。
- `TokenRefreshResult(accessToken)` —— 去掉 `refreshToken`。
- `UserAssembler.toLoginResult` 去掉 refreshToken 参数。

## 5. 后端改动清单

### 5.1 新增：`RefreshTokenStore`（framework，端口）+ `RefreshTokenStoreImpl`

- 接口方法：`create(userId)` → String；`rotate(token)` → `TokenRotation`；`revoke(token)`；`revokeAllSessions(userId)`。
- `TokenRotation` record：`{userId, newToken}`（rotate 成功时）。
- 复用/无效用 `BusinessException`（`ResultCode.UNAUTHORIZED`）表达；`RefreshTokenStoreImpl` 内部完成宽限判断与吊销。
- token 生成：`SecureRandom` 32 字节 → Base64 URL 无填充；哈希：SHA-256。

### 5.2 修改：`TokenService` / `TokenServiceImpl`

接口改为：
- `createAccessToken(userId, username, authorities)` —— JWT，不变。
- `createRefreshToken(userId)` —— opaque（委托 store.create）。
- `rotateRefreshToken(token)` → `TokenRotation`（委托 store.rotate）。
- `revokeRefreshToken(token)`（委托 store.revoke）。
- `revokeAccessToken(accessToken)`（黑名单 jti，复用现有 `invalidateToken` 逻辑）。
- `revokeAllUserSessions(userId)`（store.revokeAllSessions + access 强制下线标记，替代原 `invalidateAllUserTokens`）。

`TokenServiceImpl` 移除 refresh JWT 的 jti 黑名单逻辑（由 store 接管）；`TokenRevocationFilter` 不变（只管 access JWT）。

### 5.3 Cookie 装配：`RefreshCookie` 组件（framework）

- `write(response, token, maxAgeSeconds, props)`：`ResponseCookie.from(cookieName, token).httpOnly(true).secure(secure).sameSite(sameSite).path(cookiePath).maxAge(maxAge)`。
- `clear(response, props)`：sameSite + maxAge(0)。
- 从 `JwtProperties` 读 cookie 配置。

### 5.4 修改：`JwtProperties`

新增：`refreshCookieName`="eo_refresh_token"、`refreshCookiePath`="/api/auth"、`refreshCookieSecure`=true、`refreshCookieSameSite`="Lax"。

### 5.5 修改：`AuthAppService`

- `login`：创建 access + refresh（refresh 仅供 controller 设 cookie，不进 DTO）。
- `refreshToken(refreshToken)`：
  1. `rotateRefreshToken` → userId + newRefresh（复用/无效 → 401）。
  2. 按 userId 查库：不存在或非启用 `UserStatus` → `revokeAllUserSessions(userId)` + 401。
  3. `createAccessToken`。返回内部记录 `RefreshResult(accessToken, refreshToken)`：refresh 供 controller 设 cookie，access 供 body DTO。

  注：`RefreshResult`（内部，access+refresh 两字段）与 API DTO `TokenRefreshResult(accessToken)` 是两个不同类型，避免混淆。
- `logout(accessToken, refreshToken)`：两者非空则分别吊销 + 清 context。
- `changePassword`：改密成功后 `revokeAllUserSessions(userId)`。

### 5.6 修改：`AuthController`

- login / smsLogin：`RefreshCookie.write(...)`；body 返回 `LoginResult(accessToken, user)`。
- refresh：`@CookieValue(name, required=false)` 读 refresh；缺失 → 401；调用 service；`RefreshCookie.write(新 refresh)`；body 返回 `TokenRefreshResult(accessToken)`。
- logout：需认证；从 SecurityContext 取 access JWT（`((Jwt)auth.getDetails()).getTokenValue()`）+ cookie 读 refresh → `authAppService.logout(...)` → `RefreshCookie.clear(...)`。

### 5.7 新增：`RefreshCsrfFilter`（framework web/filter）

对 POST `/api/auth/refresh`、`/api/auth/logout` 强制要求 `X-Client-Type` 头（前端已全请求携带），缺失 → 403。路径由 `SecurityProperties.csrfProtectedPaths` 配置驱动（Filter + YAML，符合项目惯例）。

### 5.8 修改：`SecurityProperties`

新增 `csrfProtectedPaths`（默认 `["/api/auth/refresh", "/api/auth/logout"]`）。

### 5.9 修改：配置（application.yaml / application-prod.yaml）

- `security.ignore-paths`：**移除 `/api/auth/logout`**（改为需认证）；`refresh` 保持公开。
- `jwt.refresh-cookie-secure`：prod 默认 true；本地 dev 设 false（http）。

### 5.10 修改：`AdminUserSecurityService`

`tokenService.invalidateAllUserTokens(id)` → `revokeAllUserSessions(id)`。

### 5.11 修改：`LoginCacheConstants`

新增 `REFRESH_SESSION_KEY`、`REFRESH_USED_KEY`、`REFRESH_USER_KEY`（前缀 `eo:user:refresh:*`）。

## 6. 前端改动清单

### 6.1 `api/core/request.ts`

- `fetch(url, config)` 加 `credentials: 'include'`（cookie 随请求携带/接收）。
- `PUBLIC_ENDPOINTS`：**移除 `/auth/logout`**（改为需认证）。

### 6.2 `store/authStore.ts`

- 移除 `refreshToken` 字段与 `login` 的 refreshToken 参数。
- **移除 `persist` middleware**（access token 仅内存；user 由 bootstrap 恢复）。

### 6.3 `features/auth/session.ts`

- `refreshAccessToken()`：不再读 store 的 refresh；`POST /auth/refresh`（`skipAuth:true`，cookie 自动带）；响应 `{accessToken}` → 更新 store.token。保留单飞 coordinator。
- `setSession(token, user)`：仅 token + user。
- `logout()`：调用 `/auth/logout`（需认证，Authorization 头 + cookie 自动带）；本地清空。
- `initAuth()` 改为 `restoreSession()`：无 token 时调用 refresh → 成功后 `GET /users/me` 拉用户 → `setSession`；失败 → 清空。`main.tsx` 调用点同步改。

### 6.4 `api/userApi.ts` / 类型

- 移除 `refreshToken()` 方法。
- `LoginResponse` → `{accessToken, user}`；`TokenRefreshResult` → `{accessToken}`。

## 7. 测试计划（TDD）

后端：
- 新增 `RefreshTokenStoreImplTest`：旋转、复用（良性宽限 / 盗用吊销）、无效/过期、按用户吊销、登出。
- 新增/更新 `TokenServiceImplTest`：refresh 委托、access 黑名单、强制下线。
- 更新 `AuthAppServiceTest`：cookie 流程、刷新重验用户、改密吊销、登出。
- 更新 `AdminUserSecurityServiceTest`：`revokeAllUserSessions` 调用。
- 更新 `JwtPropertiesTest`（新 cookie 字段）。
- `RefreshCsrfFilterTest`：缺失头 403、带头放行。

前端：
- 更新 `session` / `authStore` / `userApi` / `LoginPage` 测试（无 refresh 字段、restoreSession、logout 需认证）。

## 8. 风险与边界

- **多标签页并发刷新**：由服务端 30 秒宽限期兜底（不误杀），被盗检测仍有效（长效复用）。
- **access 仅内存**：刷新页面需 rely 于 cookie 恢复会话；`restoreSession()` 失败则回登录页。
- **Secure cookie 本地开发**：dev 配置 `refresh-cookie-secure=false`；生产必须 true。
- **logout 语义变化**：从"公开"改为"需认证"，前端 `PUBLIC_ENDPOINTS` 与后端 ignore-paths 需同步移除，保证 401 处理正确。