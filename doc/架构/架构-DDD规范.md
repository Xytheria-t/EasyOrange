# EasyOrange DDD 领域设计规范

> 本文档定义了 EasyOrange 项目的 DDD 领域设计规范、防腐层设计、架构守卫及关键细节。

---

## 一、聚合根设计

**聚合根职责：**

- 保证聚合内的业务不变量（Invariants）
- 作为聚合的唯一访问入口
- 通过工厂方法创建，确保创建时满足不变量

**当前实践** `[现状]`：

项目使用 Lombok `@Builder(toBuilder = true)` 实现不可变聚合根，通过 `toBuilder()` 实现返回新实例的"修改"模式：

```java
@Getter
@Builder(toBuilder = true)
public class User {
    private final String id;
    private final Credentials credentials;       // username + encodedPassword
    private final UserType userType;
    private final UserStatus status;
    private final ContactInfo contactInfo;       // email + phone
    private final PersonalInfo personalInfo;     // realName + nickName + sex + studentId + avatar
    private final LoginInfo loginInfo;
    private final AuditInfo auditInfo;

    public String getUsername() { return credentials.username(); }
    public String getPassword() { return credentials.encodedPassword(); }

    public static User register(String username, String encodedPassword, String nickName) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(encodedPassword, "password must not be null");
        return User.builder()
            .credentials(new Credentials(username, encodedPassword))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(ContactInfo.empty())
            .personalInfo(new PersonalInfo(null, nickName, null, null, null))
            .loginInfo(LoginInfo.initial())
            .build();
    }

    public User changePassword(String encodedNewPassword, String operatorId) {
        Objects.requireNonNull(encodedNewPassword, "password must not be null");
        return this.toBuilder()
            .credentials(this.credentials.changePassword(encodedNewPassword))
            .loginInfo(this.loginInfo.updatePasswordTime())
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }
}
```

**聚合边界规则：**

- 一个事务只能修改一个聚合根
- 聚合根通过 ID 引用其他聚合，不直接持有其他聚合的引用
- 聚合内的所有修改必须通过聚合根的方法进行
- 聚合根字段必须为 `final`，修改操作返回新实例

---

## 二、值对象设计

值对象强制要求：

- **不可变**：使用 `final` 字段或 `record`
- **基于属性的相等性**：实现 `equals()` 和 `hashCode()`（record 自动实现）
- **自验证**：创建时校验业务规则
- **替换而非修改**：通过返回新实例实现"修改"

```java
public record ContactInfo(String email, String phone) {
    public ContactInfo {
        if (email != null && !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    public static ContactInfo empty() { return new ContactInfo(null, null); }
    public ContactInfo withEmail(String newEmail) { return new ContactInfo(newEmail, phone); }
}
```

---

## 三、领域服务

- **无状态**：不持有任何可变状态
- **纯业务逻辑**：不包含框架调用、数据库操作
- **多聚合协调**：当业务逻辑涉及多个聚合时使用领域服务
- **与应用层区分**：领域服务处理纯业务规则，应用层处理用例编排和事务管理

```java
// 领域服务：纯业务规则，使用通用业务异常携带模块错误码
public class RegistrationService {
    public void validateUniqueContactInfo(User user, List<User> existingUsers) {
        if (existingUsers.stream().anyMatch(u -> u.getContactInfo().phone().equals(user.getContactInfo().phone()))) {
            throw BusinessException.of(UserResultCode.PHONE_EXISTS);
        }
    }
}
```

---

## 四、仓储设计

- **接口在 domain/repository**：定义以聚合根为操作对象的接口
- **实现在 adapter/outbound/persistence**：内部处理 DO 与聚合根的转换
- **返回聚合根，不返回 DO**：仓储的使用者只关心领域模型

```java
// domain/repository/UserRepository.java
public interface UserRepository {
    Optional<User> findById(String id);
    User save(User user);
    void deleteById(String id);
    Optional<User> findByUsername(String username);
}

// adapter/outbound/persistence/UserRepositoryImpl.java
// 继承 framework/repository/BaseRepository 获取 lambdaQuery()/lambdaUpdate() 便利方法
public class UserRepositoryImpl extends BaseRepository<UserMapper, UserDO> implements UserRepository {

    @Override
    public User save(User user) {
        UserDO entity = UserAssembler.toEntity(user);
        mapper.insertOrUpdate(entity);
        return UserAssembler.toDomain(entity);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findBy(UserDO::getUsername, username).map(this::toDomain);
    }
}
```

