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
│   ├── easyorange-framework/    # 框架基础设施 (Security, Redis, 多级缓存, Bloom 过滤器, AOP, 事件, 文件, 分布式 ID, 一致性哈希, **RabbitMQ 消息队列**)
│   ├── easyorange-user/         # 用户模块 (DDD)
│   ├── easyorange-product/      # 商品模块 (DDD + CQRS)
│   ├── easyorange-order/        # 订单模块 (DDD + CQRS + Saga)
│   ├── easyorange-payment/      # 支付模块 (DDD + CQRS + Outbox)
│   ├── easyorange-message/      # 消息模块 (DDD + WebSocket, Repository 已迁移)
│   ├── easyorange-favorite/     # 收藏模块 (DDD 六边形架构)
│   ├── easyorange-ai/           # AI 模块 (Port/Adapter + LLM + Embedding + Vision)
│   ├── easyorange-admin/        # 管理端模块 (用户/商品/订单/分类/举报管理 API)
│   └── easyorange-application/  # 应用启动入口 + Flyway + 架构测试 + ES 搜索适配器
├── easyorange-frontend/         # React 前端
│   ├── src/admin/              # 管理端模块（暖橙指挥中心设计系统，前后端已完整对接）
│   │   ├── layout/             # AdminLayout, AdminSidebar(毛玻璃 210px), AdminHeader
│   │   ├── pages/              # dashboard / users / products / orders / categories / reviews / reports / stats
│   │   ├── users/UserDetailModal.tsx       # 用户详情弹窗（Portal挂载body）
│   │   ├── products/ProductDetailDrawer.tsx # 商品审核抽屉（Portal+驳回弹窗+图片预览）
│   │   └── orders/OrderDetailModal.tsx      # 订单详情弹窗（Portal挂载body）
│   │   └── 全部页面已对接真实后端 API（无 Mock 数据残留）
│   ├── src/components/         # AdminTable, AdminSelect(Portal+listRef防误关), StatusBadge, ConfirmModal(Portal), StatCard, AdminMenuEntry
│   │   ├── chat/              # 聊天组件（ChatHeader, MessageBubble[长按菜单], MessageList[虚拟滚动], ChatInputBar, TypingIndicator）
│   │   ├── hooks/             # useAdmin* / useAdminProductAudit / useAdminGuard
│   │   │   └── chat/          # useStompChat(STOMP连接), useChatMessages(react-query+store合并), useMessageRecall, useChatNotification(桌面通知+声音), useOfflineQueue
│   │   ├── api/adminApi.ts    # 39 个 API 函数，覆盖 8 个管理端 Controller
│   │   ├── api/messageApi.ts  # 消息 API（conversations, send, recall, typing, markAsRead）
│   │   ├── types/admin.ts     # 完整类型定义（Order/Report/Category/Audit/User 操作）
│   │   ├── store/chatStore.ts# 聊天全局状态（Zustand: messages, typingUsers, connectionStatus）
│   │   └── styles/            # admin.css（侧边栏 210px/头部）, admin-layout.css（布局骨架）, chat-window.css
│   ├── src/components/admin/   # 管理端共享组件（AdminMenuEntry 等，供用户侧 Header 引用）
│   ├── src/api/core/            # API 核心模块 (请求/缓存/拦截器)
│   ├── src/features/auth/       # 认证模块 (TokenRefreshManager)
│   └── src/hooks/ui/            # UI Hooks (useColumnCount 等)
├── doc/                         # 项目文档
│   └── 架构/                   # 架构规范文档（已切分为多个子文档）
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
- 所有 API 统一返回 `Result<T>`，分页返回 `PageResult<T>`（搜索返回 `SearchPageResponse<T>`，在 `PageResult` 基础上增加 `facets` 分面桶列表）
- 测试覆盖率目标 ≥ 80%
- **多模块构建**: 修改子模块后启动前必须先执行 `mvn clean install -Dmaven.test.skip=true`，确保子模块 JAR 安装到本地仓库，否则 DevTools 运行时会 ClassNotFoundException
- **前端CSS导入**: 共享组件（如 ProductCard）使用的样式CSS必须在组件文件自身 import，禁止仅依赖页面级导入。首页通过 React.lazy 懒加载 section 组件时，页面级CSS不会随组件chunk加载，导致首次渲染无样式
- **管理端弹窗/抽屉必须使用 Portal**: `src/admin/` 下所有 Modal、Drawer、确认框等弹出层组件**必须**通过 `createPortal(..., document.body)` 挂载到 `<body>` 节点。原因：`.admin-sidebar` 和 `.admin-header` 使用了 `backdrop-filter: blur()`，这会创建新的包含块（containing block），导致 `position: fixed` 的定位基准从视口变为被偏移的祖先元素，弹窗居中失效且底部截断。Portal 直接挂载 body 可彻底绕过此问题。居中方式统一用 `position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%)`
- **管理端弹窗内容溢出处理**: 所有 Portal 弹窗容器必须设置 `maxHeight: 'calc(100vh - 2rem)'` + `display: flex; flexDirection: column; overflow: hidden`，内容区域设置 `flex: 1; overflowY: auto; minHeight: 0`。确保超长内容可滚动，header/footer 固定可见
- **AdminTable render 函数签名**: 列定义的 `render` 回调签名为 `(value, record)` — 第一个参数是单元格值（`getValue(record, key)` 的结果），第二个参数才是完整行记录。常见错误：只接收第一个参数 `(record) => ...` 导致实际拿到的是 `undefined`（当列 key 在数据中不存在时），点击事件无法获取行数据
- **管理端样式约定**: `src/admin/` 下所有页面和组件**必须使用内联 `style={{}}` 方式编写样式**，禁止依赖外部CSS文件。原因：`admin-layout.css` 的 `.admin-content` 容器会导致外部CSS选择器优先级冲突或样式不生效。唯一例外是 `src/admin/styles/admin.css`（侧边栏/头部布局样式），由 AdminLayout 统一 import
- **管理端下拉菜单**: 所有 `<select>` 必须使用 `AdminSelect` 组件（位于 `src/admin/components/AdminSelect.tsx`）。原生 `<select>` 无法自定义选项样式且各浏览器渲染不一致。AdminSelect 通过 React `createPortal` 将下拉面板渲染到 `document.body`，解决父级 `backdrop-filter`/`transform` 导致的 fixed 定位失效问题
- **管理端路由架构**: `admin/*` 路由必须在 `MinimalLayout` 外部独立渲染（见 `src/routes/index.tsx`），否则用户端 Header/导航栏会在管理页面显示
- **Snowflake ID 序列化**: 所有 `Long` 类型主键（orderId, userId, productId 等）在 JSON 响应中必须序列化为字符串，禁止以数字形式返回。后端同时配置 Jackson 2.x `ObjectMapper` 和 Jackson 3.x `JsonMapper` 的 `ToStringSerializer`（`JacksonConfig.longToStringModule()` + `longToStringJackson3Module()`）。前端（用户端和管理端）TypeScript 中所有实体 ID 字段类型为 `string`（非 `number`），防止 JavaScript 精度丢失
- **React Query 缓存失效**: mutation 后 `invalidateQueries` 必须使用 `ORDER_KEYS.all`（`['orders']`）前缀，确保能匹配 `myOrders` / `soldOrders` / `detail` 等所有查询。使用 `ORDER_KEYS.lists()`（`['orders', 'list']`）会导致 myOrders/soldOrders 缓存无法失效
- **管理员角色判断**: 判断用户是否为管理员必须使用 `ADMIN_USER_TYPE` 常量（位于 `src/constants/app.ts`），禁止硬编码 `'00'`。使用处包括 `AdminMenuEntry`、`useAdminGuard` 等
- **查询方法只读事务**: 所有 Service 类中的纯查询/读取方法（find/get/list/query/count/check/is* 等命名）**必须**标注 `@Transactional(readOnly = true)`。写操作方法使用 `@Transactional(rollbackFor = Exception.class)`。遗漏只读注解会导致 Hibernate/MyBatis 做不必要的脏检查和 flush，影响性能
- **管理端页面布局模式**: `src/admin/pages/` 下所有页面**必须**使用以下三层结构，否则内容区会被裁切或背景不随内容滚动：
  1. 根容器: `position: relative, minHeight: 'calc(100vh - 80px)'`
  2. 背景层: `position: absolute, inset: 0, borderRadius: 20`（**禁止用 fixed**）
  3. 内容层: `position: relative, zIndex: 1, display: flex, flexDirection: column`
