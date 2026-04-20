# EasyOrange Codemap 索引

**Last Updated:** 2026-04-18  
**项目:** EasyOrange - 校园二手交易平台  
**技术栈:** TypeScript, React-free Vanilla JS, Vite, Spring Boot, PostgreSQL

## 文档结构

```
docs/codemaps/
├── INDEX.md              # 本索引文档
├── frontend-auth.md      # 前端认证模块
├── frontend-api.md       # 前端 API 层
├── shared-adapters.md    # 共享适配层
└── testing-e2e.md        # E2E 测试
```

## 核心模块 Codemap

### 1. 前端认证模块

**文档:** [`frontend-auth.md`](./frontend-auth.md)  
**入口文件:** 
- [`easyorange-frontend/src/app/authSession.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/authSession.ts)

**核心功能:**
- 会话管理（setSession, clearSession）
- 登出流程（调用后端撤销 token）
- 401 未授权处理
- 认证状态事件

**最近更新 (2026-04-18):**
- ✅ logout 函数重构 - 调用后端 API 撤销 token
- ✅ 增加 escapeHtml 防止 XSS
- ✅ 增加 PUBLIC_ENDPOINTS 白名单

---

### 2. 前端 API 层

**文档:** [`frontend-api.md`](./frontend-api.md)  
**入口文件:** 
- [`easyorange-frontend/src/api/core/request.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts)

**核心功能:**
- HTTP 请求封装（request）
- 请求管理器（去重、取消）
- 缓存机制（TTL 5 分钟）
- 重试机制（指数退避）
- 错误处理（XSS 防护）

**最近更新 (2026-04-18):**
- ✅ 新增 escapeHtml 函数 - XSS 防护
- ✅ 新增 PUBLIC_ENDPOINTS 常量 - 公开端点白名单
- ✅ 优化 shouldHandleUnauthorized 逻辑

---

### 3. 共享适配层