---

## 五、领域事件

- **事件接口**：所有领域事件 record 实现 `common` 中的 `DomainEvent` 接口（含 `eventType()` 默认方法，由类名消除 `Event` 后缀自动派生。Jackson 反序列化依赖 `ParameterNamesModule` + `-parameters`，无需 `@JsonCreator`）
- **事件命名**：使用过去时态（UserRegistered、OrderPaid、PasswordChanged）
- **事件内容**：仅包含必要 ID 和状态，不传输完整聚合
- **事件发布**：应用服务调用 `DomainEventPublisher` 发布，框架层通过 RabbitMQ Topic Exchange 路由到各模块 `@RabbitListener` 消费者
- **路由键**：由事件类名自动派生（`ProductCreatedEvent` → `product.created`），无需手动注册
- **消费者模式**：多方法消费者使用类级 `@RabbitListener` + 方法级 `@RabbitHandler`（类型分发），每个消费者独占队列，失败消息路由到 DLQ + 指数退避重试

`[现状]` 已从 Spring EventBus → RabbitMQ 直发 → Spring Modulith 三级演进。`ModulithDomainEventPublisher`（`@Primary`）通过 `ApplicationEventPublisher` 代理，Spring Modulith 在 `EVENT_PUBLICATION` 表中持久化事件（与应用事务同原子），事务提交后异步发布到 `eo.domain.events` Topic Exchange。at-least-once 语义 + 消费者 `EventIdempotencyChecker` 确保精确一次处理。`@ConditionalOnProperty(matchIfMissing=true)` 保留以支持无 RabbitMQ 环境启动。

---

## 六、错误码体系与异常传播

### 错误码分层

| 错误码定义位置 | 前缀/模式 | 示例 |
|-----------|------|------|
| `common/enums/ResultCode` | 通用系统错误码 | `INTERNAL_SERVER_ERROR`、`VALIDATE_FAILED`、`FORBIDDEN`、`NOT_FOUND` |
| 各模块 `domain/constant/*ResultCode` | 模块业务错误码 | `UserResultCode`、`ProductResultCode`、`MessageResultCode` |

### 异常继承体系

```
RuntimeException
├── BaseBusinessException (common, 业务异常基类)
│   ├── code: String          # 错误码
│   └── message: String       # 错误消息
│   ├── BusinessException (common, 通用业务异常，几乎所有模块通过 `of(ResultCode)` 使用)
│   ├── ParamValidationException (common, 参数校验异常)
│   ├── FileException (common, 文件异常, 构造器 protected, 使用 `FileException.of(...)`)
│   ├── OrderDomainException (order 模块, 统一领域异常)
│   ├── PaymentDomainException (payment 模块, 统一领域异常)
│   └── ... 其他模块领域异常（推荐统一，而非多叶子类）
```

**划分原则：**
- `BaseBusinessException` — 所有业务异常的基类，提供统一的 code + message（HTTP 状态码由 `GlobalExceptionHandler` 按错误码前缀映射）
- `BusinessException` — 通用业务异常，用于不需要自定义子类的场景
- 各模块异常 — 继承 `BaseBusinessException`，通过模块专属 `ResultCode` 区分具体业务场景（禁止回退到全局 `B0002`）
- 领域层异常（如 `OrderDomainException`、`PaymentDomainException`）— 领域层专用，不含任何框架依赖
- 无自定义异常类的模块（如 user、favorite）— 直接使用 `BusinessException.of(ModuleResultCode.XXX)`，无需定义子类

### 异常传播路径

```
领域层 (throw OrderDomainException / BusinessException.of(...))
  → 应用层 (无需 catch，事务自动回滚)
    → 适配层 (GlobalExceptionHandler 拦截)
      → HTTP 响应 (JSON: { "code": "VALIDATE_FAILED", "message": "用户不存在" })
```

**关键规则：**

