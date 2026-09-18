# EasyOrange 为什么上 DDD + 六边形：收益与代价

> 只回答三件事：为什么这么分层、拿到了什么、付出了什么。怎么做见 [架构-DDD规范.md](架构-DDD规范.md) / [架构-系统架构.md](架构-系统架构.md)；具体决策见 [ADR-0002](../adr/0002-cqrs-scope-4-modules.md) / [ADR-0006](../adr/0006-module-decoupling-port-adapter-acl.md) / [ADR-0007](../adr/0007-order-local-tx-over-saga.md) / [ADR-0009](../adr/0009-domain-service-placement.md)；数字口径以 [工程指标.md](../工程指标.md) 为唯一来源。

**一句话**：让「业务规则」有唯一落点、让「模块边界」能被测试强制；代价是每个跨模块协作多一层接口 + 适配器，以及模块间结构不统一。

---

## 一、驱动因素：MVC 三层为什么不够

| 驱动 | 项目里的具体形态 | MVC 三层会怎样 |
|---|---|---|
| 业务有真规则，要单点守卫 | 商品 6 状态生命周期，两条进 ONLINE 的路径（`approve` / `putOnline`）必须过同一组 `validateOnline()`；订单是「订单状态 × 支付状态」二维状态机，`OrderAction` 动作表是唯一事实来源；支付 6 个状态谓词收口 `PaymentStatusGuard` | 规则散在多个 Service 用例里，同一个不变量各写一份，改一处漏一处 |
| 模块边界要可执行 | 11 个 Maven 模块，跨模块只看得见 Port 接口与值对象 | 包结构拦不住 `import` 对方 Mapper。历史教训：order 曾直接依赖 product 的 mapper 扣库存（订单模块知道商品表的列），后来才重构回 Port |
| 外部依赖多，要可换可降级 | LLM 供应商三套（DeepSeek / Qwen-VL / DashScope Embedding），ES 可关（关掉走 LIKE 兜底适配器），RabbitMQ 可关，支付网关、短信；全仓 26 处 `@ConditionalOnProperty` 条件装配 | 业务代码里 `new` 客户端，换供应商 = 改业务代码 |
| 领域逻辑要能快速测 | domain 零框架依赖 → 聚合根用例是纯 JUnit；PIT 只对 domain 注入变异（765 个变异，约 3 分钟） | 规则写在 Service 里，测试要起 Spring 上下文 + mock 一圈依赖 |
| 项目要展示真实工程能力（作品集诉求） | 分层 / 边界 / CQRS 都是可被 ArchUnit 验证的现状，不是文档里的空壳 | 三层 MVC 也能写业务，但展示不出边界治理；这条是项目诉求，不是技术必要性 |

---

## 二、收益（都能核验）

| 收益 | 证据 |
|---|---|
| 不变量只写一处 | 建单校验走 `Order.createOrder` 的 `BizRequire` 链 + `OrderAction.canApply`；商品两条上架路径共用 `validateOnline()`；支付状态判断全收口到 6 个谓词，无散写 |
| 依赖方向由 CI 强制，不靠自觉 | ArchUnit 12 条规则在 `./mvnw test` 内，失败即 CI 红；domain 白名单准入（只准 JDK + `common`）；domain/application 禁 `org.springframework.dao`；禁反向依赖 adapter（历史债用 `FreezingArchRule` 冻结，不新增） |
| 模块边界 = 接口签名 | 46 个 Port 接口 + 跨模块 Maven 依赖全 `<optional>true</optional>` + 适配器集中 `easyorange-application/adapter/outbound/`；Port 目录本身就是模块协作地图 |
| 换实现 / 降级只动 adapter | ES 关闭时 `KnowledgeFallbackAdapter` 走 LIKE 兜底；MQ 可关应用照起；AI 供应商切换 = 改 `AiModelConfig` 的 options；手写多级缓存整体换成 Spring Cache 单层（2026-08-13）；自研 AI Port / 适配器 / 装饰器全部删除换 Spring AI（ADR-0008） |
| 领域层可独立测试 | domain 行覆盖 88.9%；PIT 765 变异 / 约 3 分钟；聚合根用例不启动 Spring |
| 读路径可独立优化 | product 5 个 ReadModel 承载 ES 全文搜索 + facets，order 2 个承载订单列表分页；payment / message 只做 Handler 级分离（ADR-0002） |
| 跨模块读可以「自持快照」换解耦 | 订单列表读 `eo_order_item.product_snapshot`，商品改名 / 删除不影响已下订单，跨模块读只剩 `ProductInventoryPort`（ADR-0006，2026-09-16 更新） |
| 演进有量化触发条件 | 拆独立部署 → Port 变 Feign/gRPC 契约（ADR-0006）；拆独立数据源 → 重新评估 Saga（ADR-0007 / [ADR-0010](../adr/0010-order-saga-evolution-plan.md)，订单写 QPS > 5k/s 且持续） |

---

## 三、代价（都是实际付了的）

