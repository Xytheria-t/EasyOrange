# EasyOrange DDD 核心思想轻量落地（面试版）

> 本项目 DDD 是**「展示核心思想、拒绝过度设计」的克制落地**。本文档给你面试讲 DDD 时用：每个核心思想 → 具体文件落点 → 为什么这样是轻量的。与 [话术集.md](话术集.md) 互补——那边是「通用叙事 + 高频追问」，这边是「DDD 专项清单 + 取舍话术」。
>
> 一句话定位：**DDD 六边形 + CQRS，domain → application → adapter 单向依赖**。11 个 Maven 模块、4 个核心聚合根（Product / Order / Payment / User）、33 个 Port 接口编译期隔离、ArchUnit 10 条规则在 CI 守分层不被破坏（注意：老口径写 11，实际代码与 `ArchitectureRulesTest` 自身 javadoc 都是 10）。代码里的聚合根不止 4 个，另有 `Favorite` / `Message` / `OfflineMessage` 三个简单聚合——只有不可变快照字段 + 所有权校验、无状态机，不占讲点。

---

## 1 核心思想清单（讲了能拿分的）

> 面试讲 DDD，别背概念，直接指代码。下面每一条都带落点文件 + 「为什么这是轻量的」——后者才是拉开差距的地方。

### 1.1 聚合根（Aggregate Root）——状态机 + 不变量内聚
- **落点**：`order/domain/aggregate/Order.java`、`payment/domain/aggregate/Payment.java`、`product/domain/aggregate/Product.java`、`user/domain/aggregate/User.java`
- **共性（先说）**：4 个聚合都是**不可变对象**，字段全 `final`，用 `@Builder(toBuilder = true)`「重建新实例」代替原地修改；聚合只做状态机 + 不变量，**不碰持久化、不注入外部依赖**（`domain` 层零框架依赖由 ArchUnit 锁死，见 1.9）。
- **状态机「只有部分聚合有」，有状态机的统一走『动作表』模式，字段按业务裁剪——别一勺烩**：
  - `Order`（订单）——**状态机最完整**：`private Order transitionTo(OrderAction action, String reason)`（一处校验合法性 + 一处应用副作用，见 `Order.java:248`）；公开转换 `pay()/cancel(reason)/forceCancel/ship()/confirmReceipt()/refund(reason)` 全部返回 `Transition<Order, XxxEvent>`（新聚合 + 领域事件一并产出）。
  - `Product`（商品）——**动作表 + 目标驱动**：`ProductAction` 枚举声明 `sources + target`（合法转换唯一事实来源，见 `ProductAction.java`），`ProductStatus.canTransitionTo` 由它派生，聚合根 `private Product transitionTo(ProductAction action)` 统一守卫（见 `Product.java:163`）；`approve()/reject()/putOnline()/takeOffline()` 返回 `Transition<Product, XxxEvent>`，`markAsSold()` 幂等返回 `Optional<Transition<...>>`（订单完成链路重复触发时返回空，不重复产事件）。
  - `Payment`（支付）——**无统一 target 的动作表**：`PaymentAction` 枚举只声明 `sources` 谓词（`PAY/REFUND/CLOSE/FAIL/CONFIRM_PAY/CONFIRM_REFUND`，唯一事实来源，见 `PaymentAction.java`），聚合根 `canPay()/canRefund()/...` 委托 `PaymentAction.X.canApply(status)` 裁决；同一动作可能落到多个目标（确认支付 → SUCCESS/FAILED、确认退款 → REFUNDED/PARTIALLY_REFUNDED）且副作用字段各异，因此动作表不声明 target，目标与副作用由各转换方法内置（`withStatus()/withRefundResult()`）；**终态转换**（`create()/confirmPay()/refund()`）返回 `Transition<Payment, XxxEvent>`，**中间态转换**（`preparePay()→PAYING`、`cancelPay()→PENDING`、`prepareRefund()→REFUNDING`、`cancelRefund()→SUCCESS`）**直接返回裸 `Payment`、不产事件**。
  - `User`（用户）——**没有状态机**：`status` 仅在 `create()` 置为 `NORMAL`，之后不再转换；只有 `updateContactInfo()/changePassword()/changeAvatar()/recordLogin()/assignId()` 这类 toBuilder 变换方法（承载业务不变量，不产领域事件）。