1. **领域层抛异常**：当业务不变量被违反时，直接抛出模块级领域异常，不 try-catch
2. **应用层不吞异常**：应用服务通常不处理领域异常，让事务回滚；仅在需要自定义错误映射时 catch
3. **全局统一拦截**：`GlobalExceptionHandler` 在 `easyorange-framework` 中统一拦截，确保 API 响应格式一致
4. **RPC 异常映射** `[演进]`：跨模块 Feign 调用时，调用方需 catch FeignException 并转换为自己的业务异常，防止外部异常类型泄漏

```java
// 领域层异常（继承 BaseBusinessException，含统一错误码）
public class OrderDomainException extends BaseBusinessException {
    public static OrderDomainException of(IResultCode resultCode) {
        return new OrderDomainException(resultCode.getCode(), resultCode.getMessage());
    }
    public static OrderDomainException of(IResultCode resultCode, String message) {
        return new OrderDomainException(resultCode.getCode(), message);
    }
    private OrderDomainException(String code, String message) {
        super(code, message);
    }
}

// 应用层/适配层使用通用业务异常（无自定义异常类的模块）
throw BusinessException.of(UserResultCode.USER_NOT_FOUND);
```

> **注意**：避免使用 `static final` 异常实例（如 `public static final UserBizException USER_NOT_FOUND = ...`），因为所有抛出点共享同一堆栈跟踪，导致调试困难。应使用 `throw new UserBizException(...)` 每次创建新实例。

---

## 七、防腐层（ACL）设计

当跨模块通过直接依赖调用或集成外部系统时，必须使用防腐层将外部模型转换为内部模型：

```java
// easyorange-application/.../adapter/outbound/user/MessageUserInfoAdapter.java
public class MessageUserInfoAdapter implements UserInfoPort {
    private final UserRepository userRepository;

    @Override
    public Map<String, UserInfo> getUserInfoMap(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, this::toUserInfo, (a, _) -> a));
    }

    // 外部 user 模块 User 聚合 → message 模块内部 UserInfo 值对象（只暴露需要的字段）
    private UserInfo toUserInfo(User user) {
        String avatar = user.getPersonalInfo() != null ? user.getPersonalInfo().avatar() : null;
        return UserInfo.of(user.getId(), user.getUsername(), avatar);
    }
}
```

### 当前 ACL 实践 `[现状]`

| 模块 | 跨模块调用 | ACL 实现 | 状态 |
|------|-----------|---------|------|
| message → user | 批量查用户名/头像 | `MessageUserInfoAdapter`（`UserInfoPort` 唯一实现；2026-08-12 起 order 侧单查已下线） | ✅ 已隔离 |
| product → user | 查资产方信息 | `SellerInfoAdapter`（实现 `SellerInfoPort`） | ✅ 已隔离 |
| order → product | 查商品 / 扣减恢复库存 | `ProductQueryAdapter` / `ProductInventoryAdapter` | ✅ 已隔离 |
| order → payment | 发起支付 | `OrderPaymentGatewayAdapter`（实现 `PaymentGatewayPort`） | ✅ 已隔离 |
| favorite → product | 查询商品信息 | `FavoriteProductInfoAdapter`（实现 `ProductInfoPort`） | ✅ 已隔离（`<optional>true</optional>`） |
| admin → product/order/user | 聚合查询 | `AdminProductAdapter` 等（`AdminProductPort` 等 8 个 ACL 端口） | ✅ 已隔离 |

### 跨模块 RPC 版本兼容策略 `[演进]`

当业务模块之间通过 Feign 直接调用时（强一致性场景），必须考虑 RPC 接口的版本兼容：

| 变更类型 | 兼容性 | 处理方式 |
|---------|--------|---------|
| 新增字段（响应体） | 向前兼容 | 调用方 ACL 忽略未知字段，使用 `@JsonIgnoreProperties(ignoreUnknown = true)` |
| 新增可选参数（请求体） | 向前兼容 | 提供方设置默认值，调用方可选传递 |
| 删除/重命名字段 | **不兼容** | 提供方必须保留旧字段并用 `@Deprecated` 标记，至少保留一个迭代周期 |
| 修改字段类型 | **不兼容** | 必须新增接口版本，旧接口保留一个迭代周期 |

---

## 八、架构守卫测试

