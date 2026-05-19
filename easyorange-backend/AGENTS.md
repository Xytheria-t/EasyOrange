# EasyOrange Backend 编码指南

Spring Boot 4.0.3 + Java 25 后端，采用 DDD + 六边形架构。

## 技术栈版本

| 依赖 | 版本 |
|------|------|
| Java | 25 |
| Spring Boot | 4.0.3 |
| MyBatis-Plus | 3.5.16 |
| MapStruct | 1.6.3 |
| Flyway | 11.14.1 |
| JJWT | 0.13.0 |
| ArchUnit | 1.4.1 |
| Spring Data Elasticsearch | 6.0.3 |
| Elasticsearch | 8.17.3 |

## DDD 分层规则

依赖方向严格单向向内：`adapter → application → domain`

```
┌─────────────────────────────────────────────┐
│  adapter (适配器层)                          │
│  inbound/: REST Controller, DTO, Validation │
│  outbound/: Persistence, Cache, Messaging   │
├─────────────────────────────────────────────┤
│  application (应用层)                        │
│  command/: 命令处理                          │
│  query/: 查询处理 (CQRS 模块)               │
│  service/: 应用服务                          │
│  assembler/: DTO 组装                        │
├─────────────────────────────────────────────┤
│  domain (领域层) — 纯业务逻辑，零框架依赖    │
│  aggregate/: 聚合根                          │
│  valueobject/: 值对象 (record)               │
│  event/: 领域事件                            │
│  port/output/: 出站端口接口                  │
│  repository/: 仓储接口                       │
│  service/: 领域服务                          │
└─────────────────────────────────────────────┘
```

**关键约束**：
- `domain` 层禁止依赖 Spring 框架、MyBatis、Redis 等基础设施
- `domain` 层通过 `port/output/` 接口与外部交互，由 `adapter/outbound/` 实现
- `application` 层编排业务流程，事务边界在此层
- `adapter/inbound/` 仅做参数校验和 DTO 转换，不含业务逻辑
- **查询方法只读事务**: `application/service/` 和 `application/query/` 下的纯查询方法（find/get/list/query/count 等）**必须**标注 `@Transactional(readOnly = true)`；写操作方法使用 `@Transactional(rollbackFor = Exception.class)`。这是项目级约定，所有模块（user/product/order/payment/message/favorite/admin）一致遵循

## CQRS 模式

product、order、payment 模块使用 CQRS：

- **Command 侧**: `adapter/inbound/web/*CommandController` → `application/command/*CommandHandler` → `domain`
- **Query 侧**: `adapter/inbound/web/*QueryController` → `application/query/*QueryHandler` → `domain/repository/query/`

读写使用不同的 Repository 接口和数据模型。

## 领域事件机制

业务模块在 domain/port/output/ 下定义带领域语义的事件发布 Port（如 `UserEventPort`），adapter 层实现委派给框架的 `DomainEventPublisher`：

```java
// Port 定义（domain 层）
public interface UserEventPort extends OutboundPort {
    void publishUserRegistered(UserRegisteredEvent event);
}

// Adapter 实现（adapter 层）
@Component
public class UserEventPublisher implements UserEventPort {
    private final DomainEventPublisher domainEventPublisher;
    public void publishUserRegistered(UserRegisteredEvent event) {
        domainEventPublisher.publish(event);
    }
}
```

- 应用服务注入 Port 接口发布事件，不直接依赖 `DomainEventPublisher`
- `DomainEventPublisher`（common/event/）同步发布到 Spring EventBus
- 监听器使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async("domainEventExecutor")`
- 需要 Outbox 可靠投递的模块（如支付）通过 `framework/outbox/` 在业务事务内持久化事件

## 跨模块通信

**当前状态 (2026-05-09)**：所有跨模块依赖已通过端口接口 + 适配器模式隔离，Maven 依赖标记为 `<optional>true</optional>`。

**隔离方式**：
- 调用方模块定义 `domain/port/output/` 接口（如 `ProductInventoryPort`）
- 适配器实现在 `easyorange-application/adapter/outbound/` 包下
- Maven 依赖标记为 `<optional>true</optional>` 实现编译期隔离

**事件驱动**：
- 写操作通过领域事件解耦（如 `PaymentInitiationRequestedEvent`、`StockReservationRequestedEvent`）
- 事件监听器在 `easyorange-application/adapter/event/` 包下
- 使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async("domainEventExecutor")` 模式
- `DomainEventPublisher`（common/event/）同步发布，`DomainEventPublisherImpl` 转发到 Spring EventBus
- 需要 Outbox 可靠投递的模块（如支付）通过 `framework/outbox/` 存储在业务事务内持久化事件

