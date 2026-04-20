# E2E 测试 Codemap - 认证关键场景

**Last Updated:** 2026-04-18  
**Entry Points:** 
- [`easyorange-frontend/tests/e2e/auth-critical.spec.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/tests/e2e/auth-critical.spec.ts)
- [`easyorange-frontend/playwright.config.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/playwright.config.ts)

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                  Playwright E2E 测试框架                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              auth-critical.spec.ts                    │  │
│  │                                                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │ 场景 1       │  │ 场景 2       │  │ 场景 3       │  │  │
│  │  │ 重定向返回   │  │ 401 收敛     │  │ 登出撤销    │  │  │
│  │  │             │  │             │  │             │  │  │
│  │  │ - 未登录访问 │  │ - Token 过期  │  │ - 清除状态  │  │  │
│  │  │ - 登录返回   │  │ - 清除会话   │  │ - API 调用   │  │  │
│  │  │ - 参数验证   │  │ - 重定向登录 │  │ - 失败容错  │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐  │
│  │ 辅助函数    │    │ Mock API     │    │ 状态管理      │  │
│  │             │    │              │    │               │  │
│  │ - clearAuth │    │ - route.fulfill│  │ - localStorage│  │
│  │ - setAuth   │    │ - 模拟响应    │    │ - 事件触发   │  │
│  └─────────────┘    └──────────────┘    └───────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 测试场景

### 场景 1: 认证重定向返回

