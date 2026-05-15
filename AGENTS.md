# EasyOrange 项目指南

EasyOrange 是基于 Spring Boot 4 + React 的全栈二手交易平台。

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4.0.3, MyBatis-Plus 3.5.16 |
| **前端** | TypeScript, React |
| **数据库** | MySQL 8.4, Redis 7.4 |
| **认证** | JWT (Access + Refresh Token) |
| **迁移** | Flyway 11.14.1 |
| **部署** | Docker, docker-compose |

## 数据库表清单

| 表名 | 说明 | 备注 |
|------|------|------|
| `eo_user` | 用户信息表 | 含角色/状态/手机号 |
| `eo_product` | 商品信息表 | 6状态: DRAFT(0)/PENDING_REVIEW(4)/REJECTED(5)/ONLINE(1)/SOLD(2)/OFFLINE(3) |
| `eo_product_audit_log` | 审核记录表 | action: 1通过/2拒绝/3重提交; 含维度JSON+前后状态快照 |
| `eo_product_detail` | 商品详情表 | JSON 格式 |
| `eo_product_report` | 举报记录表 | 4状态: PENDING(0)/PROCESSING(1)/RESOLVED(2)/DISMISSED(3); 含reason_type分类+24h重复检测 |
| `eo_report_handle_history` | 举报处理历史表 | 记录每次管理员操作(action/remark/operator_id) |

## 商品审核工作流

状态机: `DRAFT(0) → PENDING_REVIEW(4) → ONLINE(1)` / `REJECTED(5) → PENDING_REVIEW(4)` (循环)

- 卖家发布商品自动进入待审核
- 管理员审核通过→上架, 驳回→退回草稿(可重新提交)
- 审核结果触发站内消息通知(AUDIT_SUCCESS/AUDIT_REJECTED)
- 审核记录持久化至 `eo_product_audit_log` 表

## 举报处理工作流

状态机: `PENDING(0) → PROCESSING(1) → RESOLVED(2)` / `DISMISSED(3)`