**查询操作**：保留同步端口调用（如 `getSnapshot()`），通过可选依赖实现

**事件流**：
```
OrderCreatedEvent → OrderCreatedEventSubscriber → StockReservationRequestedEvent → StockReservationEventListener → ProductCommandService.decrementStock()
PaymentInitiationRequestedEvent → PaymentInitiationEventListener → PaymentCommandHandler.handle()
```

## 统一响应格式

```java
Result<T>       // 单条数据
Result.success(data)
Result.fail(ResultCode)

PageResult<T>   // 分页数据
PageResult.of(records, total, page, size)
```

## 命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| 聚合根 | 名词 | `User`, `Product`, `OrderAggregate` |
| 值对象 | 名词 (record) | `ProductId`, `Money`, `StockQuantity` |
| 领域事件 | `*Event` | `UserRegisteredEvent` |
| 领域服务 | `*DomainService` | `AuthenticationDomainService` |
| 应用服务 | `*AppService` / `*CommandHandler` | `UserAppService` |
| 仓储接口 | `*Repository` | `UserRepository` |
| 仓储实现 | `*RepositoryImpl` / `Mybatis*Repository` (继承 `BaseRepository`) | `UserRepositoryImpl extends BaseRepository<UserMapper, UserEntity>` |
| 出站端口 | `*Port` | `PaymentGatewayPort` |
| 控制器 | `*Controller` | `AuthController` |
| 请求 DTO | `*Request` / `*Command` | `LoginRequest` |
| 响应 DTO | `*Response` / `*Response` | `UserResponse` |
| 数据对象 | `*DO` / `*PO` | `UserEntity`, `PaymentPO` |

## 数据对象基类

```java
public class BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "2")
    private Integer delFlag;
    @Version
    private Integer version;
}
```

## 测试策略

| 类型 | 工具 | 范围 |
|------|------|------|
| 单元测试 | JUnit 5, Mockito | 领域模型、值对象、领域服务 |
| 集成测试 | Testcontainers (MySQL + Redis) | Repository、Cache、事件发布 |
| 架构测试 | ArchUnit | DDD 分层合规、包依赖规则 |
| 控制器测试 | MockMvc | API 端点 |

架构守卫测试位于 `easyorange-application/src/test/.../ArchitectureRulesTest.java`。

**TestSecurityUtil 模式**：测试中设置 `SecurityContextHolder` 统一使用 `TestSecurityUtil.setSecurityContext(userId)`（位于 `easyorange-framework` 的 main 源码），替代 `mockStatic(SecurityContextUtil.class)`。`clearSecurityContext()` 必须在 `finally` 块中调用保证测试间隔离。

## Flyway 迁移规范

- DDL 脚本: `db/migration/V{N}__description.sql`
- 开发数据: `db/dev/test_data.sql`
- 禁止修改已执行的迁移脚本
- 新增字段必须可空或有默认值

## 安全要点

- JWT 双 Token: Access Token (短期) + Refresh Token (长期)
- 密码: BCrypt 加密存储
- 限流: `@RateLimiter` (Redis + Lua 滑动窗口)
- 防重: `@RepeatSubmit`
- XSS: `XssFilter` + `XssHttpServletRequestWrapper`
- CORS: 生产环境严格白名单

## 不可变集合约定

全项目 Java 代码**禁止使用 `Collections` 工具类**创建空/单元素/不可包装集合，统一使用 Java 9+ 工厂方法：

| 场景 | ✅ 推荐 | ❌ 禁止 |
|------|---------|---------|
| 空 List | `List.of()` | `Collections.emptyList()` |
| 空 Set | `Set.of()` | `Collections.emptySet()` |
| 空 Map | `Map.of()` | `Collections.emptyMap()` |
| 单元素 List | `List.of(x)` | `Collections.singletonList(x)` |
| 单元素 Set | `Set.of(x)` | `Collections.singleton(x)` |
| 不可变包装 | `List.copyOf(x)` / `Set.copyOf(x)` / `Map.copyOf(x)` | `Collections.unmodifiableXxx(x)` |

