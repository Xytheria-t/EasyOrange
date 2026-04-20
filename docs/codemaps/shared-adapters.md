# 共享适配层 Codemap

**Last Updated:** 2026-04-18  
**Entry Points:** 
- [`easyorange-shared/src/adapters/web-adapter.ts`](file:///d:/Projects/EasyOrange/easyorange-shared/src/adapters/web-adapter.ts)
- [`easyorange-shared/src/api/core/ApiClient.ts`](file:///d:/Projects/EasyOrange/easyorange-shared/src/api/core/ApiClient.ts)

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                    easyorange-shared                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐  │
│  │ ApiClient   │───▶│ HttpClient   │    │ WebAdapter    │  │
│  │             │    │ (interface)  │    │               │  │
│  │ - request() │    │              │    │ - fetch       │  │
│  │ - upload()  │    │ - request()  │    │ - upload()    │  │
│  └─────────────┘    └──────────────┘    └───────────────┘  │
│                            ▲                                │
│                            │ 实现                           │
│                            │                                │
│                     ┌──────────────┐                        │
│                     │ MiniAdapter  │ (未来扩展)              │
│                     │ (小程序适配)  │                        │
│                     └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

## 关键模块

| 模块 | 目的 | 主要导出 | 依赖 |
|------|------|----------|------|
| [`WebAdapter`](file:///d:/Projects/EasyOrange/easyorange-shared/src/adapters/web-adapter.ts) | Web 平台 HTTP 适配器 | `WebAdapter`, `FormFieldValue` | 原生 Fetch API |
| [`HttpClient`](file:///d:/Projects/EasyOrange/easyorange-shared/src/api/core/HttpClient.ts) | HTTP 客户端接口 | `HttpClient`, `HttpResponse`, `RequestConfig` | TypeScript 类型 |
| [`ApiClient`](file:///d:/Projects/EasyOrange/easyorange-shared/src/api/core/ApiClient.ts) | 统一 API 客户端 | `ApiClient`, `initApiClient`, `getClient` | `HttpClient` |

## 核心接口

### HttpClient

```typescript
interface HttpClient {
  request<T>(config: RequestConfig): Promise<HttpResponse<T>>;
  upload<T>(
    url: string,
    filePath: string,
    name: string,
    formData?: Record<string, FormFieldValue>
  ): Promise<HttpResponse<T>>;
  uploadMultiple<T>(
    url: string,
    filePaths: string[],
    name: string,
    formData?: Record<string, FormFieldValue>
  ): Promise<HttpResponse<T>>;
  setToken(token: string): void;
  clearToken(): void;
}
```

### RequestConfig

```typescript
interface RequestConfig {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  data?: unknown;
  headers?: Record<string, string>;
  timeout?: number;
}
```

### HttpResponse

```typescript
interface HttpResponse<T> {
  data: T;
  status: number;
  headers: Record<string, string>;
}
```

## WebAdapter 实现

### 1. 基础请求

```typescript
async request<T>(config: RequestConfig): Promise<HttpResponse<T>> {
  const url = config.url.startsWith('http') 
    ? config.url 
    : `${this.baseUrl}${config.url}`;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-Client-Type': 'web',
    ...config.headers
  };

  if (this.token) {
    headers['Authorization'] = `Bearer ${this.token}`;
  }

  const response = await fetch(url, {
    method: config.method || 'GET',
    headers,
    body: config.data ? JSON.stringify(config.data) : undefined,
    signal: config.timeout ? AbortSignal.timeout(config.timeout) : undefined
  });

  // 401 处理
  if (response.status === 401 && this.onUnauthorized) {
    this.onUnauthorized();
  }

  const data = await response.json() as T;
  return { data, status: response.status, headers: {} };
}
```

### 2. 单文件上传

```typescript
async upload<T>(
  url: string,
  filePath: string,
  name: string,
  formData?: Record<string, FormFieldValue>
): Promise<HttpResponse<T>> {
  const body = new FormData();
  body.append(name, await this.fileToBlob(filePath));

  // 添加额外表单字段
  if (formData) {
    Object.entries(formData).forEach(([key, value]) => {
      if (typeof value === 'string' || 
          typeof value === 'number' || 
          typeof value === 'boolean') {
        body.append(key, String(value));
      }
    });
  }

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'X-Client-Type': 'web',
      ...(this.token && { 
        'Authorization': `Bearer ${this.token}` 
      })
    },
    body
  });

  return { data: await response.json() as T, status: response.status, headers: {} };
}
```

### 3. 多文件上传

```typescript
async uploadMultiple<T>(
  url: string,
  filePaths: string[],
  name: string,
  formData?: Record<string, FormFieldValue>
): Promise<HttpResponse<T>> {
  const body = new FormData();
  
  // 添加多个文件
  for (const filePath of filePaths) {
    body.append(name, await this.fileToBlob(filePath));
  }

  // ... 其余逻辑同 upload()
}
```

## 安全增强

### SSRF 防护

**新增 `FormFieldValue` 类型** ([`web-adapter.ts:L3`](file:///d:/Projects/EasyOrange/easyorange-shared/src/adapters/web-adapter.ts#L3))

```typescript
type FormFieldValue = string | number | boolean;
```

**用途：**
- 限制 FormData 字段值类型
- 防止复杂对象注入
- 类型安全的表单数据

### 文件路径验证

```typescript
private async fileToBlob(filePath: string): Promise<Blob> {
  // SSRF 防护：只允许 blob: 和 data: URL
  if (!filePath.startsWith('blob:') && 
      !filePath.startsWith('data:')) {
    throw new Error('Invalid file path: only blob: and data: URLs are allowed');
  }
  
  const response = await fetch(filePath);
  return response.blob();
}
```

**防护说明：**
- 禁止 `http://` 和 `https://` URL
- 禁止 `file://` 本地文件协议
- 仅允许浏览器 blob URL 和数据 URL
- 防止服务端请求伪造 (SSRF)

### 401 统一处理

```typescript
constructor(baseUrl: string = '', onUnauthorized?: () => void) {
  this.baseUrl = baseUrl.replace(/\/$/, '');
  this.onUnauthorized = onUnauthorized;  // 注入未授权回调
}

// 在每个请求中检查 401
if (response.status === 401 && this.onUnauthorized) {
  this.onUnauthorized();
}
```

## 令牌管理

```typescript
// 设置令牌
setToken(token: string): void {
  this.token = token;
}

// 清除令牌
clearToken(): void {
  this.token = null;
}

// 自动添加到请求头
if (this.token) {
  headers['Authorization'] = `Bearer ${this.token}`;
}
```

## 客户端类型标识

所有请求自动添加 `X-Client-Type` 头：

```typescript
const headers: Record<string, string> = {
  'Content-Type': 'application/json',
  'X-Client-Type': 'web',  // 标识 Web 客户端
  ...config.headers
};
```

**用途：**
- 后端区分客户端类型
- 日志分析和监控
- 差异化处理逻辑

## 数据流

### 请求流程

```
调用 ApiClient.request(config)
    │
    ▼
WebAdapter.request(config)
    │
    ├── 构建完整 URL
    ├── 添加请求头
    ├── 添加 Authorization (如有 token)
    └── 设置超时
    │
    ▼
fetch(url, options)
    │
    ▼
检查 401
    │
    ├── 是 → 调用 onUnauthorized()
    └── 否 → 继续
    │
    ▼
解析 JSON 响应
    │
    ▼
返回 HttpResponse<T>
```

### 上传流程

```
调用 ApiClient.upload(url, file, name, formData)
    │
    ▼
WebAdapter.upload(...)
    │
    ├── 验证文件路径 (SSRF 防护)
    ├── 读取文件为 Blob
    ├── 构建 FormData
    └── 添加额外字段
    │
    ▼
fetch(url, { method: 'POST', body: FormData })
    │
    ▼
检查 401
    │
    ▼
返回响应
```

## 外部依赖

- 原生 Fetch API
- FormData API
- Blob API

## 相关区域

- [前端 API 层 Codemap](./frontend-api.md) - request.ts
- [前端认证模块 Codemap](./frontend-auth.md) - authSession.ts
- [API 规范文档](../api/API_SPEC.md) - 后端接口定义

## 变更历史

### 2026-04-18 更新

1. **FormFieldValue 类型** ([`web-adapter.ts:L3`](file:///d:/Projects/EasyOrange/easyorange-shared/src/adapters/web-adapter.ts#L3))
   - 新增：表单字段值类型定义
   - 类型：`string | number | boolean`
   - 用途：类型安全的 FormData

2. **SSRF 防护** ([`web-adapter.ts:L125-131`](file:///d:/Projects/EasyOrange/easyorange-shared/src/adapters/web-adapter.ts#L125-L131))
   - 新增：`fileToBlob()` 路径验证
   - 允许：`blob:` 和 `data:` URL
   - 禁止：`http:`, `https:`, `file://` 协议
   - 防止：服务端请求伪造攻击

3. **上传方法签名更新**
   - `upload()`: 单文件上传
   - `uploadMultiple()`: 多文件上传
   - 支持：额外表单字段参数
