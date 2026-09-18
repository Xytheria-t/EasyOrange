# EasyOrange DDD 领域设计规范

> 本文档定义 DDD 领域设计的**约定与判据**；代码怎么写读代码，不复述实现。
>
> 「为什么上 DDD / 六边形、收益、代价、什么情况下是负收益」见 [架构-DDD选型取舍.md](架构-DDD选型取舍.md)；分层与包结构见 [架构-系统架构.md](架构-系统架构.md)；错误码全貌见 [架构参考.md](../agents/架构参考.md)；分层规则的可执行版本见 [`ArchitectureRulesTest`](../../easyorange-backend/easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java)（类 javadoc 是规则清单的单一来源）。

---

## 一、聚合根

**职责**：保证聚合内业务不变量；作为聚合唯一访问入口；通过工厂方法创建，创建时即满足不变量。

**实现约定**：

- Lombok `@Builder(toBuilder = true)` + 全 `final` 字段，`toBuilder()` 返回新实例实现"修改"（例：`User.changePassword()`）
- 一个事务只修改一个聚合根；聚合间通过 ID 引用，不直接持有其他聚合引用
- 聚合只做状态机 + 不变量，**不碰持久化、不注入外部依赖**

```java
@Getter
@Builder(toBuilder = true)
public class User {
    private final String id;
    private final Credentials credentials;   // username + encodedPassword
    // ...

    public static User register(String username, String encodedPassword, String nickName) { /* 跑不变量 */ }
    public User changePassword(String encodedNewPassword, String operatorId) {
        return this.toBuilder().credentials(this.credentials.changePassword(encodedNewPassword)).build();
    }
}
```

**状态机只给真有生命周期的聚合上**：`Order` / `Product` 强状态（`transitionTo` 单一入口 + 动作表/转换表），`Payment` 中等（`PaymentStatusGuard` 谓词 + 终态才产事件），`User` 无状态机就不硬造。

## 二、值对象

强制要求：**不可变**（`record` 或全 `final` 字段）、**按值等价**（`equals`/`hashCode`）、**自验证**（构造时校验业务规则，如 `ContactInfo` 校验邮箱格式）、**替换而非修改**（`withEmail()` 返回新实例）。

**只对"有规则"的字段做 VO**（ID 语义 / 金额精度 / 手机号格式），不把每个字段都包一层——那是 ceremony。

## 三、领域服务 vs 应用服务

| 维度 | 应用服务 | 领域服务 |
|-----|---------|---------|
| 职责 | 用例编排、事务管理、端口协调 | 纯业务规则、多聚合协调、聚合持久化 |
| 依赖 | 领域服务 + 输出端口 | 仅领域模型 + 仓储接口 |
| 框架 | 可含 `@Transactional` 等注解 | **绝对无框架依赖** |
| 返回值 | DTO / Response | 聚合根 / 值对象 |
| 拆分原则 | **按用例拆分**，一个类一个完整场景 | **按业务领域拆分**，一个类一个业务子域 |

**应用服务**：避免上帝类，但依赖集与事务边界一致的用例应聚合（`AuthAppService` 处理注册/登录/登出/刷新/忘记密码/修改密码）。判据：**一个应用服务的构造函数变更不应强迫修改不相关的调用方**。命名 `{领域}AppService`；每个方法就是一个完整事务边界。

**领域服务**：无状态、无框架调用、无数据库操作；自己完成聚合持久化（`return userRepository.save(user)`，而不是把聚合丢回应用层保存）；所有依赖走构造器注入且必须是接口；依赖数量理想 1-2 个，4 个及以上考虑拆分。

**归属判断表**：

| 场景 | 归属 | 原因 |
|------|------|------|
| `user.changePassword()` / `user.recordLogin()` | 聚合根 | 只涉及 User 自己的状态变更 |
| 检查用户名是否已存在 | 领域服务 | 需要查询**其他** User 聚合 |
| 密码加密比对 | 领域服务 | 涉及**外部能力**（`PasswordEncoderPort`） |
| 登录安全策略（失败次数锁定）| 领域服务 | 涉及**另一个概念**（LoginAttempt）|
| 短信验证码校验 | 领域服务 | 涉及**另一个聚合**（SmsCode）|
| 登录成功后组装 Token 返回 | 应用服务 | 用例编排 + DTO 组装 |

一句话记法：聚合根「我变我自己」/ Domain Service「我帮你们协调」/ Application Service「我负责跑腿」。

## 四、仓储

- **接口在 `domain/repository/`**：以聚合根为操作对象；实现在 `adapter/outbound/persistence/`
- **返回聚合根，不返回 DO**：仓储使用者只关心领域模型；DO ↔ 聚合根的转换在 `RepositoryImpl` 或 Converter/Assembler 内完成
- DO 统一 `*DO` 后缀，只存在于 `adapter/outbound/persistence`，**不得泄漏到领域层**

## 五、领域事件