- **AdminSelect Portal 防误关**: AdminSelect 的下拉面板通过 `createPortal` 渲染到 `document.body`。`handleClickOutside` 必须**同时排除触发按钮 ref 和列表 listRef**，否则点击选项会立即关闭（mousedown 先冒泡到 document → 检测为外部点击 → 关闭 → onClick 被吞掉）。详见 `AdminSelect.tsx`
- **父 POM 模块注册**: 新增后端子模块时（如 easyorange-admin），必须在 `easyorange-backend/pom.xml` 的 `<modules>` 中注册，否则该模块不会被构建/安装到本地仓库，依赖它的模块会报 `未解析的依赖项` 错误
- **Flyway SQL 格式**: 迁移脚本中 CREATE TABLE 的列定义**禁止使用对齐格式**（列名与类型之间用大量空格填充对齐），必须使用紧凑格式。Flyway MySQL 解析器会对齐格式产生兼容性问题，导致 MySQL 1064 语法错误。详见 [架构-数据库迁移.md](doc/架构/架构-数据库迁移.md) 反模式章节
- **Zustand store 写入规则**: zustand store **只接受事件驱动写入**（STOMP 回调、用户操作回调），**禁止在 useEffect 内写入 store**。原因：zustand 的 `...spread` 操作每次产生新对象引用 → effect 内写 store → 新引用触发重渲染 → effect 再执行 → 无限循环（Maximum update depth exceeded）。正确做法：react-query 提供 `staleTime: Infinity` 初始数据，zustand store 叠加实时更新（selector 纯读取安全）
- **Zustand selector 稳定引用**: selector 中使用 `?? []` 或 `?? {}` 时**必须用模块级常量**（如 `const EMPTY_MESSAGES: ChatMessage[] = []`），禁止内联 `?? []`。原因：内联写法每次调用都创建新引用 → Zustand 用 `Object.is` 比较发现变化 → 触发重渲染 → selector 再执行 → 无限循环。配合 React 19 + StrictMode 的 `useSyncExternalStore` 双快照机制，循环会被放大到 50 层嵌套
- **聊天 conversationId 格式**: 前后端统一使用排序双 ID 格式 `conv_{minId}_{maxId}`（如 `conv_123_456`），确保 A→B 和 B→A 的会话 ID 一致。前端：`conv_${[currentUserId, targetUserId].sort().join('_')}`；后端：`"conv_" + Math.min(sender, receiver) + "_" + Math.max(sender, receiver)`
- **Mockito Java 25 兼容**: Java 25 下 Mockito inline mock maker 使用 ByteBuddy attach 机制会失败（WSL2 环境尤为明显）。已配置 `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` 使用 `mock-maker-subclass` 模式。**新测试不要尝试改回 inline 模式**
- **`product-paths` 白名单陷阱**: `security.product-paths` 配置的路径会跳过 JWT 认证（`JwtAuthenticationFilter.shouldNotFilter` 中 `"GET".equals(method) && matchesAnyPattern(path, productPaths)`）。配置项如 `/api/products` 会匹配 `/api/products/my`，导致需要认证的接口被公开访问。如果新增需要认证的商品相关接口，必须在 `JwtAuthenticationFilter.AUTH_REQUIRED_PRODUCT_PATHS` 中显式排除，或改用更精确的 `ignore-paths` 配置
- **TestSecurityUtil**: 测试中禁止使用 `mockStatic(SecurityContextUtil.class)`（SubclassByteBuddyMockMaker 不支持静态 mock）。改用 `TestSecurityUtil.setSecurityContext(userId)` + `} finally { TestSecurityUtil.clearSecurityContext(); }` 模式。工具类位于 `easyorange-framework/src/main/java/.../util/TestSecurityUtil.java`，所有模块测试通用。`clearSecurityContext()` 必须在 `finally` 块中调用，确保测试间隔离
- **不可变集合**: 全项目统一使用 Java 9+ 不可变集合工厂方法，**禁止使用 `Collections` 工具类创建空/单元素/不可包装集合**。具体规则见 `.trae/rules/java/coding-style.md` §Immutability
- **前端 ESLint jsx-a11y**: 所有非交互元素（div/span/article）上的点击事件必须改为语义化的 `button` 元素，或添加 `role="button"` + `tabIndex={0}` + `onKeyDown`。`label` 元素必须通过 `htmlFor` 关联表单控件或使用嵌套结构
- **前端 ESLint curly**: 所有 if/else/for/while 语句必须使用大括号，即使单行也要加 `{}`
- **前端 React Hooks**: `useEffect` 内禁止同步调用 `setState`（会触发无限循环）。使用 `useReducer` 或将状态逻辑移出 effect
- **前端 scrollIntoView 防误触发**: 使用 `scrollIntoView` 自动滚动时，必须通过 ref 记录上一次状态（如历史记录长度），仅在数据真正新增时滚动。禁止在依赖数组仅为 props/state 的 `useEffect` 中无条件调用 `scrollIntoView`，否则组件挂载/数据初始化时会意外滚动整个页面
- **注册昵称默认值**: 注册时 `nick_name` 默认等于 `username`，禁止引入随机昵称生成逻辑。用户后续可通过 `updatePersonalInfo` 接口自由修改昵称（`NicknameGeneratorPort`/`NicknameGenerator` 已删除）
- **LoginCredential sealed interface**: 登录凭据使用 `sealed interface LoginCredential`（位于 `domain/valueobject/`），新增登录方式必须添加新的 `record` 实现（如 `Password(String identifier, String password)`、`Sms(String phone, String verifyCode)`），禁止在单个命令类中通过枚举字段区分登录方式。`*Request` DTO 通过 `toCredential()` 方法转换为密封接口子类型
- **RabbitMQ 路由键规范**: 新增领域事件必须在 `RoutingKeyResolver.EVENT_ROUTING_KEYS` 中注册路由键，格式为 `{module}.{aggregate}.{event}`（如 `order.aggregate.created`）。使用字符串映射 `event.eventType()` 作为 key（非 Class 引用），避免 Maven 循环依赖
- **RabbitMQ 双模式切换**: 所有 RabbitMQ 相关组件（发布器、消费者）使用 `@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled")` 条件化启用。设置 `easyorange.rabbitmq.enabled=false` 可回退到原有 Spring EventBus + @Async 模式
- **RabbitMQ Spring AMQP 4.0.x API**: `CorrelationData` 在 `org.springframework.amqp.rabbit.connection` 包（非 support）；`ReturnsCallback.returnedMessage()` 接收 `ReturnedMessage` 对象（非分散参数）；concurrency 配置使用 `concurrent-consumers` + `max-concurrent-consumers`（不支持 `"1-5"` 范围格式）
- **ConfigurationProperties Bean 冲突**: 禁止在 `@ConfigurationProperties` 类上加 `@Component`，会导致与 `@EnableConfigurationProperties` 双重注册。如需解决冲突加 `@Primary`，并清除本地 Maven 仓库缓存 (`rm -rf ~/.m2/repository/com/cartethyia/easyorange-*`)

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
