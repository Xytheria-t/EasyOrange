# Changelog

> 基于 Git 提交记录自动生成

## [unreleased]

### 2026-07-01 — Nginx 现代化安全响应头整合

- **feat(infra)**: 新增 `security-headers.conf` 统一管理安全响应头 — CSP (`default-src 'self'` + `style-src 'unsafe-inline'` + `img-src data: blob:`)、X-Content-Type-Options、X-Frame-Options、Referrer-Policy、Permissions-Policy；删除已废弃的 X-XSS-Protection；所有 location 块通过 `include` 继承，消除不一致
- **refactor(infra)**: nginx.conf 安全头从重复散落改为 `security-headers.conf` 片段统一 include；HSTS 仅保留于 HTTPS server block；Dockerfile 同步复制新文件

### 2026-06-30 — 移除前端深色模式切换代码

- **refactor(frontend)**: 移除 `ProfilePreferences` 中的主题外观卡片（浅色/深色/自动切换 UI）及关联 CSS
- **cleanup**: 清理 `tokens.css` 中残留的 `--glass-bg-dark` 变量；更新 `profile/codemap.md` 描述

### 2026-06-30 — 前端组件库收尾、可访问性与代码整洁度修复

- **refactor(frontend)**: `shadcn-button.tsx` 重命名为标准 `button.tsx`，全项目导入统一为 `@/components/ui/button`；`Button` 组件支持 `isLoading` / `loadingText`，修复 `asChild` 子元素渲染异常
- **refactor(frontend)**: 完成剩余原生表单控件迁移 — `EditProductPage` / `PublishPage` 类别与成色改为 shadcn `<Select>`；`CategoryManagePage` 排序输入改为 `<Input>`、状态切换改为 `<RadioGroup>`；`FavoritesPage` 复选框改为 `<Checkbox>`
- **fix(frontend)**: Checkbox 边框可见性修复 — 默认边框从 `border-border` 改为 `--border-control` token（`rgba(42,37,32,0.35)`），解决登录/注册/协议勾选框几乎不可见的问题
- **fix(frontend)**: 修复 `ProductDetailDrawer` / `ProductDetailPage` 中重复的 `@/components/ui` 导入；`InputProps` / `TextareaProps` 由空接口改为 type alias，消除 `no-empty-object-type` 报错
- **fix(a11y)**: 为 `CategoryManagePage` 和 `SearchPage` 中可点击的自定义选项/标签添加 `role="button"`、`tabIndex` 与 `onKeyDown` 键盘支持，满足 jsx-a11y 规范
- **chore(frontend)**: 全量运行 `eslint --fix`，自动修复 39 个 curly 风格错误；手动清理 3 处 `console.error` 调试残留，生产代码不再输出 console
- **test**: 前端 `typecheck` + `lint:check`（0 errors，75 warnings）+ `test`（100 测试文件 / 953 用例）+ `build` 全部通过

### 2026-06-29 — 前端 shadcn/ui 组件化迁移与 CSS 清理

- **refactor(frontend)**: 全面迁移到 shadcn/ui 组件库 — 原生 `<button>` → `<Button>`、`<input>` → `<Input>`、`<textarea>` → `<Textarea>`、`<select>` → 保留原生（测试兼容性）、checkbox → `<Checkbox>`、switch → `<Switch>`、label → `<Label>`
- **refactor(frontend)**: 清理大量废弃 CSS — 从 `src/styles/main.css` 移除约 200 行未使用规则（`.glass-input`、`.auth-btn`、`.form-input`、`.checkbox-custom`、`.switch-slider` 等）；从 `src/admin/styles/admin.css` 移除约 220 行（`.admin-btn`、`.admin-card`、`.admin-table`、`.admin-input`、`.admin-select`、`.admin-badge` 等）；删除未引用的 `src/admin/styles/admin-pages.css`
- **fix(css)**: 修复 `global.css` 中未分层的基础 reset 规则（`* { padding: 0 }`、`button { border: none }` 等）覆盖 Tailwind utilities 的问题 — 将所有 reset 规则移入 `@layer base`，解决登录/注册界面输入框 padding 失效和 checkbox 边框不可见问题
- **fix(frontend)**: `FilterSidebar` 分类/条件筛选原生 checkbox 替换为 shadcn `<Checkbox>`；`ProfilePreferences` 开关替换为 shadcn `<Switch>`；`ChatInputBar` / `PublishPage` textarea 替换为 shadcn `<Textarea>`；管理后台表单控件全部统一为 shadcn 组件
- **test**: 前端 typecheck + 100 测试文件 / 953 用例全部通过 + build 通过