- **为什么轻量**：状态机只给**真正有状态生命周期**的聚合上（Order / Product 强状态、Payment 中等），User 没有状态机就不硬造；规则只在**动作表 enum 一处**写（OrderAction / ProductAction / PaymentAction），字段按业务裁剪，不散在应用层。跨聚合一致性交给事件（见 1.4），不搞聚合内嵌反规范化——聚合保持小而纯。

### 1.2 不变量（Invariant）只写一处
- **落点**：`Order.createOrder()` 里 `BizRequire` 链——不能认领自己、必须有至少一件资产、金额必须 > 0；状态合法性由 `OrderAction.canApply(status, paymentStatus)` 裁决（`OrderAction.java:96`，每个动作声明 `sources/target/targetPaymentStatus/requiresReason/resultCode/paymentGuard`）。
- **为什么轻量**：规则**只写一处**，测试直接打聚合根（`Payment` 聚合根测试 75 个全绿）。不需要额外规则引擎 / Guard 框架，`BizRequire` 一个静态方法就够。状态裁决**统一为动作表 enum**：Order 用 `OrderAction`（含副作用/错误码）、Product 用 `ProductAction`（sources+target）、Payment 用 `PaymentAction`（仅 sources）——「状态合法性由谁说了算」这一问要能答出来。

### 1.3 值对象（Value Object）——按值等价的不可变类型
- **落点**：`OrderId` / `UserId` / `Phone` / `Address` / `OrderNo`（`order/domain/valueobject/`）、`common/domain/ProductId.java`（跨模块共享的 `ProductId` 在 common，不在 order 的 valueobject 包里）、`common/domain/Money.java`（金额不落 `double`，用 `BigDecimal` 避免精度坑）。
- **为什么轻量**：**只对「有规则」的字段做 VO**（ID 的语义、金额的精度、手机号格式），不把每个字段都包一层——避免为了 VO 而 VO 的 ceremony。

### 1.4 领域事件（Domain Event）——状态转换的副产物
- **落点**：`OrderCreatedEvent` / `OrderPaidEvent` / `OrderCancelledEvent`…（order 模块 `domain/event/`），状态转换方法统一返回 `Transition<Agg, Event>`；事务提交后经 **Outbox** 与业务同事务一写（Spring Modulith）。
- **为什么轻量**：事件是「状态转换自然产生的副产物」，不是到处 `new` 事件。跨模块副作用（扣库存、恢复库存、标记售出）由消费者异步触发，聚合不感知。

### 1.5 工厂与重建分离（Factory vs Reconstruction）
- **落点**：`Order.createOrder(spec)`（**跑不变量**）vs `Order.from(spec)`（**不跑不变量**，直接从库重建）。
- **为什么轻量**：明确「新对象 vs 从库重建」两条路径——重建时库里的数据已被校验过，不再重复触发不变量，性能与正确性都对。参数用 record spec 收敛，避免长参数列表。

### 1.6 仓储 + 端口（Repository & Port / 六边形）
- **落点**：`OrderRepository` + `OrderReadRepository`（CQRS 读写分离，见 1.8）、`LockPort` / `PaymentGatewayPort` / `ProductQueryPort`…（**33 个 Port** 编译期隔离）。
- **为什么轻量**：domain 只依赖接口，实现全部放 adapter。依赖方向单向（domain → application → adapter），换实现（Redis/Mem 锁、Rabbit/NATS 总线）只改 adapter，domain/application 一行不动。

### 1.7 领域服务 vs 应用服务（Domain vs Application Service）
- **落点**：领域服务 `message/domain/service/SensitiveWordFilterService.java`（纯规则、无仓储）；应用服务 `message/application/service/OfflineMessageStoreService.java`（**编排**：读在线状态 → 决定是否持久化）。
- **为什么轻量**：**只有不天然属于某个聚合的规则才做领域服务**；涉及读状态 + 调仓储的编排一律放 application。分不清这两个，是 DDD 面试最常见的扣分点。

