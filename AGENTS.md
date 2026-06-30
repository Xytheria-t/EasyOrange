# EasyOrange — 砍业务,撑架构

> **副标:砍业务,撑架构。** 业务做减法,架构做加法。

EasyOrange 是基于 Spring Boot 4 + React 的全栈 **DDD + CQRS + Saga + 事件驱动 + AI 多模态** 架构 demo，**2025 年 11 月启动开发**。

> **品牌**：EasyOrange
> **定位**：面向 AI 工程实践的领域驱动设计与事件驱动架构 demo
> **业务容器**：C2C 资产流转（刻意简化：固定价格 + C2C 直发 + 平台不碰货）

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4.0.3, MyBatis-Plus 3.5.16 |
| **前端** | TypeScript, React |
| **数据库** | MySQL 8.4, Redis 7.4 |
| **消息队列** | RabbitMQ 3.13 (Spring AMQP 4.0.x) |
| **搜索引擎** | Elasticsearch 8.17.3 (IK 中文分词器) |
| **认证** | JWT (Access + Refresh Token) |
| **迁移** | Flyway 11.15.0 |
| **部署** | Docker, docker-compose |

## 数据库表清单

| 表名 | 说明 | 备注 |
|------|------|------|
| `eo_user` | 用户信息表 | user_type 枚举: 00(ADMIN)/01(NORMAL)/02(MANAGER) |
| `eo_product` | 商品信息表 | 6状态: DRAFT(0)/PENDING_REVIEW(4)/REJECTED(5)/ONLINE(1)/SOLD(2)/OFFLINE(3) |
| `eo_product_audit_log` | 审核记录表 | action: 1通过/2拒绝/3重提交; 含维度JSON+前后状态快照 |
| `eo_product_detail` | 商品详情表 | JSON 格式 |
| `eo_product_image` | 商品图片表 | 1:N, 含 is_main 主图标记 + sort_order 排序; 图片不在 eo_product 表上 |
| `eo_product_report` | 举报记录表 | 4状态: PENDING(0)/PROCESSING(1)/RESOLVED(2)/DISMISSED(3); 含reason_type分类+24h重复检测 |
| `eo_report_handle_history` | 举报处理历史表 | 记录每次管理员操作(action/remark/operator_id) |
| `eo_upload_file` | 文件上传记录表 | 含 storage_type/storage_key 支持多后端存储 |
| `eo_user_credit` | 用户信用分表 | score(0-200)+total_trades+completed_trades+cancelled_trades+total_reports+confirmed_reports+avg_rating |
| `eo_order` | 订单主表 | total_amount(行项总和); 状态机: PENDING_PAYMENT/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDED |
| `eo_order_item` | 订单行项表 | 含 product_snapshot JSON 快照, unit_price, quantity, subtotal |

## 商品审核工作流

状态机: `DRAFT(0) → PENDING_REVIEW(4) → ONLINE(1)` / `REJECTED(5) → PENDING_REVIEW(4)` (循环)

- 资产方提交资产自动进入待审核
- 管理员审核通过→上架, 驳回→退回草稿(可重新提交)
- 审核结果触发站内消息通知(AUDIT_SUCCESS/AUDIT_REJECTED)
- 审核记录持久化至 `eo_product_audit_log` 表

## AI 能力清单

> **业务定位**：项目选 C2C 资产流转作为业务容器，AI 能力是 **架构展示** 的一部分（演示 LLM / Vision 的端口抽象 + 多级缓存 + 限流降级），不是商业模式护城河。议价 / 阶梯降价 / AI 自动成单等"AI 替资产方运营"的营销叙事已在 2026-06-25 下线。
>
> **平台边界**：平台不碰货、不囤货、不经手资金，物流走资产方→认领方 C2C 直发。
> 资产方只需发布资产、设固定价格，平台 AI 在两端做辅助决策；认领方获得 AI 找货 / 评估 / 信用画像等能力。
> **详细机制**（智能定价 / AI 营销文案 / WebSocket 实时沟通协议）见 [doc/集成/AI-资产管理.md](doc/集成/AI-资产管理.md)。