### 2026-06-25 — 副标敲定:"砍业务,撑架构"

- **refactor(brand)**: 副标从"业务是容器,架构才是主角"精炼为 6 字对仗版 "**砍业务,撑架构**"。长版副标"业务做减法,架构做加法"作为展开说明保留
- **refactor(docs)**: `README.md` / `CLAUDE.md` / `AGENTS.md` / `PRODUCT_DIRECTION.md` / `index.html` 顶层副标全部统一为"砍业务,撑架构",主标题统一改为"EasyOrange — 砍业务,撑架构"
- **docs(presentation)**: 比赛答辩 / 面试推荐开场 hook —— "别人堆功能,我压架构",引出"为什么砍 + 砍了什么 + 换来什么"5 分钟故事线

### 2026-06-25 — 项目定位重塑:技术 demo 项目

- **refactor(brand)**: 项目定位从"AI 资产管理产品"重塑为"DDD + CQRS + Saga + 事件驱动 + AI 多模态 全栈架构 demo"。业务场景（C2C 资产流转）从主角调整为"业务容器"，架构与工程成为主角
- **refactor(docs)**: `README.md` 重写为技术 demo 叙事（技术亮点 + 11 Maven 模块 + 业务简化原则 + 业务是容器、架构才是主角）
- **refactor(docs)**: `PRODUCT_DIRECTION.md` 改名为《业务场景说明》，明确说明业务简化原则 + 业务不是商业计划书
- **refactor(docs)**: `doc/集成/AI-资产管理.md` 改名为《AI 能力清单 — 详细机制》，聚焦 AI 架构侧关注点（端口抽象、缓存装饰、限流降级）
- **refactor(docs)**: `CLAUDE.md` / `AGENTS.md` 顶部项目说明同步更新；AGENTS.md 中"AI 资产管理 工作流"小节改名为"AI 能力清单"并补充 AI 架构侧关注点
- **refactor(frontend)**: `easyorange-frontend/index.html` meta description / keywords / title 更新为"DDD + CQRS + Saga + 事件驱动 + AI 多模态 全栈架构 demo"

### 2026-06-25 — 议价 / 阶梯降价功能下线

- **refactor(product)**: 移除 `OfferRuleEngine` / `OfferAppService` / `OfferResult` / `OfferDecision` / `ConsignmentMode` 枚举 / 议价领域事件 (`PriceAdjustedEvent` / `OfferAcceptedEvent` / `OfferRejectedEvent` / `CounterOfferMadeEvent`)；`Product` 聚合根移除 `floorPrice` / `consignmentMode` / `listedAt` / `currentPriceLevel` 字段及对应调价方法
- **refactor(product)**: 移除 `NegotiationMessagePort` / `OrderCreationPort`（AI 自动成单 port）跨模块接口
- **refactor(product)**: 移除 `ProductPriceAdjustTask` 阶梯降价定时任务与对应 Bean 注册
- **refactor(message)**: 移除 `OfferMessageType` 枚举 / `OfferProcessingPort` / `@MessageMapping("/offer.make")` WebSocket 端点
- **refactor(application)**: 移除 `AiOrderCreationAdapter` / `OfferProcessingAdapter` 跨模块编排适配器
- **refactor(ai)**: 移除 `DeepSeekNegotiationMessageAdapter` 议价话术 LLM 实现
- **refactor(frontend)**: 移除出价弹窗 / 还价弹窗 / 议价 UI / `useOfferSocket` 实时推送 hook
- **refactor(sql)**: Flyway 迁移 `V4__add_ai_consignment_fields.sql` 改写为 DROP COLUMN 移除 `floor_price` / `consignment_mode` / `listed_at` / `current_price_level` 字段及 `idx_product_ai_managed` 索引
- **refactor(docs)**: `AGENTS.md` / `CLAUDE.md` / `README.md` / `DATABASE.md` / `PRODUCT_DIRECTION.md` / `doc/集成/AI-资产管理.md` / `doc/集成/API-速查.md` 同步移除议价 / 阶梯降价 / 寄售模式相关描述
- **fix(index)**: `easyorange-frontend/index.html` meta / title 移除"AI 议价"关键词
- **fix(sql)**: `R__seed_message_templates.sql` / `R__insert_dev_test_data.sql` 欢迎语移除"议价"相关文案
- **test**: 后端 11 模块全量回归 + 前端 tsc + vitest

