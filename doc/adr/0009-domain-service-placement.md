# ADR 0009 — 领域服务按需分布，不以数量对齐模块

- **状态**：接受
- **日期**：2026-08-06
- **决策者**：后端架构
- **标签**：`ddd` `domain-service` `aggregate` `layering`

---

## 上下文（Context）

项目各业务模块的 `domain/service` 数量差异显著：product=1、user=6、order=0、message=1、favorite=0、payment=0。评审与讨论中，这种数量差异容易被误读为质量差异，产生「对齐数量」的重构冲动——例如质疑 user 模块「为什么这么多领域服务」、或反向质疑 product 模块「为什么只有一个」。

现状事实：

1. **富聚合在本项目是普遍形态**：order 的 `Order` 聚合每个状态机方法都返回 `Transition<Order, XEvent>`（`pay()`→`OrderPaidEvent`、`ship()`→`OrderShippedEvent`…）；product 的 `Product` 在聚合方法上产出领域事件（`takeOffline().event()`）；user 的 `User` 聚合承载 `updateContactInfo`/`changePassword`/`recordLogin` 等不变量方法。业务不变量已下沉到聚合，未出现贫血模型。
2. **现有领域服务全部是「跨聚合 / 依赖端口与仓库」的规则**：user 的 `AuthenticationService`/`LoginSecurityService`/`SmsVerificationService`/`PasswordManagementService` 注入 `LoginAttemptPort`/`SmsCodePort`/`PasswordEncoderPort`；product 的 `ProductReportDomainService` 跨举报 + 商品 + 缓存端口编排；message 的 `SensitiveWordFilterService` 是跨消息的净化规则。这些规则天然放不进单个聚合（聚合不允许反问自己的仓库/端口）。
3. **数量差异由领域性质决定**：认证域天然跨聚合（User + 会话 + 失败次数 + 短信），故领域服务多；商品的业务逻辑多为单聚合状态机，故领域服务少。这是领域的产物，不是结构的优劣。
4. **发现一处应用层命令校验泄漏进领域服务**：`ProfileUpdateService.hasAny(...)` 是「更新命令是否携带任何需更新字段」的静态工具，属于应用服务的命令校验，却被放进领域服务。

最优做法既不是把领域服务全部上收，也不是全部下沉进聚合，而是按规则归属分层。若不做任何约定，未来可能被「数量指标」驱动做出负收益重构。

## 决策（Decision）

**领域服务的数量是领域性质的产物，不是质量指标；分层以「业务规则归属」为准，不以模块间数量对齐。**

1. **富聚合优先**。业务不变量、状态机、领域事件产出放聚合内（`Order`/`Product`/`User` 的 `Transition<T, Event>` 模式）。能放聚合的规则不外提。
2. **领域服务只承载「跨聚合 / 依赖端口与仓库」的规则**。规则需要注入 repository/port 表达（认证、锁定、验证码、密码生命周期、举报流、敏感词），无法塞进单个聚合时才建领域服务。
3. **应用层命令校验归属应用服务**。命令级校验（如「命令是否有需更新字段」）是应用服务的私有逻辑，不泄漏进领域服务。本次将 `ProfileUpdateService.hasAny` 迁入 `ProfileAppService` 为私有方法；`isPresent` 因 `validateUniqueContact` 仍在领域层使用而保留于 `ProfileUpdateService`。
4. **禁止以数量对齐模块**。不得为了「看起来一致」而拆散内聚的领域服务或人为上收/下沉，避免负收益 churn。

关键实现：

- 本次迁移：[ProfileAppService.java](../../easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/application/service/ProfileAppService.java)（私有 `hasAny`/`isPresent`）
- 领域服务保留：[ProfileUpdateService.java](../../easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/service/ProfileUpdateService.java)（`validateUniqueContact`）
- 富聚合范式：[Order.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/aggregate/Order.java)（`Transition<Order, XEvent>` 状态机）

核心驱动力：

- 「领域逻辑尽量进聚合、领域服务只留跨聚合」是教科书式现代 DDD 形态，本项目各模块已天然满足，无需改动
- 真正的卫生问题是命令校验泄漏进领域服务，迁入应用层即可，不动领域服务的整体结构
- 以本 ADR 固化判据，避免未来被数量指标误导

## 后果（Consequences）

### 正向后果

- 模块结构反映领域性质，评审中「为什么这个模块服务多/少」不再需要逐次辩解
- 富聚合保真业务不变量并可独立产出领域事件，测试聚焦于聚合方法
- 应用层命令校验归位，领域服务职责纯粹

### 负向后果

- 模块间结构不统一，初次阅读理解每个模块的领域性质存在认知成本
- 领域服务数量差异可能再次被新成员误读为不一致

### 缓解措施

- 以本 ADR 作为 code review 的判据，数量差异按「领域性质」解释而非「质量缺陷」
- 抽查贫血聚合：若某模块出现「领域服务多 + 聚合空壳」，说明规则误上收，需下沉回聚合

## 备选方案（Alternatives Considered）

- **全部规则上收到应用层**：拒绝。丢失领域层可独立测试的认证/密码/净化规则，应用层掺入端口依赖变胖，与六边形架构的 domain 封装冲突。
- **全部规则下沉进聚合**：拒绝。跨聚合唯一性、端口校验、跨消息净化放不进单个聚合（聚合不允许反问自己的仓库/端口），强塞违背聚合边界。
- **按数量对齐各模块领域服务**：拒绝。false metric 驱动的 churn，拆散内聚领域服务的净收益为负（正是本 ADR 要防的反模式）。

## 备注（Notes）

- 相关 ADR：[0002-cqrs-scope-4-modules.md](0002-cqrs-scope-4-modules.md)（CQRS 边界）、[0007-order-local-tx-over-saga.md](0007-order-local-tx-over-saga.md)（拒绝过度设计）
- 相关文档：[doc/架构/架构-DDD规范.md](../架构/架构-DDD规范.md)、`doc/interview/DDD-核心思想轻量落地.md`
- 相关代码：user 认证族（`AuthenticationService`/`LoginSecurityService`/`SmsVerificationService`/`PasswordManagementService`）、product 举报流（`ProductReportDomainService`）、message 净化（`SensitiveWordFilterService`）
- 后续演进触发条件：若某模块出现「领域服务一堆但聚合为数据容器」的贫血模型，重新评估并下沉规则回聚合