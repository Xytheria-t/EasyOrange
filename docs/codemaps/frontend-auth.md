# 前端认证模块 Codemap

**Last Updated:** 2026-04-18  
**Entry Points:** 
- [`easyorange-frontend/src/app/authSession.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/authSession.ts)
- [`easyorange-frontend/src/pages/home/auth.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/pages/home/auth.ts)

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端认证层                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐  │
│  │ authSession │───▶│ navigation   │    │ storage       │  │
│  │             │    │              │    │               │  │
│  │ - setSession│    │ - replace()  │    │ - get/set()   │  │
│  │ - clearSess │    │ - navigate() │    │ - remove()    │  │
│  │ - logout    │    └──────────────┘    └───────────────┘  │
│  └──────┬──────┘                                            │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────┐    ┌──────────────┐                       │
│  │ request.ts  │    │ ApiClient    │                       │
│  │             │    │ (shared)     │                       │
│  │ - logout    │───▶│              │                       │
│  │   API call  │    │ - setToken   │                       │
│  └─────────────┘    │ - clearToken │                       │
│                     └──────────────┘                       │
└─────────────────────────────────────────────────────────────┘
```

## 关键模块

| 模块 | 目的 | 主要导出 | 依赖 |
|------|------|----------|------|
| [`authSession.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/authSession.ts) | 认证会话管理 | `setSession`, `clearSession`, `logout`, `handleUnauthorized` | `storage`, `navigation`, `request` |
| [`navigation.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/navigation.ts) | 路由导航 | `navigate`, `replace` | 原生 History API |
| [`storage.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/utils/storage.ts) | 本地存储封装 | `get`, `set`, `remove` | `localStorage` |

## 核心流程

### 1. 登录流程

```
用户提交登录表单
    │
    ▼
调用 POST /api/auth/login
    │
    ▼
后端验证成功，返回 token + user
    │
    ▼
setSession(token, user)
    │
    ├── 存储到 localStorage
    ├── 同步到共享 ApiClient
    └── 触发 auth-session-change 事件
    │
    ▼
检查 redirect 参数
    │
    ▼
有 redirect → 跳转到原始页面
无 redirect → 停留在首页
```

### 2. 登出流程 (已重构)

```
用户点击登出
    │
    ▼
logout() 函数
    │
    ├── 获取当前 token
    │
    ▼
调用 POST /api/auth/logout (带 token)
    │
    ├── 成功 → 继续
    └── 失败 → 忽略 (确保本地状态清除)
    │
    ▼
clearSession('logout')
    │
    ├── 清除 localStorage
    ├── 清除 ApiClient token
    └── 触发 auth-session-change 事件
    │
    ▼
重定向到首页
```

### 3. 401 未授权处理流程

```
API 请求返回 401
    │
    ▼
handleUnauthorized()
    │
    ├── 防止重复处理 (inFlight 标志)
    ├── clearSession('unauthorized')
    └── navigation.replace('home', { redirect: currentPath })
    │
    ▼
用户被重定向到登录页
```

## 数据结构

### AuthSessionDetail

```typescript
interface AuthSessionDetail {
  isAuthenticated: boolean;  // 是否已认证
  token: string | null;      // 访问令牌
  user: AuthSessionUser | null;  // 用户信息
  reason?: AuthSessionClearReason;  // 清除原因
}

type AuthSessionClearReason = 'logout' | 'unauthorized' | 'manual';
```

### 认证事件

```typescript
// 认证状态变化事件
window.dispatchEvent(
  new CustomEvent<AuthSessionDetail>('auth-session-change', { detail })
);
```

## 安全增强

### 1. XSS 防护

- 错误消息使用 [`escapeHtml()`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L157-L166) 函数转义
- 防止后端返回的恶意 HTML 注入

### 2. 登出撤销确认

- 登出时调用后端 `/auth/logout` 接口
- 后端应撤销/黑名单化 token
- 即使 API 失败也清除本地状态（确保用户体验）

### 3. 401 收敛

- 统一处理未授权场景
- 防止重复重定向 (`unauthorizedRedirectInFlight`)
- 自动记录原始路径用于返回

## 外部依赖

- `easyorange-shared` - 共享 ApiClient 和 HttpClient 接口
- `storage` - localStorage 封装
- `navigation` - 路由导航工具

## 相关区域

- [API 层 Codemap](./frontend-api.md) - request.ts 和 API 客户端
- [共享适配层 Codemap](./shared-adapters.md) - WebAdapter 实现
- [E2E 测试 Codemap](./testing-e2e.md) - 认证流程测试

## 变更历史

### 2026-04-18 更新

1. **logout 函数重构** ([`authSession.ts:L102-116`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/authSession.ts#L102-L116))
   - 新增：调用后端 `/auth/logout` API
   - 新增：传递当前 token 用于后端撤销
   - 改进：即使 API 失败也清除本地状态
   - 改进：忽略 API 错误，确保用户体验一致

2. **escapeHtml 函数添加** ([`request.ts:L157-166`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L157-L166))
   - 新增：HTML 实体转义工具
   - 用途：错误消息 XSS 防护
   - 转义字符：`&`, `<`, `>`, `"`, `'`

3. **PUBLIC_ENDPOINTS 常量** ([`request.ts:L191`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L191))
   - 定义：无需认证的公开端点
   - 端点：`/auth/login`, `/auth/logout`, `/users/register`
   - 用途：避免公开端点触发 401 重定向