### 2026-06-25 — AI 资产管理定位回滚（好物→资产）

- **refactor(brand)**: 品牌定位回滚 —— "AI 好物管家" → "**AI 资产管理**"(EasyOrange — AI 资产管理 · 让每一份资产,运转不息)
- **refactor(naming)**: 文案回滚 —— 好物 → 资产 / 管家 → AI 资产管家 / 买家 → 认领方 / 卖家 → 资产方
- **refactor(frontend)**: `HeroSection` 主标题 "让每一份价值,物尽其用" → "让每一份价值,运转不息";副标改回 "AI 资产管理 · 把资产交给我们,管家替你估值、写描述、自动调价"
- **refactor(frontend)**: `Footer` / `LoginPage` / `HomePage` / `AIFeaturesSection` / `ProductDetailPage` 同步资产方/认领方术语
- **refactor(backend)**: 后端 Java 实体 / 领域事件 / Port 接口 / Service / 测试 —— 卖家→资产方、买家→认领方、好物→资产
- **refactor(sql)**: Flyway 迁移脚本 `R__seed_categories.sql` / `R__seed_message_templates.sql` / `V1__init_schema.sql` / `R__insert_dev_test_data.sql` 全部回滚到资产方/认领方术语
- **refactor(docs)**: `doc/集成/AI-替卖家运营.md` → `AI-资产管理.md`;`PRODUCT_DIRECTION.md` / `CLAUDE.md` / `DATABASE.md` / `AGENTS.md` 同步
- **refactor(admin)**: 管理后台订单/商品 `AdminOrder` 字段文案 —— 买家/卖家 → 认领方/资产方(全平台术语统一)
- **test**: 前端测试 + 后端测试 全部同步更新

### 2026-06-25 — AI 好物管家定位重塑

- **refactor(brand)**: 全平台品牌重塑 — "AI 替卖家运营" → "**AI 好物管家**"(EasyOrange — AI 好物管家 · 让每一份价值,物尽其用)
- **refactor(naming)**: 文案统一替换表 —— 资产 → 好物 / 议价 → 管家 / 智能助手 → AI 好物管家
- **refactor(frontend)**: `AIFeaturesSection` 完全重写 —— 从"4 张静态卡片"改为"双列管道 + 管家日报"(`StewardDailyReport` + `PipelineStepRow`)
- **refactor(frontend)**: `HeroSection` 主标题 —— "让闲置流转,让价值延续" → "让每一份价值,物尽其用";副标 "设一个底价,AI 替你议价、改价、撮合" → "AI 好物管家 · 设一个底价,管家替你估值、写描述、自动调价"
- **refactor(frontend)**: `Footer` / `LoginPage` / `HomePage` 同步新定位
- **refactor(docs)**: `PRODUCT_DIRECTION.md` v1.0 → v2.0 —— 4 决策点扩为 6 决策点(双端对称),新增"AI 信用画像" + "AI 智能找货" + "AI 物品评估"
- **refactor(docs)**: `README.md` 顶部"AI 替卖家运营的 C2C 平台" → "AI 好物管家 · 让每一份价值,物尽其用"
- **test**: `HeroSection.test.tsx` + `Footer.test.tsx` 文案同步

