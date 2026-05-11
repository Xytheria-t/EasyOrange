---
tags:
  - always-on
---

# EasyOrange 项目指南

**EasyOrange** 是一个基于 Spring Boot + React 的全栈电商平台项目。

## 项目结构

```
easy-orange/
├── easyorange-backend/     # Spring Boot 后端 (Java 25)
│   ├── easyorange-application/   # 应用启动入口
│   ├── easyorange-common/        # 通用组件
│   ├── easyorange-framework/      # 框架配置
│   ├── easyorange-user/          # 用户模块
│   ├── easyorange-product/       # 商品模块
│   ├── easyorange-order/         # 订单模块
│   ├── easyorange-payment/       # 支付模块
│   ├── easyorange-message/       # 消息模块
│   └── easyorange-favorite/      # 收藏模块
├── easyorange-frontend/     # React 前端 (TypeScript)
└── .trae/rules/            # AI 规则文件
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
- **Snowflake ID 序列化**: 所有 `Long` 类型主键（orderId, userId, productId 等）在 JSON 响应中必须序列化为字符串，禁止以数字形式返回。后端同时配置 Jackson 2.x `ObjectMapper` 和 Jackson 3.x `JsonMapper` 的 `ToStringSerializer`（`JacksonConfig.longToStringModule()` + `longToStringJackson3Module()`），前端 TypeScript 中所有实体 ID 字段类型为 `string`（非 `number`），防止 JavaScript 精度丢失
- **React Query 缓存失效**: mutation 后 `invalidateQueries` 必须使用 `ORDER_KEYS.all`（`['orders']`）前缀，确保能匹配 `myOrders` / `soldOrders` / `detail` 等所有查询。使用 `ORDER_KEYS.lists()`（`['orders', 'list']`）会导致 myOrders/soldOrders 缓存无法失效

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