### 1.8 CQRS（读写分离——4 个模块，两种深度）
- **落点**：`OrderReadModel` / `OrderReadRepository`（`order/domain/readmodel/`）、product 的 `application/query/readmodel/`；范围决策见 [ADR-0002](../adr/0002-cqrs-scope-4-modules.md)。
- **为什么轻量**：CQRS 收益是「读多写少 + 跨聚合聚合 + 读写模型独立优化」。**只在 product / order / payment / message 四个模块做，但深度不同**：
  - **有独立 ReadModel（类型级读写分离）**：product（`application/query/readmodel/` 下 5 个 ReadModel）、order（`domain/readmodel/OrderReadModel`）——ES 全文搜索 / 订单列表跨聚合分页这类复杂查询收益最明显；
  - **只有 Command/Query Handler 级分离（无独立 ReadModel）**：payment、message——`*CommandHandler` / `*QueryHandler` 分文件 + 独立查询仓储（`MessageQueryRepository`），满足「写侧不依赖读侧」的 ArchUnit 守卫，但查询侧与写侧差异没那么大，就不再造 ReadModel；
  - **刻意不做**：favorite（单表索引够用）、user（读写比均衡）——收益 < 维护成本。
  - **注意别把 message 说成「不做 CQRS」**：它做了 Handler 级读写分离，只是没有独立 ReadModel（聊天消息读写比 1:1，读模型独立优化的收益接近 0）。

### 1.9 防腐层 / 依赖治理（用测试守边界）
- **落点**：`ArchitectureRulesTest` **10 条 @ArchTest** 在 CI 阻断（⚠️ 老口径写 11，代码与文件 javadoc 都是 10）：
  ① domain 白名单准入：`domain` 只允许依赖 JDK + `common`（禁框架 / 禁 web / 禁 DTO，合并为 1 条）；
  ②③ CQRS 读写双向隔离：`*CommandHandler` 不依赖 `*QueryHandler`，`*QueryHandler` 也不依赖 `*CommandHandler`；
  ④ 业务模块间只经 `domain.port.*` / `domain.valueobject.*` 通信（覆盖 order/product/message/favorite 四模块）；
  ⑤ 每个 `*Port` 接口必须有 `adapter.outbound` 实现（白名单已清零）；
  ⑥ 禁止 `*.infrastructure.*` 包；
  ⑦ `domain`/`application` 不反向依赖 adapter（已用 `FreezingArchRule` 冻结已知历史豁免）；
  ⑧ Controller 不直连 mapper；
  ⑨ 禁 `System.out` / `System.err`；
  ⑩ 禁 `e.printStackTrace()`。
- **为什么轻量**：用 ArchUnit 测试守分层，**不靠空壳架构 / 冗余包**。规则坏了 CI 直接红，PR 合不进来。

---

## 2 刻意不做 / 轻量化取舍（防「过度设计」扣分话术）

> 面试官真正想听的，不是你会用多少个 DDD 模式，而是**你懂什么时候不该用**。下面每条都是「我评估过、主动拒绝」的决策。

| 没做的 | 为什么不做的理由 | 做了会怎样被扣分 |
|---|---|---|
| **Saga / 2PC / Seata AT/TCC** | 单库部署，一个本地 `@Transactional` 就把订单/扣库存/Outbox 全包进同一事务，比 Saga 最终一致更强（[ADR-0007](../adr/0007-order-local-tx-over-saga.md)） | 挂一个**失效的状态机**，观测价值低于写路径成本，典型「为了架构而架构」 |
| **全模块 CQRS** | 收益 < 维护成本（[ADR-0002](../adr/0002-cqrs-scope-4-modules.md)） | 每模块 Command/Query 双倍代码，纯负担 |
| **Specification 模式** | 支付的状态谓词不是「跨对象业务规则组合」，是转移谓词 | 假模式名露怯 → 已改名 `PaymentStatusGuard`，后统一为 `PaymentAction` 动作表 |
| **每个字段都包 VO** | 只对有规则的字段做（ID/金额/手机号） | ceremony，代码量膨胀 |
| **Milvus / PGVector** | SKU < 10 万，ES kNN 够用 | 向量索引 + ANN 调参一整套运维负担 |
| **聚合内嵌反规范化** | 跨聚合一致性交给事件 + Outbox | 聚合膨胀，单点改死 |