**文档:** [`shared-adapters.md`](./shared-adapters.md)  
**入口文件:** 
- [`easyorange-shared/src/adapters/web-adapter.ts`](file:///d:/Projects/EasyOrange/easyorange-shared/src/adapters/web-adapter.ts)

**核心功能:**
- HttpClient 接口定义
- WebAdapter 实现（Web 平台）
- 文件上传（单文件/多文件）
- 令牌管理
- SSRF 防护

**最近更新 (2026-04-18):**
- ✅ 新增 FormFieldValue 类型 - 类型安全表单
- ✅ 增强 SSRF 防护 - 限制文件路径协议
- ✅ 完善 upload/uploadMultiple 方法签名

---

### 4. E2E 测试

**文档:** [`testing-e2e.md`](./testing-e2e.md)  
**入口文件:** 
- [`easyorange-frontend/tests/e2e/auth-critical.spec.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/tests/e2e/auth-critical.spec.ts)

**测试场景:**
- 场景 1: 认证重定向返回（3 个测试用例）
- 场景 2: 强制 401 收敛（2 个测试用例）
- 场景 3: 登出撤销确认（3 个测试用例）

**测试覆盖:**
- ✅ 未登录访问受保护页面
- ✅ 登录成功后返回原页面
- ✅ Token 过期处理
- ✅ 登出 API 调用验证
- ✅ API 失败容错

**最近更新 (2026-04-18):**
- ✅ 新增 auth-critical.spec.ts - 9 个认证关键场景测试

---

## 架构图

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      EasyOrange 架构                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  前端 (easyorange-frontend)                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Pages     │  │ Components  │  │   Utils     │        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
│         │                │                │                 │
│         └────────────────┼────────────────┘                 │
│                          │                                  │
│                  ┌───────▼────────┐                         │
│                  │  authSession   │                         │
│                  └───────┬────────┘                         │
│                          │                                  │
│                  ┌───────▼────────┐                         │
│                  │   request.ts   │                         │
│                  └───────┬────────┘                         │
│                          │                                  │
└──────────────────────────┼──────────────────────────────────┘
                           │ HTTP/JSON
┌──────────────────────────┼──────────────────────────────────┐
│                          │         共享包                     │
│                  ┌───────▼────────┐                         │
│                  │  WebAdapter    │                         │
│                  │  (shared)      │                         │
│                  └───────┬────────┘                         │
│                          │                                  │
└──────────────────────────┼──────────────────────────────────┘
                           │ HTTP/JSON
┌──────────────────────────┼──────────────────────────────────┐
│                          │         后端                      │
│                  ┌───────▼────────┐                         │
│                  │  Spring Boot   │                         │
│                  │  Controllers   │                         │
│                  └────────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 认证流程

```
┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐
│  User    │      │  UI      │      │ authSess │      │ request  │
└────┬─────┘      └────┬─────┘      └────┬─────┘      └────┬─────┘
     │                 │                 │                 │
     │ 点击登出         │                 │                 │
     ├────────────────>│                 │                 │
     │                 │                 │                 │
     │                 │ 调用 logout()   │                 │
     │                 ├────────────────>│                 │
     │                 │                 │                 │
     │                 │                 │ POST /logout    │
     │                 │                 ├────────────────>│
     │                 │                 │                 │
     │                 │                 │ 后端撤销 token   │
     │                 │                 │                 │
     │                 │                 │ clearSession()  │
     │                 │<────────────────┤                 │
     │                 │                 │                 │
     │                 │ 重定向到首页     │                 │
     │<────────────────┤                 │                 │
     │                 │                 │                 │
```

## 相关文档

### 项目文档

- [API 规范文档](../api/API_SPEC.md) - 后端接口定义和迁移指南
- [前端导航最佳实践](../optimize/frontend-navigation-best-practices.md) - 路由和导航模式
- [数据库创建指南](../optimize/database-creation-guide.md) - 数据库初始化和迁移

### 代码规范

- [TypeScript 编码风格](../../rules/typescript/coding-style.md)
- [通用编码风格](../../rules/common/coding-style.md)
- [测试要求](../../rules/common/testing.md)
- [安全指南](../../rules/common/security.md)

## 快速导航

### 按功能查找

| 功能 | 相关 Codemap | 相关文件 |
|------|-------------|----------|
| 用户登录 | [frontend-auth.md](./frontend-auth.md) | [`authSession.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/authSession.ts) |
| 用户登出 | [frontend-auth.md](./frontend-auth.md) | [`authSession.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/authSession.ts#L102-L116) |
| HTTP 请求 | [frontend-api.md](./frontend-api.md) | [`request.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/api/core/request.ts) |
| 文件上传 | [shared-adapters.md](./shared-adapters.md) | [`web-adapter.ts`](file:///d:/Projects/EasyOrange/easyorange-shared/src/adapters/web-adapter.ts#L51-L85) |
| 401 处理 | [frontend-auth.md](./frontend-auth.md) | [`authSession.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/src/app/authSession.ts#L118-L126) |
| E2E 测试 | [testing-e2e.md](./testing-e2e.md) | [`auth-critical.spec.ts`](file:///d:/Projects/EasyOrange/easyorange-frontend/tests/e2e/auth-critical.spec.ts) |

### 按文件查找

| 文件 | Codemap | 主要内容 |
|------|---------|----------|
| `authSession.ts` | [frontend-auth.md](./frontend-auth.md) | 认证会话管理 |
| `request.ts` | [frontend-api.md](./frontend-api.md) | HTTP 请求封装 |
| `web-adapter.ts` | [shared-adapters.md](./shared-adapters.md) | Web 平台适配器 |
| `auth-critical.spec.ts` | [testing-e2e.md](./testing-e2e.md) | E2E 测试用例 |

## 变更日志

### 2026-04-18

**新增 Codemap:**
- ✅ frontend-auth.md - 前端认证模块
- ✅ frontend-api.md - 前端 API 层
- ✅ shared-adapters.md - 共享适配层
- ✅ testing-e2e.md - E2E 测试
- ✅ INDEX.md - 本索引文档

**更新内容:**
- 认证模块：logout 函数重构，增加 XSS 防护
- API 层：escapeHtml 函数，PUBLIC_ENDPOINTS 白名单
- 共享适配层：FormFieldValue 类型，SSRF 防护增强
- E2E 测试：新增 9 个认证关键场景测试用例

## 维护指南

### 何时更新 Codemap

- ✅ 新增核心功能模块
- ✅ 重构现有代码结构
- ✅ 添加重要安全机制
- ✅ 修改关键数据流
- ✅ 新增 E2E 测试场景

### 更新流程

1. 分析代码变更
2. 更新对应 Codemap 文件
3. 更新架构图和数据流
4. 更新 INDEX.md 索引
5. 验证所有链接有效

### 质量标准

- [ ] 架构图准确反映代码结构
- [ ] 所有文件路径可点击跳转
- [ ] 数据流描述清晰
- [ ] 安全机制说明完整
- [ ] 测试覆盖点明确
- [ ] 变更历史记录准确