此约定已通过全局 grep 清理完毕，新代码须直接使用工厂方法。

## 踩坑警示

### MyBatis-Plus UUID TypeHandler

MyBatis-Plus **没有内置** `java.util.UUID` 的 TypeHandler。如果 PO 类中有 `UUID` 字段（如 `OutboxMessagePO.eventId`），直接 insert/update 会报：

```
Type handler was null on parameter mapping for property 'eventId'. javaType=UUID
```

**解决方案（已配置）**：
1. 自定义 `UuidTypeHandler extends BaseTypeHandler<UUID>`（位于 `framework/config/database/`）
2. 在 `application.yaml` 的 `mybatis-plus:` 下配置 `type-handlers-package: com.cartethyia.easyorange.framework.config.database`
3. 数据库侧对应列类型为 `CHAR(36)`

**注意**：新增 PO 的 UUID 字段时无需额外配置，全局 TypeHandler 会自动生效。

### Jackson 领域事件反序列化

所有领域事件类（如 `PaymentCreatedEvent`、`OrderCreatedEvent`）只有参数化构造器，无 `@JsonCreator` / `@JsonProperty` 注解。反序列化依赖 **ParameterNamesModule** 通过构造器参数名推断属性映射。

如果移除 `JacksonConfig` 中的 `ParameterNamesModule` 或遗漏 `jackson-module-parameter-names` 依赖，Outbox 补偿器和发布器反序列化事件时会报：

```
InvalidDefinitionException: Cannot construct instance of XxxEvent (no Creators, like default constructor, exist)
```

**已配置位置**：
1. `framework/pom.xml` — `jackson-module-parameter-names` 依赖
2. `framework/.../JacksonConfig.java` — `mapper.registerModule(new ParameterNamesModule())`

**注意**：新增领域事件类时无需添加任何 Jackson 注解，遵循现有模式即可。

### Spring Boot 4 @WebMvcTest 路径变化

Spring Boot 4.0 将 `@WebMvcTest` 和 `@AutoConfigureMockMvc` 从 `org.springframework.boot.test.autoconfigure.web.servlet` 迁移到 `org.springframework.boot.webmvc.test.autoconfigure.web.servlet` 包。

**Controller 测试必须使用新路径**：
```java
import org.springframework.boot.webmvc.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
```

**`@WebMvcTest` 需要 `@SpringBootConfiguration`**：如果模块没有默认的配置类（如 `easyorange-admin`），需在 test 源码下创建 `TestAdminApplication.java`：
```java
@SpringBootApplication
public class TestAdminApplication {}
```

**`@WebMvcTest` 的 component-scan 陷阱**：`@ComponentScan` 范围过大会拉入 persistence 类（依赖 MyBatis/DataSource），在 web 切片中不可用。解决办法：限制 scan 范围为 web controller 包。
- 已修复：`easyorange-order` 的 `OrderTestApplication` 已将 `@ComponentScan` 从扫描整个包改为仅扫描 `adapter.inbound.web`

### framework 模块集成测试

`easyorange-framework` 的集成测试（Redis Cache/OutboxRepository）使用 Testcontainers，必须标注 `@Tag("integration")`。已配置 `surefire excludedGroups=integration`，默认 `mvn test` 跳过；需执行时使用 `-DexcludedGroups=""`：

```bash
./mvnw test -pl easyorange-framework -DexcludedGroups=""
```

### MapStruct + IntelliJ 误报

`@Mapper(componentModel = "spring")` 会在生成类上加 `@Component`，但 IntelliJ 的 Spring 插件静态分析会同时将接口上的 `@Mapper(componentModel = "spring")` 和生成类上的 `@Component` 都计为 bean，导致误报 "存在多个 XxxMapper 类型的 Bean"。

**实际上运行时只有 1 个 bean**（MapStruct 生成的实现类），`@MapperScan(annotationClass = org.apache.ibatis.annotations.Mapper.class)` 不会注册 MapStruct 接口。

**修复方式**：在注入字段上加 `@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")`：

```java
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
private final UserEntityMapper entityMapper;
```

当前已修复：`UserEntityMapper`（UserRepositoryImpl）、`UserAssembler`（UserLoginAppService / UserAppService）。新增 MapStruct mapper 后按此方式处理即可。