**测试文件位置：** [`auth-critical.spec.ts:L59-148`](file:///d:/Projects/EasyOrange/easyorange-frontend/tests/e2e/auth-critical.spec.ts#L59-L148)

**测试目标：**
- 未登录访问受保护页面 → 重定向到登录页
- 登录成功后 → 返回原始受保护页面
- 验证 redirect 参数正确传递

**测试用例：**

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `未登录访问受保护页面应重定向到登录页` | 访问 `/profile.html` 被重定向 | URL 包含 `redirect=/profile.html` |
| `登录成功后应返回原始受保护页面` | 完整登录流程 | 返回 `/profile.html`，token 已保存 |
| `直接访问首页应不带 redirect 参数` | 访问 `/` | URL 无 `redirect` 参数 |

**测试流程：**

```
1. 访问受保护页面 (/profile.html)
   │
   ▼
2. 验证重定向到首页 + redirect 参数
   │
   ▼
3. 打开登录表单
   │
   ▼
4. Mock API 响应 (POST /api/auth/login)
   │
   ▼
5. 填写用户名和密码
   │
   ▼
6. 提交登录表单
   │
   ▼
7. 验证返回到 /profile.html
   │
   ▼
8. 验证 token 已保存
```

**关键代码：**

```typescript
// Mock 登录 API
await page.route('/api/auth/login', async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200,
      message: '登录成功',
      data: {
        token: 'mock-token-' + Date.now(),
        user: { username: 'testuser', nickname: 'Test User' }
      }
    })
  });
});

// 验证重定向参数
const urlObj = new URL(page.url());
expect(urlObj.searchParams.get('redirect')).toBe(protectedPage);
```

### 场景 2: 强制 401 收敛

**测试文件位置：** [`auth-critical.spec.ts:L156-233`](file:///d:/Projects/EasyOrange/easyorange-frontend/tests/e2e/auth-critical.spec.ts#L156-L233)

**测试目标：**
- 模拟后端返回 401（令牌过期）
- 验证系统清除会话
- 验证重定向到登录页

**测试用例：**

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `API 返回 401 时应清除会话并重定向到登录页` | 模拟 401 响应 | token 被清除，重定向发生 |
| `401 处理后应清除本地认证状态` | 验证 localStorage | `token` 和 `user` 均为 null |

**测试流程：**

```
1. 设置认证状态 (mock token)
   │
   ▼
2. 刷新页面应用状态
   │
   ▼
3. Mock API 返回 401
   │
   ▼
4. 触发需要认证的请求
   │
   ▼
5. 等待 401 处理逻辑执行
   │
   ▼
6. 验证 token 已被清除
   │
   ▼
7. 验证重定向到登录页
```

**关键代码：**

```typescript
// 模拟 401 响应
await page.route(/\/api\/.+/, async (route) => {
  if (request.url().includes('/api/') && 
      !request.url().includes('/auth/')) {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 401,
        message: '登录已过期，请重新登录',
        data: null
      })
    });
  }
});

// 验证状态清除
const token = await page.evaluate(() => 
  localStorage.getItem('token')
);
expect(token).toBeNull();
```

### 场景 3: 登出撤销确认

**测试文件位置：** [`auth-critical.spec.ts:L242-359`](file:///d:/Projects/EasyOrange/easyorange-frontend/tests/e2e/auth-critical.spec.ts#L242-L359)

**测试目标：**
- 用户点击登出
- 验证前端状态清除
- 验证后端撤销令牌 API 调用
- 验证重定向到首页

**测试用例：**

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `点击登出应清除认证状态并重定向到首页` | 完整登出流程 | 状态清除，API 调用，重定向 |
| `登出后应重定向到首页` | 从任意页面登出 | URL 变为 `/` |
| `登出时即使 API 失败也应清除本地状态` | API 失败容错 | 本地状态仍清除 |

**测试流程：**

```
1. 设置认证状态 (mock token)
   │
   ▼
2. 刷新页面
   │
   ▼
3. 监听登出 API 请求
   │
   ▼
4. 点击登出按钮
   │
   ▼
5. 验证 API 调用 (Authorization header)
   │
   ▼
6. 验证 localStorage 已清除
   │
   ▼
7. 验证用户菜单隐藏，登录按钮显示
   │
   ▼
8. 验证重定向到首页
```

**关键代码：**

```typescript
// 监听登出 API
let logoutRequestMade = false;
await page.route('/api/auth/logout', async (route) => {
  const request = route.request();
  if (request.method() === 'POST') {
    logoutRequestMade = true;
    // 验证 Authorization header
    const authHeader = request.headers()['authorization'];
    expect(authHeader).toBe(`Bearer ${mockToken}`);
  }
  await route.fulfill({
    status: 200,
    body: JSON.stringify({ code: 200, message: '退出成功' })
  });
});

// 验证状态清除
const token = await page.evaluate(() => 
  localStorage.getItem('token')
);
expect(token).toBeNull();
```

## 辅助函数

### clearAuthState

```typescript
async function clearAuthState(page: any) {
  await page.evaluate(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  });
}
```

**用途：** 清除认证状态，用于测试隔离

### setAuthState

```typescript
async function setAuthState(
  page: any, 
  token: string = 'test-token', 
  user: object = { username: 'testuser' }
) {
  await page.evaluate(({ t, u }) => {
    localStorage.setItem('token', t);
    localStorage.setItem('user', JSON.stringify(u));
  }, { t: token, u: user });
}
```

**用途：** 设置认证状态，模拟已登录

### waitForPageReady

```typescript
async function waitForPageReady(page: any) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle');
}
```

**用途：** 等待页面加载完成

## 测试配置

### Playwright 配置

[`playwright.config.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/playwright.config.ts)

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:5173',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure'
  }
});
```

### 测试运行命令

```bash
# 运行所有 E2E 测试
npm run test:e2e

# 运行认证测试
npm run test:e2e -- auth-critical

# 有头模式调试
npm run test:e2e -- --headed

# 生成测试报告
npm run test:e2e -- --reporter=html
```

## 测试数据

### 测试用户

```typescript
const TEST_USER = {
  username: 'testuser',
  password: 'testpass123'
};
```

### Mock Token

```typescript
const mockToken = 'mock-token-' + Date.now();
```

## 断言模式

### URL 断言

```typescript
// 验证 URL 匹配正则
await expect(page).toHaveURL(
  new RegExp(`^${BASE_URL}/\\?redirect=`)
);

// 验证 URL 参数
const urlObj = new URL(page.url());
expect(urlObj.searchParams.get('redirect')).toBe(protectedPage);
```

### 元素断言

```typescript
// 可见性
await expect(loginBtn).toBeVisible();
await expect(userMenu).not.toBeVisible();

// CSS 类
await expect(authContainer).toHaveClass(/active/);
```

### localStorage 断言

```typescript
const token = await page.evaluate(() => 
  localStorage.getItem('token')
);
expect(token).toBeTruthy();
expect(token).toContain('mock-token-');
```

## 测试覆盖率

### 覆盖的认证流程

- ✅ 未登录访问受保护页面
- ✅ 重定向到登录页
- ✅ 登录成功后返回原页面
- ✅ Token 过期处理 (401)
- ✅ 登出 API 调用
- ✅ 登出状态清除
- ✅ 登出重定向
- ✅ API 失败容错

### 未覆盖的流程

- ⏳ 微信登录流程
- ⏳ Token 刷新流程
- ⏳ 注册流程
- ⏳ 密码重置流程

## 外部依赖

- **Playwright** - E2E 测试框架
- **Vite** - 开发服务器 (端口 5173)
- **Node.js** - 测试运行环境

## 相关区域

- [认证模块 Codemap](./frontend-auth.md) - authSession.ts 实现
- [API 层 Codemap](./frontend-api.md) - request.ts 实现
- [测试规范](../../rules/common/testing.md) - 测试覆盖率要求

## 测试最佳实践

### 1. 测试隔离

```typescript
test.beforeEach(async ({ page }) => {
  // 每个测试前清除认证状态
  await clearAuthState(page);
});

test.afterEach(async ({ page }) => {
  // 测试后清理
  await clearAuthState(page);
});
```

### 2. Mock API 响应

```typescript
await page.route('/api/auth/login', async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ /* mock data */ })
  });
});
```

### 3. 等待策略

```typescript
// 避免使用固定等待
// ❌ await page.waitForTimeout(5000);

// 使用确定性等待
// ✅ await page.waitForURL(expectedUrl);
// ✅ await expect(element).toBeVisible();
```

## 变更历史

### 2026-04-18 新增

1. **认证关键场景 E2E 测试文件** ([`auth-critical.spec.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/tests/e2e/auth-critical.spec.ts))
   - 新增：场景 1 - 认证重定向返回测试
   - 新增：场景 2 - 强制 401 收敛测试
   - 新增：场景 3 - 登出撤销确认测试
   - 新增：辅助函数 - clearAuthState, setAuthState, waitForPageReady
   - 新增：Mock API 响应模式
   - 测试用例：9 个

2. **测试覆盖的功能点**
   - ✅ logout 函数重构后的 API 调用验证
   - ✅ escapeHtml 不影响 E2E 流程
   - ✅ PUBLIC_ENDPOINTS 避免 401 误处理
   - ✅ handleUnauthorized 401 处理逻辑
