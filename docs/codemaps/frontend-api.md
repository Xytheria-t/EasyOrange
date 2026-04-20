# 前端 API 层 Codemap

**Last Updated:** 2026-04-18  
**Entry Points:** 
- [`easyorange-frontend/src/api/core/request.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts)
- [`easyorange-frontend/src/api/index.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/index.ts)

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      API 客户端层                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐  │
│  │ request.ts  │───▶│ requestManager│   │ cache         │  │
│  │             │    │              │    │               │  │
│  │ - request() │    │ - dedupe     │    │ - TTL 5min    │  │
│  │             │    │ - cancel     │    │ - clearCache  │  │
│  └──────┬──────┘    └──────────────┘    └───────────────┘  │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────┐    ┌──────────────┐                       │
│  │ interceptors│    │ errorParser  │                       │
│  │             │    │              │                       │
│  │ - request   │    │ - escapeHtml │                       │
│  │ - response  │    │ - 401 handle │                       │
│  └─────────────┘    └──────────────┘                       │
└─────────────────────────────────────────────────────────────┘
```

## 关键模块

| 模块 | 目的 | 主要导出 | 依赖 |
|------|------|----------|------|
| [`request.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts) | HTTP 请求封装 | `request`, `ApiClientError`, `requestManager` | `storage`, `authSession` |
| [`index.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/index.ts) | API 模块聚合 | 各业务模块 API | `request.ts` |

## 核心功能

### 1. 请求管理 (Request Manager)

```typescript
interface RequestManager {
  // 请求去重
  isDuplicate(key: string): boolean
  
  // 开始跟踪
  startTracking(key: string, controller: AbortController): void
  
  // 停止跟踪
  stopTracking(key: string): void
  
  // 取消请求
  cancel(key: string, reason?: string): void
  
  // 取消所有
  cancelAll(reason?: string): void
  
  // 按模式取消
  cancelByPattern(pattern: RegExp | string, reason?: string): void
}
```

**用途：**
- 防止重复请求（100ms 窗口）
- 支持请求取消
- 超时自动中止

### 2. 缓存机制

```typescript
// 缓存配置
const CACHE_TTL = 5 * 60 * 1000;  // 5 分钟

// 缓存接口
interface CacheItem<T> {
  data: T;
  expireAt: number;
}

// 使用示例
const result = await request('/products', { 
  cache: true,  // 启用 GET 请求缓存
  method: 'GET'
});
```

### 3. 错误处理

```typescript
class ApiClientError extends Error {
  status: ApiCode;      // HTTP 状态码
  details: unknown;     // 错误详情
  message: string;      // 已转义的安全消息
}

// 错误解析流程
parseError(response)
  │
  ├── 尝试解析 JSON body
  ├── 提取 message/msg 字段
  ├── 使用 escapeHtml() 转义
  └── 返回 ApiClientError
```

### 4. 重试机制

```typescript
// 可重试的状态码
isRetryable(status): boolean
  - 0 (网络错误)
  - 408 (请求超时)
  - 429 (请求过多)
  - 500+ (服务器错误)

// 重试策略
retries = 2 (默认)
delay = 1000 * 2^(attempt-1)  // 指数退避
```

## 安全增强

### 1. XSS 防护

**新增 `escapeHtml()` 函数** ([`request.ts:L157-166`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L157-L166))

```typescript
const escapeHtml = (str: string): string => {
  const htmlEscapes: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  };
  return str.replace(/[&<>"']/g, (ch) => htmlEscapes[ch]);
};
```

**用途：**
- 错误消息转义
- 防止后端返回恶意 HTML
- 在 `parseError()` 中自动调用

### 2. 公开端点白名单

**新增 `PUBLIC_ENDPOINTS` 常量** ([`request.ts:L191`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L191))

```typescript
const PUBLIC_ENDPOINTS = new Set([
  '/auth/login',
  '/auth/logout',
  '/users/register'
]);
```

**用途：**
- 标识无需认证的端点
- 避免公开端点触发 401 重定向
- 在 `shouldHandleUnauthorized()` 中使用

### 3. 401 处理优化

```typescript
shouldHandleUnauthorized(endpoint, skipAuth): boolean
  │
  ├── skipAuth = true → 不处理
  ├── endpoint in PUBLIC_ENDPOINTS → 不处理
  └── 其他情况 → 调用 handleUnauthorized()
```

## 请求配置

```typescript
interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  headers?: Record<string, string>;
  body?: unknown;
  params?: Record<string, unknown>;
  timeout?: number;        // 默认 10000ms
  retries?: number;        // 默认 2 次
  cache?: boolean;         // GET 请求缓存
  signal?: AbortSignal;    // 外部取消信号
  dedupe?: boolean;        // 请求去重
  skipAuth?: boolean;      // 跳过认证
}
```

## 拦截器

### 请求拦截器

```typescript
addRequestInterceptor((config) => {
  // 修改请求配置
  config.headers['X-Custom-Header'] = 'value';
  return config;
});
```

### 响应拦截器

```typescript
addResponseInterceptor(async (response) => {
  // 处理响应
  return response;
});
```

## 数据流

```
调用 request(endpoint, options)
    │
    ▼
检查缓存 (GET 请求)
    │
    ▼
请求去重检查
    │
    ▼
应用请求拦截器
    │
    ▼
发送 fetch 请求
    │
    ▼
应用响应拦截器
    │
    ▼
检查响应状态
    │
    ├── 401 → handleUnauthorized()
    ├── 错误 → 重试逻辑
    └── 成功 → 解析 JSON
    │
    ▼
缓存响应 (GET 请求)
    │
    ▼
返回结果
```

## 外部依赖

- `easyorange-shared` - 共享类型定义
- `storage` - localStorage 封装
- `authSession.handleUnauthorized` - 401 处理

## 相关区域

- [认证模块 Codemap](./frontend-auth.md) - authSession.ts
- [共享适配层 Codemap](./shared-adapters.md) - WebAdapter
- [API 规范文档](../api/API_SPEC.md) - 后端接口定义

## 变更历史

### 2026-04-18 更新

1. **escapeHtml 函数** ([`request.ts:L157-166`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L157-L166))
   - 新增：HTML 实体转义工具
   - 用途：XSS 防护
   - 转义字符：`&`, `<`, `>`, `"`, `'`

2. **PUBLIC_ENDPOINTS 常量** ([`request.ts:L191`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L191))
   - 定义：公开端点白名单
   - 端点：`/auth/login`, `/auth/logout`, `/users/register`
   - 用途：避免 401 误处理

3. **shouldHandleUnauthorized 函数** ([`request.ts:L193-200`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts#L193-L200))
   - 新增：401 处理判断逻辑
   - 逻辑：检查 skipAuth 和 PUBLIC_ENDPOINTS
   - 改进：防止公开端点触发重定向
