---
tags:
  - always-on
---

# EasyOrange 项目指南

**EasyOrange** 是一个基于 Spring Boot + React 的全栈电商平台项目。

## 项目结构

```
easy-orange/
├── easyorange-backend/          # Spring Boot 后端
│   ├── easyorange-common/       # 通用组件 (Result, PageResult, 注解, 异常)
│   ├── easyorange-framework/    # 框架基础设施 (Security, Redis, 事件, AOP)
│   ├── easyorange-user/         # 用户模块 (DDD)
│   ├── easyorange-product/      # 商品模块 (DDD + CQRS)
│   ├── easyorange-order/        # 订单模块 (DDD + CQRS + Saga)
│   ├── easyorange-payment/      # 支付模块 (DDD + CQRS + Outbox)
│   ├── easyorange-message/      # 消息模块 (DDD + WebSocket, Repository 已迁移)
│   ├── easyorange-favorite/     # 收藏模块 (DDD 六边形架构)
│   ├── easyorange-admin/        # 管理端模块 (用户/商品/订单/分类/举报管理 API)
│   └── easyorange-application/  # 应用启动入口 + Flyway + 架构测试
├── easyorange-frontend/         # React 前端
│   ├── src/admin/              # 管理端模块（暖橙指挥中心设计系统，前后端已完整对接）
│   │   ├── layout/             # AdminLayout, AdminSidebar(毛玻璃 210px), AdminHeader
│   │   ├── pages/              # dashboard / users / products / orders / reports / stats
│   │   │   └── 全部页面已对接真实后端 API（无 Mock 数据残留）
│   │   ├── components/         # AdminTable, AdminSelect(Portal+listRef防误关), StatusBadge, ConfirmModal, StatCard, AdminMenuEntry
│   │   ├── hooks/              # useAdminDashboard / Users / Products / Orders / Reports / Categories / ProductAudit / AdminGuard
│   │   ├── api/adminApi.ts     # 33 个 API 函数，与后端 6 个 Controller 完全对齐
│   │   ├── types/admin.ts      # 完整类型定义（Order/Report/Category/Audit/User 操作）
│   │   └── styles/             # admin.css（侧边栏 210px/头部）, admin-layout.css（布局骨架）
│   ├── src/components/admin/   # 管理端共享组件（AdminMenuEntry 等，供用户侧 Header 引用）
│   ├── src/api/core/            # API 核心模块 (请求/缓存/拦截器)
│   ├── src/features/auth/       # 认证模块 (TokenRefreshManager)
│   └── src/hooks/ui/            # UI Hooks (useColumnCount 等)
├── doc/                         # 项目文档
│   └── 架构/                    # 架构规范文档（已切分为多个子文档）
└── .trae/rules/                 # AI 编码规则
```

## 规则激活机制

AI 规则存放在 `.trae/rules/` 目录，根据以下条件自动激活：

### 1. 文件路径激活（paths）

当编辑对应路径的文件时自动启用：

| 路径模式 | 激活规则 |
|---------|---------|
| `**/*.java` | Java 编码规范、模式、安全、测试 |
| `**/*.ts` | TypeScript 编码规范、模式、安全、测试 |
| `**/*.tsx` | TypeScript 编码规范、模式、安全、测试 |
| `**/*.css` | Web 设计规范、性能、安全 |
| `**/*.html` | Web 设计规范、性能、安全 |

### 2. 关键词激活（tags）

当用户输入包含以下关键词时自动启用对应规则：

| 关键词 | 激活规则 |
|-------|---------|
| auth, password, token, payment, encrypt, credential | 安全规则 |
| test, TDD, coverage, mock, unit test, e2e | 测试规则 |
| performance, optimize, cache, latency | 性能规则 |
| review, PR, pull request | 代码审查规则 |
| git, commit, branch, merge | Git 工作流规则 |
| pattern, architecture, refactor, repository | 设计模式规则 |
| agent, planner, tdd-guide | Agent 编排规则 |
| UI, component, design, layout | Web 设计规则 |

### 3. 始终激活（always-on）

所有任务都会加载的基础规则：
- `.trae/rules/karpathy-guidelines.md` - 行为准则
- `.trae/rules/common/coding-style.md` - 核心编码原则

### 4. 区域规则

当用户使用中文时自动加载：
- `.trae/rules/zh/*` - 中文版规则

## 快速参考

| 任务类型 | 加载规则 |
|---------|---------|
| 编写 Java 代码 | `java/coding-style.md`, `java/patterns.md` |
| 编写 TypeScript 代码 | `typescript/coding-style.md`, `typescript/patterns.md` |
| 编写前端样式 | `web/coding-style.md`, `web/design-quality.md` |
| 安全相关任务 | `common/security.md`, `*/security.md` |
| 测试相关任务 | `common/testing.md`, `*/testing.md` |
| 性能优化任务 | `common/performance.md` |
| 代码审查 | `common/code-review.md` |
| Git 操作 | `common/git-workflow.md` |