### 当前架构测试 `[现状]`

项目使用 ArchUnit 1.4.x（`@AnalyzeClasses` + `@ArchTest`）守卫 DDD 分层规则：

| 规则 | 说明 | 执行方式 |
|------|------|---------|
| 领域层零框架依赖 | `domain/` 包不得依赖 Spring/MyBatis/Servlet，不得依赖 controller/mapper/service.impl/DTO | ArchUnit `noClasses().dependOn()` |
| CQRS 命令/查询分离 | CommandHandler 不得依赖 QueryHandler，反之亦然 | ArchUnit `noClasses().dependOn()` |
| 业务模块间端口通信 | 业务模块间仅通过 `domain.port` / `domain.valueobject` 通信 | ArchUnit 自定义 `ArchCondition` |
| 端口接口有适配器实现 | `domain.port.*Port` 接口必须在 `adapter.outbound` 有实现 | ArchUnit `JavaClasses` 方法测试 |
| 禁止 infrastructure/ 包 | 已废弃，用 `adapter/outbound/` | ArchUnit `noClasses().resideInAPackage()` |

~~**已知白名单**~~ ✅ 已全部消除（2026-07-04）：`MessageQueryRepository` 改用 domain record、`PaymentQueryRepository` 移至 `application/port/query/`、`CallbackSignatureVerifierPort` 确认结构正确后移除。`PORT_ALLOWLIST` 现为空集。

### 测试分层策略

| 测试类型 | 测试对象 | 工具 | 覆盖率要求 |
|---------|---------|------|----------|
| 单元测试 | 领域层（聚合根、值对象、领域服务） | JUnit 5 + AssertJ | 90%+ |
| 单元测试 | 应用层（应用服务，Mock 端口） | JUnit 5 + Mockito | 80%+ |
| 集成测试 | — | 已移除（WSL2 Docker 兼容性限制） | — |
| 集成测试 | Controller（MockMvc） | Spring Boot Test + MockMvc | 关键路径 100% |
| 架构测试 | DDD 分层规则、包依赖关系 | ArchUnit ArchitectureRulesTest | 核心规则 100% |

### 领域层单元测试示例

```java
@DisplayName("User 聚合根")
class UserTest {

    @Test
    @DisplayName("create: 正常创建用户")
    void create_validInput_success() {
        var user = User.create("testuser", "encodedPassword");

        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
    }

    @Test
    @DisplayName("create: 用户名为空时抛出异常")
    void create_nullUsername_throws() {
        assertThatThrownBy(() -> User.create(null, "encodedPassword"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("username");
    }

    @Test
    @DisplayName("changePassword: 返回新实例，原实例不变")
    void changePassword_returnsNewInstance() {
        var user = User.create("testuser", "encodedPassword");
        var updated = user.changePassword("newEncodedPassword", 1L);

        assertThat(updated.getPassword()).isEqualTo("newEncodedPassword");
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
    }
}
```

### 应用层单元测试示例

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthAppService")
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthAppService service;

    @BeforeEach
    void setUp() {
        service = new AuthAppService(userRepository);
    }

    @Test
    @DisplayName("changePassword: 用户不存在时抛出异常")
    void changePassword_userNotFound_throws() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(1L, "old", "new"))
            .isInstanceOf(BusinessException.class);
    }
}
```

---

## 九、关键细节避坑

### 1. 领域层绝对纯净

`domain` 包下的所有代码，**不能 import 任何 Spring、MyBatis、Redis 的 API**，只能依赖 JDK 和 `easyorange-common` 中的纯定义类（枚举、异常基类、事件基类等）。

~~**当前例外**~~ ✅ 已消除（2026-07-04）：`domain/enums/` 枚举的 MyBatis-Plus 注解属于 adapter 层关注，已不在 domain 层；message 模块 MyBatis 实现已迁移至 `adapter/outbound/persistence/`。

### 2. 值对象不可变性

所有值对象必须不可变：
- 使用 `record` 或 `final` 字段
- 不暴露可变引用（返回防御性拷贝）
- 通过返回新实例实现"修改"

### 3. 聚合根不可变性

聚合根字段必须为 `final`，修改操作返回新实例。当前使用 Lombok `@Builder(toBuilder = true)` 模式实现。

### 4. 领域服务持久化职责

**原则：领域服务应自己完成聚合的持久化，而不是将聚合返回给应用服务再保存。**

```java
// BAD — 领域服务只创建对象，应用服务负责保存，逻辑分散
public class RegistrationService {
    public User register(...) {
        User user = User.register(...);
        return user; // 不保存
    }
}

