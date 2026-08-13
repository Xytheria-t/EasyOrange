# ADR 0006 — 跨模块解耦采用 Port/Adapter + Maven optional 隔离，拒绝共享内核

- **状态**：接受
- **日期**：2026-08-08
- **决策者**：后端架构
- **标签**：`ddd` `port-adapter` `module-decoupling` `acl` `modularity`

> **现状更新（2026-08-12）**：Port 数已从决策时点的 33 演化为 **35**（order/payment `LockPort` 收敛为 framework `DistributedLockPort`、`UserInfoPort` 单查下线、admin 查询端口拆分）。本记录保留 2026-08-08 决策时点的 33 口径；现役计数命令见 [doc/工程指标.md](../工程指标.md)。

---

## 上下文（Context）

项目拆分为 11 个 Maven 模块后，跨模块协作需求很快出现，且随业务增长持续增多：

1. **查询侧同步依赖**：order 需要商品快照（`ProductQueryPort`）、user 信息（`UserInfoPort`）；product 需要资产方信息（`SellerInfoPort`）；message 需要用户信息（`UserInfoPort`）；favorite 需要商品信息（`ProductInfoPort`）；admin 需要聚合查询商品/订单/用户（`AdminProductQueryPort` 等）；ai 需要商品搜索（`ProductSearchQueryPort`）。
2. **写操作跨模块副作用**：下单要扣库存（`ProductInventoryPort`）、订单状态变化要发站内信（`MessageNotifierPort`）、支付要回调校验（`CallbackSignatureVerifierPort`）、AI 估值要查信用（`JdbcCreditScoreFetcher` 走 `CreditScoreFetcher`）。
3. **早期风险**：若这些协作直接 import 对方模块的 Mapper / DO / Service 类，模块边界形同虚设——依赖方向不可控、循环依赖必然出现、DDD 分层在模块粒度上失效。
4. **已有先例教训**：`easyorange-order` 曾直接依赖 product 的 mapper 做库存扣减，导致「订单模块知道商品表的列」；后续重构才收敛。

约束：

- domain 层零框架依赖是项目铁律（ArchUnit 规则 1），跨模块协作接口不能引入 Spring/MyBatis 类型
- 各模块可独立编译、独立测试；Maven 依赖尽量瘦身
- 写操作优先走事件驱动（Outbox），查询操作保留同步调用（低延迟、无最终一致窗口）
- 不得引入模块间消息总线的「服务注册中心」（如 Spring Cloud、Dubbo）——单 JVM 多模块部署，那是过度设计

## 决策（Decision）

**跨模块协作统一采用「调用方模块定义 Port 接口 + 应用模块提供 Adapter 实现 + Maven `<optional>true</optional>` 编译期隔离」模式；拒绝共享内核（Shared Kernel）。**

具体规则：

1. **Port 归调用方**：谁需要数据/能力，谁在自己模块的 `domain/port/`（出站）或 `application/port/query/`（读侧）定义接口，签名只用 JDK 类型 + 本模块值对象。
2. **Adapter 归应用模块**：`easyorange-application` 的 `adapter/outbound/` 下集中实现所有跨模块 Port（`ProductInventoryAdapter`、`ProductQueryAdapter`、`SellerInfoAdapter`、`MessageUserInfoAdapter`、`FavoriteProductInfoAdapter`、admin 四个 `Admin*QueryAdapter` 等），实现类标 `@Primary`（IntelliJ 误报 + 多实现冲突规避，见后端 AGENTS.md 踩坑警示）。
3. **Maven `<optional>true</optional>`**：业务模块之间的依赖全部 optional——编译期可见、运行时/传递依赖不可见，ArchUnit 在包级别兜底（规则 4/6）。
4. **写操作事件化**：跨模块写副作用一律走领域事件 + Outbox（`OrderCreatedEvent` → 扣库存、完成/取消 → 恢复库存），不允许同步跨模块写（[ADR-0007](0007-order-local-tx-over-saga.md) 的本地单事务内只保留同事务必需的同步端口调用）。
5. **ACL 语义**：跨模块只能看到 Port 接口与值对象，看不到对方聚合根/DO/Mapper——这就是防腐层的最小形态（接口即契约）。

关键实现：