**核心约定**：
- **资产方侧 3 个决策点**：**智能估值** / **AI 营销文案** / **AI 信用画像**
- **认领方侧 3 个决策点**：**AI 智能找货** / **AI 物品评估** / **AI 信用画像**
- 资产方按固定价格上架资产，平台不参与议价 / 不自动调价
- 沟通走 `WebSocket /ws/chat` STOMP 通道（认领方与资产方直聊）
- AI 适配器：`CachingLlmAdapter` / `CachingVisionAdapter` 是 `@Primary` 装饰器（包装 `DeepSeekLlmAdapter` / `QwenVlVisionAdapter`）实现 L1 + L2 多级缓存
- 限流：`AiRateLimitInterceptor` 基于 Redis 令牌桶，Redis 不可用时 fail-open

## 举报处理工作流

状态机: `PENDING(0) → PROCESSING(1) → RESOLVED(2)` / `DISMISSED(3)`

- 用户提交举报（选择类型+填写描述），24h内同一用户对同一商品不可重复举报
- 管理员可单条/批量处理（**resolve** 通过/**dismiss** 驳回/IGNORE/PRODUCT_OFFLINE/WARN_SENDER/BAN_PRODUCT）
- 每次处理操作记录至 `eo_report_handle_history` 表
- 处理完成后通过 `ReportProcessedEvent` → `ReportProcessedEventListener` 异步发送站内信通知举报人
- 用户可查看自己的举报列表（分页）和详情（含处理结果）

> **完整 API 速查表**（用户侧 + 管理端 + 所有模块 + AI 功能）见 [doc/集成/API-速查.md](doc/集成/API-速查.md)。

## 项目结构

```
easy-orange/
├── easyorange-backend/          # Spring Boot 后端 (11 Maven 模块)
│   ├── easyorange-common/       # 通用组件 (Result, PageResult, 注解, 异常)
│   ├── easyorange-framework/    # 框架基础设施 (Security, Redis, 事件, AOP, 文件存储, 图片处理, **RabbitMQ 消息队列**)
│   ├── easyorange-user/         # 用户模块 (DDD: 认证/注册/密码管理/个人资料)
│   │   ├── domain/service/      # AuthenticationService, RegistrationService, LoginSecurityService
│   │   ├── domain/port/         # SmsSenderPort, PasswordEncoderPort, LoginAttemptPort, SmsCodePort, AvatarFilePort
│   │   ├── adapter/outbound/mock/ # MockSmsCodeAdapter, MockSmsSenderAdapter (测试隔离)
│   │   └── adapter/inbound/web/dto/request/ # PasswordResetRequest, UpdateProfileRequest (新增)
│   ├── easyorange-product/      # 商品模块 (DDD + CQRS + 审核工作流 + 举报)
│   ├── easyorange-order/        # 订单模块 (DDD + CQRS + Saga)
│   ├── easyorange-payment/      # 支付模块 (DDD + CQRS)
│   ├── easyorange-message/      # 消息模块 (DDD + WebSocket + 聊天)
│   ├── easyorange-favorite/     # 收藏模块 (DDD 六边形架构)
│   ├── easyorange-ai/           # AI 模块 (Port/Adapter: LLM + Embedding + Vision)
│   ├── easyorange-admin/        # 管理端模块 (商品/举报/订单/评价/分类/用户管理 API)
│   └── easyorange-application/  # 应用启动入口 + Flyway + 架构测试 + ES 搜索适配器
├── easyorange-frontend/         # React 前端 (Vite + TypeScript + TanStack Query)
│   ├── src/admin/               # 管理端模块（暖橙指挥中心设计系统，完整 CRUD + 商品审核 + 举报处理）
│   ├── src/pages/               # C 端页面（商品详情/我的发布/通知/搜索/个人中心）
│   ├── src/components/          # 共享组件（AdminTable, Search/Facet, Chat, Notification）
│   ├── src/hooks/               # React Query hooks + 聊天/搜索 Hooks
│   ├── src/api/                 # API 模块（admin/message/notification/search）
│   └── src/types/               # 类型定义
├── doc/                         # 项目文档
│   └── 架构/                   # 架构规范文档（已切分为多个子文档）
└── .trae/rules/                 # AI 编码规则
```

## 后端架构核心原则

1. **DDD 分层**: domain → application → adapter，依赖方向单向向内
2. **CQRS**: 命令与查询分离 (product, order, payment 模块)
3. **六边形架构**: domain 层通过 port 接口与外部解耦
4. **不可变性**: 聚合根用 `@Builder(toBuilder = true)`，值对象用 `record`
5. **领域事件**: 应用服务调用 `DomainEventPublisher` 发布事件，框架层通过 **RabbitMQ Topic Exchange** (`eo.domain.events`) 路由到各模块 `@RabbitListener` 消费者。路由键由事件类名自动派生（`ProductCreatedEvent` → `product.created`），无需手动注册。每个消费者独占队列（`eo.{name}`），失败消息路由到 DLQ（`eo.{name}.dlq`）+ 指数退避重试。采用 RabbitMQ-only 模式（`@ConditionalOnProperty(matchIfMissing=true)` 保留以防无 RabbitMQ 环境）。**已实现 9 个事件消费者**: ProductEventConsumer, OrderNotificationEventConsumer, OrderSagaEventConsumer, StockReservationEventConsumer, PaymentInitiationEventConsumer, ProductAuditEventConsumer, ReportProcessedEventConsumer, WebSocketEventConsumer, PaymentMetricsConsumer
6. **Assembler 模式**: DTO 转换统一在 `adapter/inbound/web/assembler/` 目录下实现（FavoriteAssembler, CategoryAssembler, PaymentViewAssembler, UserAssembler）。**禁止**在 Controller/Service 中直接构造 Response DTO。已废弃旧 DTO（AddFavoriteDTO, FavoriteVO, QueryOrderRequest, PaymentQuery, PaymentView, PaymentMethodVO 等）
7. **ACL 隔离**: 跨模块通过 ACL/Port 适配，禁止直接依赖领域模型
8. **异常继承**: 领域异常必须继承 `BaseBusinessException`（common 模块），`GlobalExceptionHandler` 使用 Java 21 模式匹配 switch 在单个 `handle()` 方法内按类型分发，返回动态 HTTP 状态码（按错误码前缀自动映射：A0401→401/A0403→403/B→400/C→500/D→502）+ 业务错误码；校验类错误统一返回 400。**禁止直接抛出非 `BaseBusinessException` 子类的 RuntimeException**，否则会落入 500 兜底。`BusinessException` 和 `FileException` 构造器均设为 `protected`，抛业务异常时统一使用 `BusinessException.of(...)` / `FileException.of(...)` 工厂方法；子类可正常调用 `super(...)`。各模块领域异常必须使用模块专属 `ResultCode`（如 `ProductResultCode.PRODUCT_NOT_FOUND`），**禁止回退到全局 `B0002`**

## 模块依赖关系

```
application → framework, common, user, product, order, payment, message, favorite
framework → common
user → framework, common
product → framework, common, user (通过 SellerInfoPort 隔离，optional)
order → framework, common, product, user, payment (通过 Port 接口隔离，optional)
payment → framework, common
message → framework, common, user (通过 UserInfoPort 隔离，optional)
favorite → framework, common, product (通过 ProductInfoPort 隔离，optional)
ai → framework, common, product (通过 ProductSearchQueryPort 隔离)
admin → framework, common, user (optional), product (optional), order (optional), payment (optional), ai (optional)
```

> **状态**：所有跨模块依赖已通过端口接口 + 适配器模式隔离，Maven 依赖标记为 `<optional>true</optional>`。写操作通过事件驱动解耦，查询操作保留同步端口调用。

## 已知问题

- **Redis 连接**: `application.yaml` 的 base 配置和 `.env.example` 模板已统一默认 `REDIS_PASSWORD=easyorange123`，与 Docker Compose 一致。若仍遇到 `Unable to connect to Redis` 错误，检查：① 是否已执行 `docker compose up -d` 启动 Redis；② 环境变量 `REDIS_PASSWORD` 是否被设置为空值覆盖了默认值；③ **YAML 占位符必须用 `${VAR:default}`（单冒号），不要用 bash 风格的 `${VAR:-default}`**（多一个 `-`）—— Spring 会把 `-default` 当字面量默认值，导致 Lettuce 实际发出去的密码比预期多一个前导连字符，触发 `WRONGPASS invalid username-password pair`。**注意**：`docker-compose.yml` 和 `mvnw` 用 `:-` 是正确的（bash/Docker Compose 语法），但所有 `application*.yaml` 必须用单冒号。新增/修改 Spring Boot 配置占位符时，复制粘贴前先确认语法

## 错误码规范

错误码采用 **A/B/C/D 前缀格式**：

| 前缀 | 类型 | HTTP 状态 | 示例 |
|------|------|----------|------|
| A | 成功/客户端语义 | 200/4xx | A0000=成功, A0401=未登录, A0403=禁止访问 |
| B | 业务错误 | 400 | B0001=操作失败, B0002=业务异常 |
| C | 系统错误 | 500 | C0500=服务器内部错误 |
| D | 第三方错误 | 502 | D0502=上游服务不可用 |

判断成功：`"A0000".equals(code)`

### 错误码模块分配表

B 前缀（业务错误码）按模块分段，新增模块时在预留段内分配：

| 范围 | 模块 | 实现类 |
|------|------|--------|
| B000x | 全局通用 | `ResultCode` |
| B1xxx | user | `UserResultCode` |
| B2xxx | product | `ProductResultCode` |
| B3xxx | order | `OrderResultCode` |
| B4xxx | payment | `PaymentResultCode` |
| B5xxx | file | `FileResultCode` |
| B6xxx | **预留** | — |
| B7xxx | message | `MessageResultCode` |

> 新增模块时优先使用 B6xxx 范围，避免与现有范围冲突。各枚举统一使用 `@Getter @AllArgsConstructor` 模式实现 `IResultCode`。

## 架构文档

详细架构规范见 `doc/架构/` 目录：

| 文档 | 内容 |
|------|------|
| [架构.md](doc/架构/架构.md) | 主索引文档 |
| [架构-技术栈.md](doc/架构/架构-技术栈.md) | 技术选型 |
| [架构-系统架构.md](doc/架构/架构-系统架构.md) | 整体架构、模块划分 |
| [架构-模块结构.md](doc/架构/架构-模块结构.md) | 包结构规范 |
| [架构-DDD规范.md](doc/架构/架构-DDD规范.md) | DDD 设计规范 |
| [架构-安全认证.md](doc/架构/架构-安全认证.md) | JWT 认证 |
| [架构-数据库迁移.md](doc/架构/架构-数据库迁移.md) | Flyway 规范 |
| [架构-部署演进.md](doc/架构/架构-部署演进.md) | 部署与演进 |

## 集成文档

业务专题与 API 速查见 `doc/集成/` 目录：

| 文档 | 内容 |
|------|------|
| [AI-资产管理.md](doc/集成/AI-资产管理.md) | 资产方 / 认领方 6 个 AI 决策点 / 营销文案 / WebSocket 协议 / 资产方直发边界 |
| [API-速查.md](doc/集成/API-速查.md) | 后端所有 REST + WebSocket 端点速查 |

## 环境变量

| 变量 | 值 | 说明 |
|------|------|------|
| `DEEPSEEK_API_KEY` | - | DeepSeek API 密钥（智能定价/审核/问答/Embedding） |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com/v1` | DeepSeek API 地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | DeepSeek 文本模型 |
| `QWENVL_API_KEY` | - | 通义千问 VL API 密钥（拍照上架图片识别） |
| `QWENVL_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 通义千问 API 地址 |
| `QWENVL_MODEL` | `qwen-vl-max` | 通义千问视觉模型 |
| `VITE_WS_URL` | 自动推导 | WebSocket 地址（前端环境变量，默认从 `location.host` 推导） |

## 开发规范

- 编码规则见 `.trae/rules/` 目录
- 架构守卫测试: `ArchitectureRulesTest.java` (ArchUnit)
- 数据库变更必须通过 Flyway 迁移脚本
- 所有 API 统一返回 `Result<T>`，分页返回 `PageResult<T>`（搜索返回 `SearchPageResponse<T>`，包含 `records/total/current/size/pages` + `facets` 分面桶 + `aiEnhancement` 增强）
- 覆盖率报告由 **JaCoCo 0.8.12** 在 `prepare-package` 阶段生成（`jacoco:report`），门禁已移至 CI 层。依赖安全由 **OWASP Dependency Check 12.1.0** 在 `verify` 阶段检查（CVSS ≥ 8 阻断构建）
- **标准 API 优先（STP）**: 优先使用框架/标准库内置功能，不重复造轮子。Spring Security 有 JWT 认证就通过 `oauth2ResourceServer()` 配置，不要手写 Filter；有标准 `JwtDecoder`/`JwtEncoder` 就注入使用，不要手写 JWT 工具类。"零新增自定义代码"是最优方案——删掉手写代码，换成框架配置即可
- **测试统计**：后端 11 模块合计 1,269 测试用例，全部通过；前端 100 测试文件/952 测试用例
- **前端组件规范**：表单/按钮统一使用 shadcn/ui（Button、Input、Label、Checkbox、Switch、Select、Textarea、RadioGroup），禁止保留原生 `<button>` / `<input>` / `<textarea>` / `<select>`；导入优先走 `@/components/ui`，颜色/边框硬编码应提取到 `src/styles/tokens.css`；CSS reset 必须置于 `@layer base` 避免覆盖 Tailwind utilities；生产代码禁止 `console.log` / `console.warn` / `console.error`
- **前端表单校验**：所有包含显式校验逻辑的表单（登录/注册/发布/密码修改等），必须使用 `react-hook-form` + `zod` + `@hookform/resolvers` 方案，禁止手写 `useState` + 自定义 `validate` 函数。Zod schema 统一放在 `src/schemas/` 目录下，`.default()` 禁止使用（默认值通过 `useForm` 的 `defaultValues` 设置以保持类型推导正确）。`reValidateMode` 统一设为 `'onChange'` 使得首次提交后输入即时清除错误。管理端搜索/筛选类简单表单（如 CategoryManagePage）无需迁移
- **TestSecurityUtil**: 测试中禁止使用 `mockStatic(SecurityContextUtil.class)`（不支持静态 mock）。改用 `TestSecurityUtil.setSecurityContext(userId) + finally { clearSecurityContext() }` 模式，位于 `easyorange-framework/src/main/java/.../framework/util/TestSecurityUtil.java`
- **全局认证拦截**: SecurityConfig 的 `.anyRequest().authenticated()` 已在过滤器层拦截所有未认证请求，Controller 方法上**无需**重复添加 `@PreAuthorize("isAuthenticated()")`。仅在需要角色/权限校验时使用 `@PreAuthorize`（如 `hasRole('ADMIN')`）
- **UUID v7 ID**: 全库 ID 已从 Snowflake (Long) 迁移至 UUID v7 (RFC 9562, String)。后端通过 `IdGenerator` 接口（`UuidV7IdGenerator` 为 `@Primary` 实现，`SnowflakeIdGenerator` 作为备选）生成 36 位 UUID 字符串。`BaseDO.id` 字段类型为 `String`，`@TableId(type = IdType.INPUT)`。前端实体 ID 字段类型保持 `string`（无需更改，JS 始终兼容字符串）。迁移脚本：`V5__change_all_ids_to_varchar.sql`（30+ 表 BIGINT → VARCHAR(36)）。
- **全量 Long→String 迁移**: 涉及所有模块——领域事件、值对象、DO、DTO、Port 接口、Adapter、Controller、测试文件。`SecurityContextUtil.getCurrentUserIdOrThrow()` 返回 `String`（原 Long）。`SnowflakeConfig` 重命名为 `IdGeneratorConfig`。`TestSecurityUtil.setSecurityContext()` 同时保留 `Long` 和 `String` 重载。全项目 1248 测试通过。
- **React Query 缓存**: mutation 后 `invalidateQueries` 必须使用 `ORDER_KEYS.all` 前缀匹配，确保 myOrders/soldOrders/detail 等所有查询都能被正确失效
- **零配置启动**: 项目支持零配置开发环境启动（MySQL localhost:3306, Redis localhost:6379）。新开发者只需 `./mvnw install -DskipTests && ./mvnw spring-boot:run -pl easyorange-application` 即可运行。敏感配置通过根目录 `.env.example` 模板管理，复制为 `.env` 即可（IDEA、Docker Compose 通用）
- **.gitignore 规范**: 使用精简版 .gitignore (78行)，已忽略 AI 生成文件 (**/codemap.md, 298个)、AI 工具目录 (.slim/, .superpowers/)、前端 .env.production/.env.development、测试产物 (test-results/)
- **Java `var` 使用规范**: 局部变量推荐使用 `var` 的场景：同一类型构造器（`Foo x = new Foo()` → `var x = new Foo()`）、显式 cast（`Type x = (Type) expr` → `var x = (Type) expr`）、StringBuilder/ByteArrayOutputStream 等无泛型构造器。**不推荐**的场景：接口类型到实现类型的赋值（`List<X> x = new ArrayList<>()` → 保持 `List<X>`，使用 `var` 会丢失接口抽象）