- 所有事件 record 实现 `common` 的 `DomainEvent` 接口（含 `eventType()` 默认方法，由类名去 `Event` 后缀派生；Jackson 反序列化依赖 `ParameterNamesModule` + `-parameters`，无需 `@JsonCreator`）
- 命名用过去时（`UserRegistered` / `OrderPaid` / `PasswordChanged`）；**内容只含必要 ID 与状态**，不传输完整聚合
- 事件是**状态转换的副产物**（转换方法返回 `Transition<Agg, Event>`），不是到处 `new` 出来的
- 发布：应用服务调 `DomainEventPublisher`；`ModulithDomainEventPublisher`（`@Primary`）经 Spring Modulith 把事件与业务同事务写入 `EVENT_PUBLICATION`，提交后异步外发 `eo.domain.events` Topic Exchange。路由键由类名自动派生（`ProductCreatedEvent` → `product.created`），无需注册
- 消费：类级 `@RabbitListener` + 方法级 `@RabbitHandler`（类型分发），每消费者独占队列，失败进 DLQ + 指数退避重试
- `@ConditionalOnProperty(matchIfMissing=true)` 保留以支持无 RabbitMQ 环境启动

## 六、异常与错误码（领域层视角）

**异常继承体系**：

```
RuntimeException
├── BaseBusinessException (common, 业务异常基类：code + message；HTTP 状态码由 GlobalExceptionHandler 按码前缀映射)
│   ├── BusinessException (common, 通用业务异常，无自定义子类的模块用它)
│   ├── ParamValidationException / FileException (common)
│   ├── OrderDomainException / PaymentDomainException / ProductDomainException / MessageDomainException (各模块统一领域异常)
│   └── TokenBudgetExceededException (ai；调用方按类型降级)
```

**关键规则**：

1. **每模块一个领域异常类**，具体语义由 `ResultCode` + 类上具名工厂承载（`notFound(id)` / `notOwner(id)`…），构造器非公开。判据是**调用方是否需要按类型分支**（降级/重试/格式化展示）——才值得独立类型，且必须继承本模块统一异常，这样 `catch (XxxDomainException)` 仍兜得住。`TokenBudgetExceededException` 是唯一在用的例子。机械门禁：`ArchitectureRulesTest#domain_exception_roots_are_unique_per_module`
2. 领域层抛异常后**不 try-catch**；应用层通常不处理，让事务回滚；`GlobalExceptionHandler` 在 framework 统一拦截
3. 必须用模块专属 `ResultCode`（如 `ProductResultCode.PRODUCT_NOT_FOUND`），**禁止回退全局 `B0002`**
4. **禁止 `static final` 异常实例**——所有抛出点共享同一堆栈，调试困难。每次 `BusinessException.of(...)` 新建实例
5. `[演进]` 跨模块 Feign 调用时调用方需 catch `FeignException` 转本模块业务异常，防止外部异常类型泄漏

```java
public class OrderDomainException extends BaseBusinessException {
    protected OrderDomainException(IResultCode resultCode, String message) { super(resultCode, message); }

    public static OrderDomainException of(String message) { return new OrderDomainException(OrderResultCode.ORDER_ERROR, message); }

    /** 具名工厂：错误码 + 文案形状只此一处 */
    public static OrderDomainException notFound(String orderId) {
        return new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND, "订单不存在: id=" + orderId);
    }
}
```

## 七、防腐层（ACL）

跨模块直接调用或集成外部系统时，必须由 ACL 把外部模型转成内部模型，防止外部变化污染领域模型：

```java
public class MessageUserInfoAdapter implements UserInfoPort {
    // 外部 user 模块 User 聚合 → message 模块内部 UserInfo 值对象（只暴露需要的字段）
    private UserInfo toUserInfo(User user) { return UserInfo.of(user.getId(), user.getUsername(), avatar); }
}
```

### 当前 ACL 实践 `[现状]`

| 模块 | 跨模块调用 | ACL 实现 | 状态 |
|------|-----------|---------|------|
| message → user | 批量查用户名/头像 | `MessageUserInfoAdapter`（`UserInfoPort` 唯一实现） | ✅ 已隔离 |
| product → user | 查资产方信息 | `SellerInfoAdapter`（`SellerInfoPort`） | ✅ 已隔离 |
| order → product | 扣减 / 恢复库存 / 标记售出 | `ProductInventoryAdapter`（`ProductInventoryPort`；订单展示读自持留痕快照不回查商品） | ✅ 已隔离 |
| order → payment | 发起支付 | `OrderPaymentGatewayAdapter`（`PaymentGatewayPort`） | ✅ 已隔离 |
| favorite → product | 查询商品信息 | `FavoriteProductInfoAdapter`（`ProductInfoPort`） | ✅ 已隔离（`<optional>true</optional>`） |
| admin → product/order/user | 聚合查询 | `AdminProductAdapter` 等 | ✅ 已隔离 |