- 隔离模式：[ProductInventoryPort.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/port/ProductInventoryPort.java)（order 定义）+ [ProductInventoryAdapter.java](../../easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/product/ProductInventoryAdapter.java)（application 实现）
- 读侧端口：[ProductQueryPort.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/port/ProductQueryPort.java)
- 守卫：[ArchitectureRulesTest.java](../../easyorange-backend/easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java)（规则 4/6：业务模块仅经 `domain.port.*` / `domain.valueobject.*` 通信 + 端口必有适配器）

核心驱动力：

- **依赖方向由编译期强制**：optional + ArchUnit 双保险，模块边界不是口头约定
- **写事件化 + 读同步化**：把「必须同步的一致性」和「可以异步的副作用」分开，避免为了统一而牺牲一致性或延迟
- **单 JVM 场景拒绝服务注册中心**：接口 + Spring 容器装配足以表达协作，任何微服务中间件都是负收益（与 [ADR-0005](0005-messaging-rabbitmq.md) 的「量未到不上重型中间件」同一判断标准）

## 后果（Consequences）

### 正向后果

- 模块可独立编译/测试/演进，跨模块依赖面收敛为接口签名
- 替换实现零成本：锁实现（Redis/Mem）、支付网关、短信供应商、ES 开关全部只改 adapter（`@ConditionalOnProperty` 已用于 RabbitMQ/ES/TokenBudgetStore）
- 33 个 Port 成为「模块边界地图」，新人看 Port 目录即理解模块协作面
- optional 依赖 + ArchUnit 无白名单，CI 阻断任何越界依赖

### 负向后果

- 每个跨模块调用多一层「接口 + 适配器」样板代码（33 Port 对应 ~33 实现类）
- Adapter 集中堆在 application 模块，该模块文件数偏多，导航成本上升
- 查询链路多一跳方法调用 + 可能的 MapStruct 转换开销（可忽略，本地调用）
- optional 标记依赖人肉维护，Maven 不校验（TD-011 技术债）

### 缓解措施

- 33 Port 目录即边界地图；新增 Port 有 ArchUnit「端口必有适配器」规则自动兜底（缺实现直接红）
- 查询端口尽量复用值对象直传，避免无意义 DTO 拷贝
- 未来若 adapter 膨胀，可按域拆 `adapter/outbound/{domain}/` 子包（已按此组织：elasticsearch/payment/product/user/admin）

## 备选方案（Alternatives Considered）

- **共享内核（Shared Kernel）**：把跨模块共用的 DO/Mapper/Service 放一个公共模块供直接引用。拒绝——DO/Mapper 泄漏领域实现细节，模块边界退化为「包边界」，DDD 分层在模块粒度失效；历史教训正是 order 直接扣 product 库存导致「知道对方表列」。
- **统一服务注册 / 微服务化**（Spring Cloud / Dubbo / RPC）：拒绝——单 JVM 多模块部署下引入网络序列化、注册中心、治理组件，成本远高于收益；模块边界用 Port + Spring 容器已足够。
- **事件化一切（读写都走消息）**：拒绝——查询同步调用延迟低、无最终一致窗口、无消息积压观测负担；只事件化写副作用。
- **把所有跨模块代码收进 application 一个模块**：拒绝——模块变「大泥球」，业务模块失去独立演进能力，与 DDD 分层目标背道而驰。

## 备注（Notes）

- 相关 ADR：[0002-cqrs-scope-4-modules.md](0002-cqrs-scope-4-modules.md)（CQRS 边界）、[0005-messaging-rabbitmq.md](0005-messaging-rabbitmq.md)（消息中间件选型）、[0007-order-local-tx-over-saga.md](0007-order-local-tx-over-saga.md)（本地单事务 + 端口同步调用边界）
- 相关文档：[doc/架构/架构-DDD规范.md](../架构/架构-DDD规范.md)、[后端 AGENTS.md](../../easyorange-backend/AGENTS.md)「跨模块通信」节
- 相关代码：`easyorange-application/adapter/outbound/` 全部适配器、`ArchitectureRulesTest.java` 规则 4/6
- 后续演进触发条件：若模块数量继续增长、adapter 层超 60 文件，评估按域拆 adapter 子模块；若拆分独立部署（多 JVM），Port 演进为 Feign/gRPC 契约（见 ADR-0007 的演进触发条件）