// GOOD — 领域服务自己完成持久化，逻辑内聚
public class RegistrationService {
    private final UserRepository userRepository;

    public User register(...) {
        User user = User.register(...);
        return userRepository.save(user); // 领域服务自己持久化
    }
}
```

**例外情况**：当应用服务需要在领域服务执行后、持久化前插入额外逻辑（如事件发布需获取 ID），可将持久化留在应用层。但应尽量让领域服务完成完整的业务闭环。

### 5. 聚合根、领域服务、应用服务职责速查

| 层级 | 一句话职责 | 判断标准 |
|------|-----------|---------|
| **聚合根** | **"我变我自己"** — 只负责自己的状态变更 | 只修改自己的字段，不查别人 |
| **Domain Service** | **"我帮你们协调"** — 跨聚合/跨实体的业务逻辑 | 需要查其他聚合、调用外部端口、协调多个对象 |
| **Application Service** | **"我负责跑腿"** — 用例编排、事务、DTO转换 | 调用 Domain Service → 转 DTO → 返回 |

**典型归属判断：**

| 场景 | 归属 | 原因 |
|------|------|------|
| `user.changePassword()` | 聚合根 | 只涉及 User 自己的状态变更 |
| `user.recordLogin()` | 聚合根 | 只涉及 User 自己的 LoginInfo 更新 |
| 检查用户名是否已存在 | Domain Service | 需要查询**其他** User 聚合 |
| 密码加密比对 | Domain Service | 涉及**外部能力**（PasswordEncoderPort）|
| 登录安全策略（失败次数锁定）| Domain Service | 涉及**另一个概念**（LoginAttempt）|
| 短信验证码校验 | Domain Service | 涉及**另一个聚合**（SmsCode）|
| 登录成功后组装 Token 返回 | Application Service | 用例编排 + DTO 组装 |

### 6. 常量分层放置

| 常量类型 | 放置位置 | 示例 |
|---------|---------|------|
| 业务枚举 / 状态 | `domain/constant` 或 `domain/enums` | UserStatus、ProductStatus |
| 业务常量（非枚举） | `domain/constant` | UserConstant、ProductConstant |
| 全局共享业务枚举 | `common/enums` | ResultCode、BusinessType |
| 全局技术常量 | `common/constant` | CommonConstant |
| 框架层常量 | `framework/config/constant` | LoginCacheConstants |
| 模块业务错误码 | `domain/constant/*ResultCode` | UserResultCode、ProductResultCode |
| 技术常量（Redis Key 等） | `adapter/outbound/cache/` | OrderCacheConstant、ProductCacheConstant |

**包命名规范：**

- **枚举包用复数 `enums/`**：`enum` 是 Java 关键字，不能作为包名
- **常量包当前统一使用 `constant/`（单数）** `[现状]`
- `[演进]` 目标是逐步统一为 `constants/`（复数）以与 `enums/` 保持一致

**原则：常量应紧贴其使用者所在的层，而不是放在模块根级别。**

### 7. 应用服务 vs 领域服务

| 维度 | 应用服务 | 领域服务 |
|-----|---------|---------|
| 职责 | 用例编排、事务管理、端口协调 | 纯业务规则、多聚合协调、聚合持久化 |
| 依赖 | 依赖领域服务 + 输出端口 | 仅依赖领域模型 + 仓储接口 |
| 框架 | 可包含 @Transactional 等注解 | 绝对无框架依赖 |
| 返回值 | DTO/Response | 聚合根/值对象 |
| 拆分原则 | **按用例拆分**，一个类一个完整场景 | **按业务领域拆分**，一个类一个业务子域 |

**应用服务拆分原则：**

- **避免"上帝类"**：单个应用服务不应处理不相关的用例；但共享相同依赖和事务边界的用例可以聚合（如 `AuthAppService` 处理注册/登录/登出/刷新/忘记密码/修改密码——依赖 `AuthenticationService` + `RegistrationService` + `TokenService`）
- **按职责边界拆分**：当多个用例共享相同依赖集且事务边界一致时可合并；当用例差异大或独立演进时再拆分
- **命名规范**：`{领域}AppService`，如 `AuthAppService`、`ProfileAppService`
- **事务边界**：每个应用服务方法就是一个完整的事务边界

**领域服务设计原则：**

- **自己完成持久化**：领域服务应直接调用 `Repository` 完成聚合的保存和更新
- **禁止方法参数注入依赖**：所有依赖必须通过构造函数注入
- **依赖接口而非实现**：领域服务依赖的 Repository、Port、其他 Domain Service 都必须是接口
- **依赖数量控制**：理想 1-2 个，可接受 3 个，4 个及以上应考虑拆分

### 8. CQRS 渐进式引入

- **早期/简单场景**：使用传统应用服务（如 `AuthAppService`、`ProfileAppService`），同时处理读写
- **读写模型差异大时**：拆分为 `command/` 和 `query/`。Command 使用顶层 record（每个命令一个文件），3+ 命令时引入 sealed interface 统一管理。禁止使用 inner records 或 Lombok @Data @Builder。详见 [架构改进记录](#)
- **复杂查询场景**：Query 侧可使用物化视图、读库副本、ES 等
- **不推荐一刀切**：简单 CRUD 场景强制 CQRS 会增加大量样板代码

**当前 CQRS 采用情况**：

| 模块 | 模式 | 说明 |
|------|------|------|
| user | 传统应用服务 | 读写差异不大，两个服务聚合认证+密码所有用例 |
| product | CQRS | 读写模型差异大，独立 command/query 处理 |
| order | CQRS | 命令侧 + 查询侧分离 |
| payment | CQRS | 命令侧 + 查询侧分离 |
| message | CQRS | 命令侧 + 查询侧分离 |
| favorite | 传统服务 | 简单 CRUD，无需 CQRS |

### 9. DO 命名规范

- 数据库实体统一使用 `*DO` 后缀：`UserDO`、`ProductDO`、`PaymentDO`
- DO/PO 仅存在于 `adapter/outbound/persistence` 包中，不得泄漏到领域层
- DO/PO 与聚合根之间的转换在 `RepositoryImpl` 或 `Converter/Assembler` 中完成

### 10. 禁止 static final 异常实例

```java
// BAD — 所有抛出点共享同一堆栈跟踪，调试困难
public static final BusinessException USER_NOT_FOUND =
    new BusinessException("用户不存在");

// GOOD — 每次创建新实例，堆栈跟踪准确
throw BusinessException.of(UserResultCode.USER_NOT_FOUND);
```

---

## 十、架构设计原则

### 1. 六边形架构（端口与适配器）

- **输出端口**：定义依赖外部的接口（如仓储、事件发布、缓存），位于 `domain/port` 和 `domain/repository`
- **入站适配器**：将外部请求（REST、MQ、Job）转换为内部调用，位于 `adapter/inbound`
- **出站适配器**：将内部调用转换为外部实现（数据库、MQ、RPC），位于 `adapter/outbound`
- **输入端口（可选）**：仅当有多个实现或需要解耦时定义，单实现时直接调用应用服务（YAGNI）

### 2. DDD 分层职责

| 层 | 职责 | 依赖规则 |
|---|------|---------|
| adapter | 与外部系统交互，格式转换，实现端口 | 依赖 application、domain |
| application | 用例编排，事务管理，端口协调 | 依赖 domain |
| domain | 核心业务逻辑，领域模型，定义端口 | 仅依赖 common 中的纯定义 |

### 3. 依赖倒置原则（DIP）

- 领域层定义端口接口（`domain/port/`、`domain/repository/`）
- 适配器层实现端口接口（`adapter/outbound/`）
- 应用层通过端口接口与外部交互，不直接依赖具体实现

### 4. 防腐层（ACL）

- 跨模块调用必须在 ACL/Adapter 中转换外部模型为内部模型
- 集成外部系统（支付、短信）也必须通过端口抽象
- 防止外部模型变化污染内部领域模型