- 用户提交举报（选择类型+填写描述），24h内同一用户对同一商品不可重复举报
- 管理员可单条/批量处理（**resolve** 通过/**dismiss** 驳回/IGNORE/PRODUCT_OFFLINE/WARN_SENDER/BAN_PRODUCT）
- 每次处理操作记录至 `eo_report_handle_history` 表
- 处理完成后通过 `ReportProcessedEvent` → `ReportProcessedEventListener` 异步发送站内信通知举报人
- 用户可查看自己的举报列表（分页）和详情（含处理结果）

**用户侧 API**: `POST /api/reports/product/{id}`, `GET /api/reports/my`, `GET /api/reports/{id}`
**管理端 API**: 
| 功能 | 路由 | 说明 |
|------|------|------|
| 举报列表 | `GET /api/admin/reports` | 分页查询 |
| 举报详情 | `GET /api/admin/reports/{id}` | 单条详情 |
| 处理举报 | `PUT /api/admin/reports/{id}/handle` | 单条处理 |
| 批量处理 | `PUT /api/admin/reports/batch-handle` | 批量操作 |
| 处理历史 | `GET /api/admin/reports/{id}/history` | 操作记录 |
| 统计数据 | `GET /api/admin/reports/stats` | 统计信息 |

## 项目结构

```
easy-orange/
├── easyorange-backend/          # Spring Boot 后端 (10 Maven 模块)
│   ├── easyorange-common/       # 通用组件 (Result, PageResult, 注解, 异常)
│   ├── easyorange-framework/    # 框架基础设施 (Security, Redis, 事件, AOP)
│   ├── easyorange-user/         # 用户模块 (DDD)
│   ├── easyorange-product/      # 商品模块 (DDD + CQRS + 审核工作流)
│   │   ├── domain/
│   │   │   ├── enums/ProductStatus.java    # 6状态: DRAFT/PENDING_REVIEW/REJECTED/ONLINE/SOLD/OFFLINE
│   │   │   ├── enums/ProductReportStatus.java # 4状态: PENDING/PROCESSING/RESOLVED/DISMISSED
│   │   │   ├── enums/ReportReasonType.java    # 4类型: FAKE_INFO/INFRINGEMENT/VIOLATION/OTHER
│   │   │   ├── entity/ProductReport.java      # 举报聚合根（含ReportDomainException内部类）
│   │   │   ├── entity/ReportHandleHistory.java # 处理历史实体（含reconstitute工厂方法）
│   │   │   ├── event/ProductAuditedEvent.java  # 审核完成领域事件
│   │   │   └── event/ReportProcessedEvent.java # 举报处理完成事件
│   │   ├── adapter/outbound/persistence/
│   │   │   ├── dataobject/ProductAuditLogDO.java     # 审核日志 DO (Builder模式)
│   │   │   ├── dataobject/ProductReportDO.java       # 举报记录 DO
│   │   │   ├── dataobject/ReportHandleHistoryDO.java # 处理历史 DO
│   │   │   ├── mapper/ProductAuditLogMapper.java      # 审核日志 Mapper
│   │   │   ├── mapper/ReportHandleHistoryMapper.java  # 处理历史 Mapper
│   │   │   ├── mapper/xml/ProductAuditLogMapper.xml  # 审核日志 SQL
│   │   │   └── repository/
│   │   │       └── ReportHandleHistoryRepositoryImpl.java # 处理历史仓储实现
│   │   ├── application/command/handler/CreateProductReportHandler.java  # 创建举报(含24h重复检测)
│   │   ├── application/query/handler/GetMyReportsHandler.java          # 我的举报查询
│   │   ├── application/query/handler/GetReportDetailHandler.java      # 举报详情查询
│   │   └── application/command/ProductCommandService.java # 含 submitForReview()
│   ├── easyorange-order/        # 订单模块 (DDD + CQRS + Saga)
│   ├── easyorange-payment/      # 支付模块 (DDD + CQRS + Outbox)
│   ├── easyorange-message/      # 消息模块 (DDD + WebSocket + 聊天)
│   │   ├── domain/
│   │   │   ├── entity/Message.java              # 消息实体（含 recall()/isSender(), msgStatus, recalledAt）
│   │   │   ├── event/MessageRecalledEvent.java   # 撤回领域事件
│   │   │   ├── service/SensitiveWordFilterService.java # 敏感词过滤（***替换）
│   │   │   └── service/RateLimiterService.java   # Redis 滑动窗口限流（消息5条/s, typing1次/2s）
│   │   ├── application/command/
│   │   │   ├── RecallMessageCommand.java        # 撤回命令
│   │   │   └── MessageCommandHandler.java       # 含 recall() + 限流 + 敏感词过滤
│   │   ├── controller/MessageCommandController.java  # 新增 /{id}/recall, POST /typing
│   │   ├── websocket/
│   │   │   ├── ChatWebSocketHandler.java        # STOMP: /chat.send, /chat.typing, broadcastRecallEvent
│   │   │   └── WebSocketEventListener.java      # @EventListener MessageRecalledEvent → WS 推送
│   │   └── TypingIndicatorService.java          # Redis TTL typing 状态管理
│   ├── easyorange-favorite/     # 收藏模块 (DDD 六边形架构)
│   ├── easyorange-admin/        # 管理端模块 (独立模块，管理API + 审核工作流)
│   │   ├── controller/AdminProductAuditController.java  # PUT audit, POST batch-audit, GET audit-logs
│   │   ├── controller/AdminReportController.java       # 举报管理（列表/详情/处理/批量处理/历史/统计）
│   │   ├── service/
│   │   │   ├── AdminDashboardService.java
│   │   │   ├── AdminUserService.java
│   │   │   ├── AdminUserServiceExtension.java
│   │   │   ├── AdminProductService.java
│   │   │   ├── AdminProductAuditService.java       # 状态校验→写日志→发事件
│   │   │   ├── AdminOrderService.java
│   │   │   ├── AdminCategoryService.java
│   │   │   └── AdminReportService.java             # 含批量处理+处理历史+事件发布
│   │   └── dto/
│   │       ├── request/                             # 14 个请求 DTO（含 BatchHandleRequest）
│   │       └── response/                            # 19 个响应 VO（含 TrendVO/ActivityVO）
│   └── easyorange-application/  # 应用启动入口 + Flyway + 架构测试
│       ├── adapter/event/ProductAuditEventListener.java    # 审核→站内消息通知
│       └── adapter/event/ReportProcessedEventListener.java # 举报处理→站内消息通知(AFTER_COMMIT+Async)
├── easyorange-frontend/         # React 前端 (Vite + TypeScript + TanStack Query)
│   ├── src/admin/               # 管理后台
│   │   ├── components/         # AdminTable, AdminSelect(Portal+listRef防误关), StatusBadge, ConfirmModal(Portal), StatCard, AdminMenuEntry
│   │   ├── pages/
│   │   │   ├── products/
│   │   │   │   ├── ProductListPage.tsx          # 商品列表
│   │   │   │   ├── ProductReviewPage.tsx        # 商品审核页（默认待审核，含状态筛选）
│   │   │   │   └── ProductDetailDrawer.tsx      # 商品详情抽屉（Portal挂载body，审核维度+原因输入+驳回弹窗+审核时间线）
│   │   │   ├── orders/
│   │   │   │   ├── OrderManagePage.tsx           # 订单管理列表
│   │   │   │   └── OrderDetailModal.tsx          # 订单详情弹窗（Portal挂载body，含状态/金额/商品/时间/地址/备注）
│   │   │   ├── users/UserDetailModal.tsx         # 用户详情弹窗（Portal挂载body）
│   │   ├── hooks/useAdminProductAudit.ts    # useAuditProduct / useBatchAuditProducts / useAuditLogs
│   │   ├── types/admin.ts                   # 含 AuditLogVO, AuditDimension, ProductAuditRequest
│   │   └── api/adminApi.ts                  # 含 getAuditLogs() + 增强 auditProduct()
│   ├── src/pages/products/ProductDetailPage.tsx  # 用户商品详情（审核状态标签+重提交按钮）
├── doc/                         # 项目文档
│   ├── 架构/                   # 架构规范文档（已切分为多个子文档）
│   └── specs/                  # 功能设计规格文档
└── .trae/rules/                 # AI 编码规则
```

## 后端架构核心原则

1. **DDD 分层**: domain → application → adapter，依赖方向单向向内
2. **CQRS**: 命令与查询分离 (product, order, payment 模块)
3. **六边形架构**: domain 层通过 port 接口与外部解耦
4. **不可变性**: 聚合根用 `@Builder(toBuilder = true)`，值对象用 `record`
5. **领域事件**: `@PublishEvent` 注解 + AOP 切面发布
6. **ACL 隔离**: 跨模块通过 ACL/Port 适配，禁止直接依赖领域模型
7. **异常继承**: 领域异常必须继承 `BaseBusinessException`（common 模块），`GlobalExceptionHandler` 已有统一处理器返回 400 + 业务错误码；**禁止直接抛出非 `BaseBusinessException` 子类的 RuntimeException**，否则会落入 500 兜底

## 模块依赖关系

```
application → framework, user, product, order, payment, message, favorite
framework → common
user → framework
product → framework, user (通过 SellerInfoPort 隔离，optional)
order → framework, product, user, payment (通过 Port 接口隔离，optional)
payment → framework
message → framework, user (通过 UserInfoPort 隔离，optional)
favorite → framework, product (通过 ProductInfoPort 隔离，optional)
admin → framework, common, user (optional)
```

> **状态 (2026-05-09)**：所有跨模块依赖已通过端口接口 + 适配器模式隔离，Maven 依赖标记为 `<optional>true</optional>`。写操作通过事件驱动解耦，查询操作保留同步端口调用。

## 已知问题

（暂无）

## 错误码规范

错误码采用 **A/B/C/D 前缀格式**：

| 前缀 | 类型 | HTTP 状态 | 示例 |
|------|------|----------|------|
| A | 成功/客户端语义 | 200/4xx | A0000=成功, A0401=未登录, A0403=禁止访问 |
| B | 业务错误 | 400 | B0001=操作失败, B0002=业务异常 |
| C | 系统错误 | 500 | C0500=服务器内部错误 |
| D | 第三方错误 | 502 | D0502=上游服务不可用 |

判断成功：`"A0000".equals(code)`

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

## 开发规范

- 编码规则见 `.trae/rules/` 目录
- 架构守卫测试: `ArchitectureRulesTest.java` (ArchUnit)
- 数据库变更必须通过 Flyway 迁移脚本
- 所有 API 统一返回 `Result<T>`，分页返回 `PageResult<T>`
- 测试覆盖率目标 ≥ 80%
- **Snowflake ID**: 后端 Long 主键通过 Jackson 2.x `ObjectMapper` 和 Jackson 3.x `JsonMapper` 的 `ToStringSerializer` 序列化为字符串；前端所有实体 ID 字段类型为 `string`，禁止使用 `number`（防止 JS 精度丢失）
- **React Query 缓存**: mutation 后 `invalidateQueries` 必须使用 `ORDER_KEYS.all` 前缀匹配，确保 myOrders/soldOrders/detail 等所有查询都能被正确失效

## Repository Map

A full codemap is available at `codemap.md` in the project root.

Before working on any task, read `codemap.md` to understand:
- Project architecture and entry points
- Directory responsibilities and design patterns
- Data flow and integration points between modules

For deep work on a specific folder, also read that folder's `codemap.md`.

## 常用命令

```bash
# 后端构建
cd easyorange-backend && ./mvnw clean package -DskipTests

# 运行所有测试
./mvnw test

# 运行特定模块测试
./mvnw test -pl easyorange-message -am
./mvnw test -pl easyorange-admin

# 启动开发环境 (MySQL + Redis)
docker-compose up -d

# 启动后端
./mvnw spring-boot:run -pl easyorange-application
```