---

## 3 本次 DDD 轻量化清理（审计记录 2026-08）

> 契合「轻量落地」原则，清理掉三处「为了 DDD 而 DDD」的痕迹：

1. **`PaymentSpecification` → `PaymentStatusGuard`**（`payment/domain/aggregate/`）
   - 原包名 `specification/` 撞「Specification 模式」名，但实际只是支付状态转移谓词，且与订单模块 `OrderAction.canApply` 角色重复。改名 + 移包，去掉假抽象名。
2. **`OfflineMessageStoreService` 从 `domain/service` → `application/service`**
   - 它包了个 repository 做「在线判断 → 持久化」，是应用层编排，不是纯领域规则。移出后 `domain/service` 只剩纯领域服务 `SensitiveWordFilterService`，领域/应用职责分清。
3. **`SensitiveWordFilterService` 注释统一中文**
   - 消除模块内中英混用，与全项目一致。
4. **聚合状态机守卫签名对齐**（Order / Product / Payment，2026-08）
   - `Product.transitionTo` 去掉仅为拼错误消息的 `String action` 参数，改单参 `transitionTo(target)`；错误消息由「当前 → 目标」状态 desc 生成，信息量更高。
   - `Payment.cancelPay()/cancelRefund()` 内联 `PaymentStatus.equals(status)` 改走 `canConfirmPay()/canConfirmRefund()` 谓词，守卫单一来源收口 `PaymentStatusGuard`。
   - 消除 Order/Product 同名私有 `transitionTo` 参数反序的观感噪音（其后三形态统一为动作表，见 5）。
5. **状态机三形态统一为动作表**（Order / Product / Payment，2026-08）
   - 原 Order 用 `OrderAction` 动作表、Product 用 `ProductStatus.ALLOWED_TRANSITIONS` 邻接表、Payment 用 `PaymentStatusGuard` 静态谓词——三种表达是**同一方案的三个不完整版本**，不是三种强度。
   - 统一为动作表 enum：`ProductStatus` 删 `ALLOWED_TRANSITIONS`，`canTransitionTo` 由 `ProductAction`（sources + target）派生，聚合根改 `transitionTo(ProductAction)`；删 `PaymentStatusGuard`，`Payment` 经 `PaymentAction.canApply(status)` 裁决。
   - 三模块同一心智模型：**状态 enum 声明状态 + 动作 enum 声明转换（唯一事实来源）+ 聚合根守卫经动作 enum**；字段按业务裁剪（Order 带副作用/错误码、Product 带 target、Payment 仅 sources）。

---

## 4 面试 60 秒 DDD 概括稿

> 直接把下面这段背下来，临场改写即可。

「这个项目是 DDD 六边形 + CQRS 的克制落地。核心是 4 个聚合根——Order、Payment、Product、User，都是不可变对象。**三个有状态机的聚合统一走『动作表』模式**：状态枚举只声明状态，动作枚举是合法转换的唯一事实来源——Order 的 `OrderAction` 最全（sources + target + 支付副作用 + 错误码），单一入口 `transitionTo(action, reason)` 守卫，返回 `Transition<事件>` 发领域事件；Product 的 `ProductAction` 带 target，`transitionTo(ProductAction)` 目标驱动；Payment 的 `PaymentAction` 只有 sources 谓词（同一动作落多个目标，目标由转换方法内置）、终态才产事件；User 没有状态机，只有不变量方法。跨模块副作用（扣库存 / 恢复库存 / 标记售出）由事件 + Outbox 异步触发，聚合不碰持久化。领域层只依赖 Port 接口，33 个 Port 编译期隔离，ArchUnit 10 条规则在 CI 守分层。

我特意做了几个**轻量化取舍**：首先是**拒绝 Saga**——单库部署下本地事务 + Outbox 比 Saga 更强，挂个失效状态机是过度设计；二是 CQRS 只做 4 个模块（product/order 有 ReadModel、payment/message 是 Handler 级），收益小于成本就不做；三是值对象只对有规则的字段做。这些取舍比堆 DDD 模式更能体现架构判断力。」