## 八、架构守卫与测试分层

**架构守卫**：ArchUnit `ArchitectureRulesTest` 在 `./mvnw test` 内阻断违规（禁反向依赖 adapter / 端口必有适配器 / domain 零框架 / CQRS 读写隔离 / 业务模块间只经 `domain.port` + `domain.valueobject` 通信 / 禁 `infrastructure` 包 / Controller 不直连 mapper / 禁 `System.out` / 禁 `printStackTrace` / 禁 `org.springframework.dao` / 每模块唯一异常根）。**规则条数与清单以该类 javadoc 为单一来源**；除 `FreezingArchRule` 冻结的已知历史债（快照在 `src/test/resources/archunit_store/`）外无白名单。

**测试分层**：

| 类型 | 对象 | 工具 |
|------|------|------|
| 单元测试 | 领域层（聚合根、VO、领域服务） | JUnit 5 + AssertJ，**不启动 Spring** |
| 单元测试 | 应用层（Mock 端口） | JUnit 5 + Mockito |
| 切片测试 | Controller | `@WebMvcTest` + MockMvc |
| 集成测试 | 跨层真实链路（`*IT`） | Boot docker-compose 复用根 `compose.yaml`，failsafe 绑定 `mvn verify` |
| 架构测试 | 分层与包依赖 | ArchUnit |

> **不用 Testcontainers**：ryuk sidecar 镜像在无代理 Docker 下拉取失败，`disabledWithoutDocker=true` 会**静默跳过**用例、掩盖装配缺陷。`mvn test` 只跑 `*Test`，`mvn verify` 才跑 `*IT`。覆盖率口径与实测数字见 [工程指标.md](../工程指标.md)。

## 九、关键细节避坑

1. **领域层绝对纯净**：`domain` 包不能 import 任何 Spring / MyBatis / Redis API，只依赖 JDK 与 `easyorange-common` 的纯定义。
2. **值对象与聚合根都必须不可变**：全 `final` 字段，修改返回新实例；不暴露可变引用（返回防御性拷贝）。
3. **常量分层放置**——常量紧贴使用者所在层，不放模块根级别：

   | 常量类型 | 位置 | 示例 |
   |---------|------|------|
   | 业务枚举 / 状态 | `domain/enums`、`domain/constant` | `UserStatus`、`ProductStatus` |
   | 全局共享业务枚举 | `common/enums` | `ResultCode`、`BusinessType` |
   | 全局技术常量 | `common/constant` | `CommonConstant` |
   | 框架层常量 | `config/constant` | `LoginCacheConstants` |
   | 模块业务错误码 | `domain/constant/*ResultCode` | `UserResultCode` |
   | 技术常量（Redis Key 等） | `adapter/outbound/cache/` | `ProductCacheConstant` |

   包名规范：枚举包用复数 `enums/`（`enum` 是关键字）；常量包当前统一 `constant/`（单数）`[现状]`，`[演进]` 目标是统一为 `constants/`。

4. **CQRS 渐进式引入**：简单场景用传统应用服务（`AuthAppService` 读写通吃）；读写模型差异大时才拆 `command/` / `query/`。Command 用顶层 record（一命令一文件）；入口是 handler 上以用例命名的具名方法（`createOrder` / `payOrder`…），一命令一方法，不用重载或统一 `handle` 分发。**禁止**把命令内联为 service 的 inner record，**禁止** Lombok `@Data` / `@Builder`。`sealed interface` 只在有穷尽分发消费者时引入（没有 pattern matching 的 sealed 标记接口只是死代码）。当前采用情况：

   | 模块 | 模式 | 说明 |
   |------|------|------|
   | product / order | CQRS + 独立 ReadModel | 读写模型差异大 |
   | payment / message | CQRS（Handler 级） | 只做读写 Handler 分离 + 独立查询仓储 |
   | user | 传统应用服务 | 读写差异不大 |
   | favorite | 传统服务 | 简单 CRUD，单表索引够用 |

5. **应用服务方法返回值**与 **Controller 响应内联约定**见 [easyorange-backend/AGENTS.md](../../easyorange-backend/AGENTS.md)。

## 十、分层职责与依赖倒置

| 层 | 职责 | 依赖规则 |
|---|------|---------|
| adapter | 与外部系统交互，格式转换，实现端口 | 依赖 application、domain |
| application | 用例编排，事务管理，端口协调 | 依赖 domain |
| domain | 核心业务逻辑、领域模型，**定义**端口 | 仅依赖 common 中的纯定义 |

- **输出端口**（仓储、事件发布、缓存）定义在 `domain/port` / `domain/repository`，实现在 `adapter/outbound`；入站适配器在 `adapter/inbound`。
- **输入端口可选**：仅当有多个实现或需要解耦时才定义，单实现直接调应用服务（YAGNI）。
- 跨模块调用与外部系统集成（支付、短信）一律经端口抽象 + ACL 转换。