## 开发规范

- 编码规则见 `.trae/rules/` 目录
- 架构守卫测试: `ArchitectureRulesTest.java` (ArchUnit)
- 数据库变更必须通过 Flyway 迁移脚本
- 所有 API 统一返回 `Result<T>`，分页返回 `PageResult<T>`
- 测试覆盖率目标 ≥ 80%
- **多模块构建**: 修改子模块后启动前必须先执行 `mvn clean install -Dmaven.test.skip=true`，确保子模块 JAR 安装到本地仓库，否则 DevTools 运行时会 ClassNotFoundException
- **前端CSS导入**: 共享组件（如 ProductCard）使用的样式CSS必须在组件文件自身 import，禁止仅依赖页面级导入。首页通过 React.lazy 懒加载 section 组件时，页面级CSS不会随组件chunk加载，导致首次渲染无样式
- **管理端样式约定**: `src/admin/` 下所有页面和组件**必须使用内联 `style={{}}` 方式编写样式**，禁止依赖外部CSS文件。原因：`admin-layout.css` 的 `.admin-content` 容器会导致外部CSS选择器优先级冲突或样式不生效。唯一例外是 `src/admin/styles/admin.css`（侧边栏/头部布局样式），由 AdminLayout 统一 import
- **管理端下拉菜单**: 所有 `<select>` 必须使用 `AdminSelect` 组件（位于 `src/admin/components/AdminSelect.tsx`）。原生 `<select>` 无法自定义选项样式且各浏览器渲染不一致。AdminSelect 通过 React `createPortal` 将下拉面板渲染到 `document.body`，解决父级 `backdrop-filter`/`transform` 导致的 fixed 定位失效问题
- **管理端路由架构**: `admin/*` 路由必须在 `MinimalLayout` 外部独立渲染（见 `src/routes/index.tsx`），否则用户端 Header/导航栏会在管理页面显示
- **Snowflake ID 序列化**: 所有 `Long` 类型主键（orderId, userId, productId 等）在 JSON 响应中必须序列化为字符串，禁止以数字形式返回。后端同时配置 Jackson 2.x `ObjectMapper` 和 Jackson 3.x `JsonMapper` 的 `ToStringSerializer`（`JacksonConfig.longToStringModule()` + `longToStringJackson3Module()`）。**用户端** TypeScript 中所有实体 ID 字段类型为 `string`（非 `number`），防止 JavaScript 精度丢失。**管理端** (`src/admin/types/admin.ts`) 的 ID 字段使用 `number` 类型，因为管理端直接操作内部 Long 主键，不经过用户侧的序列化层
- **React Query 缓存失效**: mutation 后 `invalidateQueries` 必须使用 `ORDER_KEYS.all`（`['orders']`）前缀，确保能匹配 `myOrders` / `soldOrders` / `detail` 等所有查询。使用 `ORDER_KEYS.lists()`（`['orders', 'list']`）会导致 myOrders/soldOrders 缓存无法失效
- **管理员角色判断**: 判断用户是否为管理员必须使用 `ADMIN_USER_TYPE` 常量（位于 `src/constants/app.ts`），禁止硬编码 `'00'`。使用处包括 `AdminMenuEntry`、`useAdminGuard` 等
- **查询方法只读事务**: 所有 Service 类中的纯查询/读取方法（find/get/list/query/count/check/is* 等命名）**必须**标注 `@Transactional(readOnly = true)`。写操作方法使用 `@Transactional(rollbackFor = Exception.class)`。遗漏只读注解会导致 Hibernate/MyBatis 做不必要的脏检查和 flush，影响性能
- **管理端页面布局模式**: `src/admin/pages/` 下所有页面**必须**使用以下三层结构，否则内容区会被裁切或背景不随内容滚动：
  1. 根容器: `position: relative, minHeight: 'calc(100vh - 80px)'`
  2. 背景层: `position: absolute, inset: 0, borderRadius: 20`（**禁止用 fixed**）
  3. 内容层: `position: relative, zIndex: 1, display: flex, flexDirection: column`
- **AdminSelect Portal 防误关**: AdminSelect 的下拉面板通过 `createPortal` 渲染到 `document.body`。`handleClickOutside` 必须**同时排除触发按钮 ref 和列表 listRef**，否则点击选项会立即关闭（mousedown 先冒泡到 document → 检测为外部点击 → 关闭 → onClick 被吞掉）。详见 `AdminSelect.tsx`
- **父 POM 模块注册**: 新增后端子模块时（如 easyorange-admin），必须在 `easyorange-backend/pom.xml` 的 `<modules>` 中注册，否则该模块不会被构建/安装到本地仓库，依赖它的模块会报 `未解析的依赖项` 错误

---

# 行为准则

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