## 常用命令

```bash
# 后端构建
cd easyorange-backend && ./mvnw clean package -DskipTests

# 运行所有测试
./mvnw test

# 运行特定模块测试
./mvnw test -pl easyorange-framework
./mvnw test -pl easyorange-admin
./mvnw test -pl easyorange-order
./mvnw test -pl easyorange-message -am

# 生成 JaCoCo 覆盖率报告
./mvnw clean test jacoco:report

# OWASP 依赖安全检查
./mvnw org.owasp:dependency-check-maven:check

# 启动开发环境 (MySQL + Redis + 可选 ES)
docker-compose up -d
docker compose up -d elasticsearch   # 启用 ES 搜索（需先构建镜像: docker compose build elasticsearch）

# 启动后端（⚠️ 不要单独用 spring-boot:run -pl，会从本地仓库加载依赖模块旧 JAR）
# 正确方式：先打包所有依赖模块，再 java -jar
cd easyorange-backend && ./mvnw clean package -DskipTests -pl easyorange-application -am && java --sun-misc-unsafe-memory-access=allow -jar easyorange-application/target/easyorange-application-*.jar

# 或先 install 所有模块到本地仓库，再用 spring-boot:run（spring-boot-maven-plugin 已配置 jvmArguments）
cd easyorange-backend && ./mvnw install -DskipTests && ./mvnw spring-boot:run -pl easyorange-application

# 前端测试
cd easyorange-frontend

# 运行所有单元/组件/Hook 测试
npm test

# 监听模式
npm run test:watch

# 覆盖率报告
npm run test:coverage

# E2E 测试（需先启动 dev server）
npm run test:e2e

# 生产构建 + Bundle 分析（rollup-plugin-visualizer 输出 dist/stats.html，treemap 可视化）
npm run build:analyze
```

# CI/CD 流水线（GitHub Actions）
# 配置文件: .github/workflows/ci.yml
# 触发: push/PR 到 main/develop 分支
# 包含: 后端编译 → 后端测试 → 前端依赖安装 → 前端 typecheck → 前端 lint → 前端测试
# 超时: 30 分钟 | 并行控制: 相同 PR 自动取消进行中运行
```

## Repository Map

A full codemap is available at `codemap.md` in the project root.

Before working on any task, read `codemap.md` to understand:
- Project architecture and entry points
- Directory responsibilities and design patterns
- Data flow and integration points between modules

For deep work on a specific folder, also read that folder's `codemap.md`.
