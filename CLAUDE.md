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
| [doc/adr/](doc/adr/) | 架构决策记录（7 个 ADR，如 ADR-0007 拒绝 Saga 单事务、ADR-0008 Spring AI 迁移） |

核心原则：**DDD 六边形 + CQRS**，domain → application → adapter 单向依赖；**事件驱动 + Outbox + DLQ 三级重试 + traceId 全链路**；**分布式锁防超卖**；**AI 8 件套**（Spring AI 2.0 框架化 + 令牌桶 + stale 降级 + Embedding 真实现 + Prompt YAML + TokenBudget + 多模态 Vision）。状态机与错误码规范以 AGENTS.md 为准。

## 编码规则（ECC）

编码细则在 `.claude/rules/ecc/`（ECC 分层规则集，按文件路径自动激活，语言级规则优先级高于 common）：

| 路径 | 激活规则 |
|------|---------|
| `**/*.java` | `java/*.md`（extends common） |
| `**/*.ts` / `**/*.tsx` | `typescript/*.md` + `react/*.md` |
| `**/*.css` / `**/*.html` | `web/*.md` |
| 全局 | `common/*.md` |

## 后端约定

- **多模块构建**：修改子模块后启动前必须先 `mvn clean install -Dmaven.test.skip=true`，否则运行时按已安装 jar 解析子模块会 ClassNotFoundException
- **启动方式**：统一 `./mvnw spring-boot:run -pl easyorange-application`（终端）。JVM 参数（`--enable-native-access` 等）已由 `easyorange-application/pom.xml` 的 `<jvmArguments>` 管，环境变量走 `.env`/shell export。**IDE 调试可另用 run config，但必须把 JVM flag 手动补进 VM options**（pom 的 jvmArguments 不传给 IDE）；禁止终端/IDE 混跑同一份代码，否则 JVM flag 不生效难排查
- **父 POM 模块注册**：新增后端子模块必须在 `easyorange-backend/pom.xml` 的 `<modules>` 中注册
- **Mockito Java 25 兼容**：已配置 `mock-maker-subclass` 模式，**新测试不要改回 inline 模式**（WSL2 下 ByteBuddy attach 会失败）
- **`product-paths` 白名单陷阱**：`security.product-paths` 会跳过 JWT 认证且前缀匹配（`/api/products` 会匹配 `/api/products/my`）。新增需认证接口必须添加更精确的 `.requestMatchers(GET, "/api/products/my/**").authenticated()`
- **LoginCredential sealed interface**：登录凭据用 `sealed interface LoginCredential`（`domain/valueobject/`），新增登录方式添加新 `record` 实现，禁止用枚举字段区分
- **RabbitMQ Spring AMQP 4.0.x API**：`CorrelationData` 在 `org.springframework.amqp.rabbit.connection`；`ReturnsCallback.returnedMessage()` 接收 `ReturnedMessage` 对象；concurrency 用 `concurrent-consumers` + `max-concurrent-consumers`（不支持 `"1-5"` 范围格式）
- **ConfigurationProperties 注册方式**：统一 `@ConfigurationProperties` + `@ConfigurationPropertiesScan`，Properties 为纯 POJO（不加 `@Component`），已注册的不要再加 `@EnableConfigurationProperties`
- **Flyway SQL 格式**：CREATE TABLE 列定义禁止使用对齐格式（列名与类型间大量空格填充），必须紧凑格式，否则 MySQL 1064 语法错误
- **注册昵称默认值**：注册时 `nick_name` 默认等于 `username`，禁止随机昵称逻辑

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

## 行为准则

通用编码行为准则（KISS/DRY/YAGNI、外科手术式改动、先思考后编码、目标驱动执行）由 **karpathy-guidelines 技能**与 ECC `common/coding-style.md` 自动加载，此处不再重复。
