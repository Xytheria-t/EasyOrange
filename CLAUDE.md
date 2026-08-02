---
always-apply: true
---

# EasyOrange — LLM × DDD：Java 架构工程化实战

**EasyOrange** 是基于 Java 25 + Spring Boot 4 的 LLM × DDD 工程化实战项目——在 DDD 六边形架构里集成 LLM，让 AI 链路可换供应商、可降级、可观测。业务聚焦 C2C 资产流转（固定价格 + 直发 + 平台不碰货），把复杂度留给架构与 AI 工程化。**2025 年 11 月启动开发**。

> **定位、数字锚点与完整规范**见 [README.md](./README.md)、[AGENTS.md](./AGENTS.md)、[doc/工程指标.md](./doc/工程指标.md)。本文件为会话记忆，聚焦高频项目专属约定与避坑，与 AGENTS.md 互为补充而非重复。编码细则见 `.claude/rules/ecc/`。

## 快速开始

```bash
# 启动开发环境 (MySQL 8.4 + Redis 7.4 + RabbitMQ 3.13)
docker compose -f compose.yaml up -d

# 后端：先 install 依赖模块，再运行
cd easyorange-backend && ./mvnw install -DskipTests && ./mvnw spring-boot:run -pl easyorange-application

# 后端测试
./mvnw test            # 全部
./mvnw test -pl easyorange-order -am   # 单模块

# 前端测试
cd easyorange-frontend && npm test
```

> 完整命令清单（PIT 变异测试、JaCoCo 覆盖率、OWASP、生产构建）见 [AGENTS.md](./AGENTS.md)「常用命令」。

## 架构与文档地图

| 资源 | 内容 |
|------|------|
| [AGENTS.md](./AGENTS.md) | 唯一规范来源：技术栈、数据库表、状态机、错误码、模块依赖、开发规范 |
| [README.md](./README.md) | 项目定位与数字锚点 |
| [doc/工程指标.md](./doc/工程指标.md) | 测试数 / 覆盖率单一事实来源（2,412 测试 / Domain 层 84.1%） |
| [doc/架构/](doc/架构/) | 架构规范（系统架构、DDD、安全认证、数据库迁移、部署） |
| [doc/集成/](doc/集成/) | 业务专题（AI 资产管理、API 速查） |
| [doc/adr/](doc/adr/) | 架构决策记录（6 个 ADR，如 ADR-0007 拒绝 Saga 单事务） |

核心原则：**DDD 六边形 + CQRS**，domain → application → adapter 单向依赖；**事件驱动 + Outbox + DLQ 三级重试 + traceId 全链路**；**分布式锁防超卖**；**AI 8 件套**（Port/Adapter + 多级缓存 + 令牌桶 + stale 降级 + AiMetrics + Prompt YAML + TokenBudget + Bulkhead）。状态机与错误码规范以 AGENTS.md 为准。

## 编码规则（ECC）

编码细则在 `.claude/rules/ecc/`（ECC 分层规则集，按文件路径自动激活，语言级规则优先级高于 common）：

| 路径 | 激活规则 |
|------|---------|
| `**/*.java` | `java/*.md`（extends common） |
| `**/*.ts` / `**/*.tsx` | `typescript/*.md` + `react/*.md` |
| `**/*.css` / `**/*.html` | `web/*.md` |
| 全局 | `common/*.md` |

## 后端约定

- **多模块构建**：修改子模块后启动前必须先 `mvn clean install -Dmaven.test.skip=true`，否则 DevTools 运行时会 ClassNotFoundException
- **父 POM 模块注册**：新增后端子模块必须在 `easyorange-backend/pom.xml` 的 `<modules>` 中注册
- **查询只读事务**：纯查询方法（find/get/list/query/count/check/is*）必须标注 `@Transactional(readOnly = true)`；写操作 `@Transactional(rollbackFor = Exception.class)`
- **Mockito Java 25 兼容**：已配置 `mock-maker-subclass` 模式，**新测试不要改回 inline 模式**（WSL2 下 ByteBuddy attach 会失败）
- **`product-paths` 白名单陷阱**：`security.product-paths` 会跳过 JWT 认证且前缀匹配（`/api/products` 会匹配 `/api/products/my`）。新增需认证接口必须添加更精确的 `.requestMatchers(GET, "/api/products/my/**").authenticated()`
- **LoginCredential sealed interface**：登录凭据用 `sealed interface LoginCredential`（`domain/valueobject/`），新增登录方式添加新 `record` 实现，禁止用枚举字段区分
- **RabbitMQ Spring AMQP 4.0.x API**：`CorrelationData` 在 `org.springframework.amqp.rabbit.connection`；`ReturnsCallback.returnedMessage()` 接收 `ReturnedMessage` 对象；concurrency 用 `concurrent-consumers` + `max-concurrent-consumers`（不支持 `"1-5"` 范围格式）
- **ConfigurationProperties 注册方式**：统一 `@ConfigurationProperties` + `@ConfigurationPropertiesScan`，Properties 为纯 POJO（不加 `@Component`），已注册的不要再加 `@EnableConfigurationProperties`
- **Flyway SQL 格式**：CREATE TABLE 列定义禁止使用对齐格式（列名与类型间大量空格填充），必须紧凑格式，否则 MySQL 1064 语法错误
- **注册昵称默认值**：注册时 `nick_name` 默认等于 `username`，禁止随机昵称逻辑
- **不可变集合**：统一使用 Java 9+ 不可变集合工厂方法，禁止 `Collections` 工具类创建空/单元素/不可包装集合