### 2026-06-24 — 知识库整理（neat-freak）

- **docs**: 新建 `doc/集成/` 目录，含 `AI-替卖家运营.md`（4 决策点/议价规则/阶梯降价/WebSocket 协议）和 `API-速查.md`（全模块 REST+WebSocket 端点）
- **docs**: AGENTS.md（315→261 行）— 详细机制迁出，改为"核心约定 + 深入文档指针"
- **docs**: AGENTS.md 新增"集成文档"指针表
- **docs**: README.md — 删除过期引用（`easyorange-shared/` `easyorange-miniprogram/` 这两个不存在的目录），更新项目结构图（含 `doc/集成/` + `PRODUCT_DIRECTION.md` + `codemap.md`）
- **docs**: CLAUDE.md — 项目结构区加上"详细目录见 codemap/AGENTS"指针

### 2026-06-24 — AI 替卖家运营 + 品牌重塑

- **feat(product)**: AI 替卖家运营功能全量上线 — 规则引擎议价 `OfferRuleEngine` + LLM 话术 `DeepSeekNegotiationMessageAdapter` + 阶梯降价 `ProductPriceAdjustTask` + 议价事件消费者 `OfferEventConsumer`
- **feat(websocket)**: 议价 WebSocket 协议扩展（MessageType 6-9: OFFER/OFFER_ACCEPTED/OFFER_REJECTED/COUNTER_OFFER）
- **feat(frontend)**: 前端完整议价交互（AI 托管开关 + 出价弹窗 + 还价弹窗 + 阶梯降价指示条 + useOfferSocket 实时推送）
- **feat(application)**: 跨模块编排适配器（AiOrderCreationAdapter + OfferProcessingAdapter）
- **refactor(product)**: NegotiationMessagePort 从 ai 模块移至 product 模块解决循环依赖
- **refactor(brand)**: 全平台品牌重塑 — "资产流转平台"→"EasyOrange — AI 替卖家运营的 C2C 平台"（32 文件）
- **test**: 后端新增 ~146 测试用例（总计 1,269），前端 945 测试用例全部通过

### 2026-06-23 — 错误码体系精简与一致性优化

- **refactor(common)**: `IResultCode.mapToHttpStatus()` 静态方法移至 `GlobalExceptionHandler`（framework），`BaseBusinessException.httpStatus()` 方法删除，common 模块不再依赖 `org.springframework.http.HttpStatus`
- **refactor(common)**: `FileException` 构造器从 `public` 改为 `protected`，统一使用 `FileException.of(...)` 工厂方法（与 `BusinessException` 模式对齐）
- **refactor(common)**: `BizRequire` 新增 `notBlank`/`notEmpty` 的 `IResultCode` 重载（此前仅 `notNull`/`requireTrue` 支持）
- **refactor(framework)**: `GlobalExceptionHandler` 所有异常处理器统一返回 `ResponseEntity` + 正确 HTTP 状态码（校验类错误此前返回 HTTP 200，现统一返回 400）
- **refactor(framework)**: `extractDuplicateFieldMessage` 移除硬编码 DB 约束名（`uk_eo_user_email` 等），改为通用兜底消息
- **refactor(product)**: 6 个异常类改用 `ProductResultCode` 专属错误码（此前回退到全局 `B0002`），新增 `REPORT_NOT_FOUND(B2007)`/`REPORT_ERROR(B2008)`/`PRODUCT_STATUS_INVALID(B2009)`
- **refactor(message)**: `MessageDomainException.defaultCode()` 改用 `MessageResultCode.MESSAGE_DOMAIN_ERROR(B7008)`（此前回退到全局 `B0002`）

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