| 代价 | 具体表现 | 缓解 / 现状 |
|---|---|---|
| 每个跨模块协作多一层样板 | 每处跨模块协作都要配一个 Port 接口 + 一个适配器实现，多数为 1:1；适配器集中在 application 模块，该模块文件数偏多 | 接受（已记录在 ADR-0006 负向后果）；适配器按域分子包（elasticsearch / payment / product / user / admin）降低导航成本 |
| 模块间结构不统一 | CQRS 只有 4 个模块有 command / query；领域服务数量 product=1、message=1、user=7、order / payment / favorite=0 | 写进 ADR-0002 / 0009 作为评审判据：结构差异是领域性质的产物，不按数量对齐 |
| 双层模型转换 | DO ↔ 聚合根（Assembler / MapStruct）；CQRS 读模型有字段漂移风险（写侧加字段、读侧忘加） | Assembler 统一转换；漂移无自动校验，靠 code review |
| 定位成本（间接性） | 查「这个字段谁写的」要跨 Port → Adapter → Repository 三跳 | 接受；Port 命名即地图 |
| 学习门槛 | 要记住「领域服务 vs 应用服务」判据、每模块一个异常根、VO 只对有规则的字段做、端口必有适配器 | 规范文档 + ArchUnit 直接报错定位；评审有现成判据 |
| 最容易翻车的地方是「装饰性 DDD」 | 空壳 CommandHandler、逐字段 VO、为分层而分层 | 用「刻意不做的清单」防（ADR-0002 / 0009；[01-怎么答.md §二-2](../interview/01-怎么答.md)） |
| `<optional>` 依赖靠人肉维护 | Maven 不校验 optional | TD-011：已由 CI 脚本闭环（除组合根 `easyorange-application` 外，跨领域模块依赖必须 optional，违规即失败） |

---

## 四、判据：什么情况下这套是负收益

- 单表 CRUD、无跨聚合不变量 → 直接 AppService + Mapper。favorite 就是这样：单表 `WHERE user_id = ?` 加索引够用，不上 CQRS。
- 读写比均衡、无独立查询维度 → 不上 CQRS（user）。
- 聚合没有状态生命周期 → 不硬造状态机（User 只有不变量方法，没有状态机）。
- 字段没有规则 → 不包 VO（只对 ID 语义 / 金额精度 / 手机号格式做）。
- **框架 / 标准库已有能力 → 不自研抽象**。这条是本项目最贵的一课：AI 侧曾自研 Port、适配器、缓存装饰器与 DTO（ADR-0003），框架成熟后全部删掉换 Spring AI（ADR-0008）——六边形架构不等于「什么都自己包一层」，零新增自定义代码才是最优（STP）。
- 单 JVM、单团队 → 不拆微服务（ADR-0006），拆分触发条件量化在 ADR-0010。

---

## 五、被拒绝的替代方案

| 方案 | 拒绝理由 |
|---|---|
| MVC 三层（Controller / Service / DAO） | 边界不可执行（历史教训：order 直接依赖 product 的 mapper）、不变量散落、无法用测试守住 |
| 共享内核（跨模块共用 DO / Mapper / Service） | DO 泄漏表结构，模块边界退化为包名（ADR-0006） |
| 微服务 + RPC / 注册中心 | 单 JVM 下独立部署与独立扩容收益为零，换来分布式一致性与运维成本（ADR-0006 / 0007） |
| 全模块 CQRS、每个字段包 VO、Specification 模式 | 收益 < 维护成本（ADR-0002；完整清单见 [01-怎么答.md §二-2](../interview/01-怎么答.md)） |
| 自研 AI Port / 适配器 / 装饰器 | 与框架能力重复，手写代码即负债（ADR-0008；ADR-0003 已被替代） |

---

## 六、口头版（60 秒内用）

「MVC 三层按技术切，规则会散、边界靠自觉；我按领域切：不变量下沉聚合根（订单状态机、商品上架校验、支付守卫），domain 零框架依赖只依赖接口，跨模块只经 Port + 值对象——46 个 Port 接口加 ArchUnit 12 条规则在 CI 里强制边界。收益是规则单点、换实现和降级只改 adapter、领域层能纯单测；代价是每个跨模块调用多一层接口和适配器、模块间风格不统一、有学习门槛。所以 favorite / user 这类简单场景我刻意不上 CQRS，AI 侧干脆把自研 Port 删了换 Spring AI——六边形不是什么都自己包一层。」

---

## 相关文档

- 规范细则（聚合根 / 值对象 / 领域服务 / 仓储 / 事件 / 异常 / ACL）→ [架构-DDD规范.md](架构-DDD规范.md)
- 分层与包结构约定 → [架构-系统架构.md](架构-系统架构.md) §三
- 决策记录 → [ADR-0002](../adr/0002-cqrs-scope-4-modules.md)（CQRS 范围）、[ADR-0006](../adr/0006-module-decoupling-port-adapter-acl.md)（跨模块解耦）、[ADR-0009](../adr/0009-domain-service-placement.md)（领域服务分布）
- 面试话术 → [01-怎么答.md §二](../interview/01-怎么答.md)（DDD 核心思想 + 刻意不做清单）