## 前端约定

- **管理端弹窗/抽屉必须使用 Portal**：`src/admin/` 下所有 Modal/Drawer/确认框必须 `createPortal(..., document.body)` 挂载到 `<body>`。原因：`.admin-sidebar`/`.admin-header` 的 `backdrop-filter: blur()` 创建新包含块，导致 `position: fixed` 定位基准错乱。居中统一 `position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%)`
- **管理端弹窗内容溢出**：Portal 容器必须 `maxHeight: 'calc(100vh - 2rem)'` + `display: flex; flexDirection: column; overflow: hidden`，内容区 `flex: 1; overflowY: auto; minHeight: 0`
- **AdminTable render 函数签名**：`(value, record)` — 第一个参数是单元格值，第二个才是完整行记录。只接收 `(record) => ...` 会拿到 `undefined`
- **管理端样式约定**：`src/admin/` 下必须使用内联 `style={{}}`，禁止依赖外部 CSS（唯一例外 `src/admin/styles/admin.css`）
- **管理端下拉菜单**：所有 `<select>` 必须用 `AdminSelect` 组件（Portal 渲染下拉面板，解决 fixed 定位失效）
- **AdminSelect Portal 防误关**：`handleClickOutside` 必须同时排除触发按钮 ref 和列表 listRef，否则点击选项会立即关闭
- **管理端页面布局模式**：三层结构——根容器 `position: relative, minHeight: 'calc(100vh - 80px)'`；背景层 `position: absolute, inset: 0, borderRadius: 20`（禁 fixed）；内容层 `position: relative, zIndex: 1, display: flex, flexDirection: column`
- **管理端路由架构**：`admin/*` 路由必须在 `MinimalLayout` 外部独立渲染，否则 C 端 Header/导航栏会在管理页面显示
- **重依赖懒加载**：体积 > 100KB 的第三方库（recharts/dayjs/monaco-editor/xlsx/@tiptap/*）必须 (1) `manualChunks` 分配独立 `vendor-*` chunk (2) `React.lazy` + `Suspense` 包装。参考：`src/admin/pages/dashboard/charts/lazyCharts.tsx`
- **前端 CSS 导入**：共享组件（如 ProductCard）使用的样式 CSS 必须在组件文件自身 import，禁止仅依赖页面级导入（React.lazy 懒加载时页面级 CSS 不随组件 chunk 加载）
- **Zustand store 写入规则**：zustand store **只接受事件驱动写入**（STOMP 回调、用户操作回调），**禁止在 useEffect 内写 store**（spread 新引用 → 重渲染 → 无限循环）
- **Zustand selector 稳定引用**：selector 中 `?? []` / `?? {}` 必须用模块级常量，禁止内联（新引用触发无限循环，React 19 StrictMode 下放大到 50 层）
- **聊天 conversationId 格式**：排序双 ID `conv_{minId}_{maxId}`，确保 A→B 与 B→A 一致。前端 `conv_${[currentUserId, targetUserId].sort().join('_')}`；后端 `"conv_" + Math.min(sender, receiver) + "_" + Math.max(sender, receiver)`
- **scrollIntoView 防误触发**：必须通过 ref 记录上一次状态（如历史长度），仅在数据真正新增时滚动，禁止在仅依赖 props/state 的 effect 中无条件调用
- **Biome**：`lint/a11y/useSemanticElements` 优先语义化元素（`role="group"` → `<fieldset>`）；SVG 图标加 `aria-hidden="true"`；所有 if/else/for/while 必须加 `{}`（`useBlockStatements`）
- **React Hooks**：`useEffect` 内禁止同步调用 `setState`（无限循环），用 `useReducer` 或移出 effect

## 行为准则

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- **Prefer standard APIs over custom code**: Before writing a utility/filter/abstraction, check if the framework already provides it. Example: Spring Security `oauth2ResourceServer()` + `JwtDecoder`/`JwtEncoder` > custom `JwtAuthenticationFilter` + JJWT.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

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

### 4. Goal-Driven Execution

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
