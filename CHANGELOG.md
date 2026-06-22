# Changelog

> 基于 Git 提交记录自动生成

## [unreleased]

### 2026-06-22 — 通知系统精简与实时推送

- **fix(message)**: `MessageCommandHandler` 在线用户系统消息现在通过 `WebSocketNotifier.sendNotification()` 实时推送（此前仅存储不推送，前端依赖 30s 轮询）
- **fix(product)**: 修复 `notifyLowStock` 发送给 `receiverId=null` 的 bug，现在正确传递 `sellerId`
- **refactor(notification)**: 删除未使用的 `NotificationService` 接口 + `DefaultNotificationServiceImpl` 日志桩（common/framework 模块）
- **refactor(order)**: `OrderNotificationEventConsumer` 从 order 模块迁移到 application 模块，改用 `MessageCommandHandler` 发送站内消息（与其他 3 个通知消费者统一路径）
- **refactor(message)**: 删除死代码 `WebSocketMessageHandler`（`/chat.sendMessage`、`/chat.addUser` 无调用方，实际路径走 `ChatWebSocketHandler`）
- **feat(frontend)**: 新增 `useNotificationSocket` STOMP hook 订阅 `/user/queue/notification`，`NotificationBell` 轮询降为 60s 兜底

### 2026-06-04 — 全局权限注解清理

- **refactor**: 移除 46 个冗余 `@PreAuthorize("isAuthenticated()")` 注解（9 个 Controller）
- **chore**: 清理 8 个未使用的 `PreAuthorize` 导入
- **docs**: 补充全局认证约定到 AGENTS.md / CLAUDE.md

### 2026-06-02 — 启动日志清理与配置修复

- **fix**: JWT 开发密钥不再触发弱密钥警告（`dev-secret` → `dev-key`）
- **fix**: Flyway 升级至 11.15.0，消除 MySQL 8.4 版本警告
- **fix**: 关闭 dev 环境的 out-of-order 模式（个人开发无需多分支合并）
- **chore**: 关闭 MyBatis-Plus ASCII banner
- **chore**: 降低 Spring Boot devtools/actuator/web-context 等内部日志级别，精简启动输出

### 2026-06-01 — 文档与配置整理

- **docs**: 精简 README (604→121 行)，创建独立 CHANGELOG
- **docs**: 同步项目启动时间 2025-11 到所有文档
- **chore**: 更新 .gitignore 最佳实践（78 行精简版）
- **chore**: 补充 AI 文件忽略规则（codemap, .slim, .superpowers）

### 2026-06-01 — DDD 架构整合

- **feat(event)**: 实现 8 个领域事件消费者（PaymentInitiation, ProductAudit, ReportProcessed, StockReservation, OrderCreated, PaymentEvent, ProductEvent, WebSocket）
- **feat(user)**: 增强认证模块（PasswordManagementService, 密码重置, 个人资料）
- **feat(payment)**: 支付视图组装器 + PaymentMethodInfo 值对象
- **feat(query)**: Assembler 模式 + CQRS 查询 DTO（FavoriteAssembler, CategoryAssembler）
- **feat(framework)**: RabbitMQ 消息基础设施（DomainEventPublisher, 序列化工具）
- **refactor**: 全模块迁移到 Assembler 模式，清理废弃 DTO
- **docs**: 架构文档同步更新

### 2026-05-30 ~ 2026-05-29 — 架构重构冲刺

- **refactor**: 全面 DDD 架构重构，Auth 服务整合，Assembler 模式迁移
- **fix**: Flyway placeholder 解析失败修复，domain service Bean 注册问题

### 2026-05-25 ~ 2026-05-28 — 功能增强

- **feat(order)**: 订单行项支持 + Saga 重构
- **feat(framework)**: 基础设施组件 + POM 注解处理器配置
- **feat(message)**: 模板缓存加载/清理/重置方法
- **test(ai)**: AI 组件测试套件（定价、问答、信用评分）
- **refactor(admin)**: 控制器迁移到 DDD adapter 模式，DTO 转为 record

### 2026-05-17 ~ 2026-05-24 — ES 搜索 & 文件存储

- **feat(product)**: Elasticsearch 集成（分面搜索、ProductSearchQueryPort、ES fallback）
- **feat(application)**: ES 索引管理器、索引适配器、ProductDocument、reindex 服务
- **feat(docker)**: ES 容器 + IK 中文分词插件
- **feat(frontend)**: FacetFilter 分面筛选组件、搜索类型和 Hook
- **feat(upload)**: FileStorage 接口 + LocalFileStorage 实现
- **feat(image)**: 图片处理服务（质量注入、渐进式 JPEG、智能裁剪）

### 2026-05-15 ~ 2026-05-16 — 测试体系 & 管理后台图表

- **test(frontend)**: 完整测试套件（98 文件 / 947 用例）— Vitest + Testing Library + MSW + Playwright E2E
- **feat(admin)**: Dashboard 图表升级（Recharts 趋势图、活动热力图、TopProductsChart）
- **feat(chat)**: STOMP 聊天 Hooks（消息撤回、离线队列）
- **feat(notification)**: 通知中心功能
- **feat(review)**: 管理端审核页面

### 2026-05-09 ~ 2026-05-12 — 管理后台 & 审核系统

- **feat(admin)**: 完整管理面板（暖橙指挥中心设计系统）
- **feat(product)**: 商品审核日志系统（事件驱动持久化）
- **refactor**: 消息/收藏模块迁移到 adapter/application 架构
- **style(frontend)**: UI 组件和动画增强
- **feat(db)**: 索引优化迁移 V4

### 2026-04-27 ~ 2026-05-08 — 核心功能开发期

- **feat(backend)**: Saga 模式、缓存服务、事件基础设施
- **feat(frontend)**: React + Vite + Tailwind 迁移完成
- **feat(frontend)**: 首页内容丰富化、UI 全面升级
- **refactor**: 登录策略模式重构、目录结构重组（Phase 1-4）
- **refactor(backend)**: 核心架构重构、用户认证优化、常量统一

### 2026-04-20 ~ 2026-04-26 — 项目初始化

- **chore**: 初始提交
- **refactor**: 前端目录结构重构
- **feat(backend)**: 后端架构重构和功能优化
- **feat(frontend)**: 前端页面优化和功能完善

---

## 版本说明

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1.0 | 2025-11 | 项目启动，DDD 模块化架构，React 19 SPA 重构 |

---

> 此文件由 Git 提交记录生成。查看完整历史：`git log --oneline`
