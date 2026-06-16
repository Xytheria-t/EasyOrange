# EasyOrange Backend 编码指南

Spring Boot 4.0.3 + Java 25 后端，采用 DDD + 六边形架构。

## 技术栈版本

| 依赖 | 版本 |
|------|------|
| Java | 25 |
| Spring Boot | 4.0.3 |
| MyBatis-Plus | 3.5.16 |
| MapStruct | 1.6.3 |
| Immutables | 2.10.0 |
| Flyway | 11.15.0 |
| Spring Security OAuth2 Resource Server | — |
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
│  valueobject/: 值对象 (record / Immutables)  │
│  event/: 领域事件                            │
│  port/: 出站端口接口                          │
│  repository/: 仓储接口                       │
│  service/: 领域服务                          │
└─────────────────────────────────────────────┘
```

**关键约束**：
- `domain` 层禁止依赖 Spring 框架、MyBatis、Redis 等基础设施
- `domain` 层通过 `port/` 接口与外部交互，由 `adapter/outbound/` 实现
- `application` 层编排业务流程，事务边界在此层
- `adapter/inbound/` 仅做参数校验和 DTO 转换，不含业务逻辑
- **查询方法只读事务**: `application/service/` 和 `application/query/` 下的纯查询方法（find/get/list/query/count 等）**必须**标注 `@Transactional(readOnly = true)`；写操作方法使用 `@Transactional(rollbackFor = Exception.class)`。这是项目级约定，所有模块（user/product/order/payment/message/favorite/admin）一致遵循

## CQRS 模式

product、order、payment 模块使用 CQRS：

- **Command 侧**: `adapter/inbound/web/*CommandController` → `application/command/*CommandHandler` → `domain`
- **Query 侧**: `adapter/inbound/web/*QueryController` → `application/query/*QueryHandler` → `domain/repository/query/`

读写使用不同的 Repository 接口和数据模型。

## 领域事件机制

应用服务注入 `DomainEventPublisher` 发布事件，框架层通过 RabbitMQ Topic Exchange 分发：

```java
// 应用服务
private final DomainEventPublisher eventPublisher;
eventPublisher.publish(new SomeEvent(...));
```

- `RabbitMQDomainEventPublisher`（`@Primary`）将事件发布到 `eo.domain.events` Topic Exchange
- 路由键由事件类名自动派生（`ProductCreatedEvent` → `product.created`），无需手动注册
- 每个消费者独占队列（`eo.{name}`），失败消息路由到 DLQ（`eo.{name}.dlq`）+ 指数退避重试
- 多方法消费者使用类级 `@RabbitListener` + 方法级 `@RabbitHandler`（类型分发，非轮询竞争）
- 各模块通过 `@RabbitListener` 注解的消费者异步处理事件（9 个消费者，见根目录 AGENTS.md）
- `@ConditionalOnProperty(matchIfMissing=true)` 确保无 RabbitMQ 环境开发/测试正常启动

## 跨模块通信

**当前状态**：所有跨模块依赖已通过端口接口 + 适配器模式隔离，Maven 依赖标记为 `<optional>true</optional>`。

**隔离方式**：
- 调用方模块定义 `domain/port/` 接口（如 `ProductInventoryPort`）
- 适配器实现在 `easyorange-application/adapter/outbound/` 包下
- Maven 依赖标记为 `<optional>true</optional>` 实现编译期隔离

**事件驱动**：
- 写操作通过领域事件解耦（如 `PaymentInitiationRequestedEvent`、`StockReservationRequestedEvent`）
- 事件监听器在 `easyorange-application/adapter/event/` 包下（机制与第 3 节"领域事件"一致）

**查询操作**：保留同步端口调用（如 `getSnapshot()`），通过可选依赖实现

**事件流**：
```
OrderCreatedEvent → OrderSagaEventConsumer → StockReservationRequestedEvent → StockReservationEventConsumer → ProductCommandService.decrementStock()
PaymentInitiationRequestedEvent → PaymentInitiationEventConsumer → PaymentCommandHandler.handle()
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
| 领域事件 | `*Event` | `OrderCreatedEvent` |
| 领域服务 | `*Service` | `AuthenticationService` |
| 应用服务 | `*AppService` / `*CommandHandler` | `AuthAppService`, `ProfileAppService` |
| 仓储接口 | `*Repository` | `UserRepository` |
| 仓储实现 | `*RepositoryImpl` / `Mybatis*Repository` (继承 `BaseRepository`) | `UserRepositoryImpl extends BaseRepository<UserMapper, UserEntity>` |
| 出站端口 | `*Port` | `PaymentGatewayPort` |
| 控制器 | `*Controller` | `AuthController` |
| 请求 DTO | `*Request` | `PasswordLoginRequest`, `RegisterRequest` |
| 响应 DTO | `*Response` / `*Response` | `UserResponse` |
| 数据对象 | `*DO` / `*PO` | `UserEntity`, `PaymentPO` |

## 服务层方法返回值约定

应用服务（`application/service/`、`application/command/`）的 public 方法遵循以下约定：

| 操作类型 | 返回值 | 说明 | 示例 |
|---------|--------|------|------|
| **创建** (create/register/add) | `Long` (ID) | 客户端需要获取新资源标识；服务端生成 Snowflake ID | `createProduct()`, `register()`, `createReview()` |
| **命令/更新/删除** (update/delete/remove/handle/put/take/mark/submit/cancel/process) | `void` | 命令不返回值；前端通过 React Query 的 `invalidateQueries` 重新拉取最新数据 | `updateProduct()`, `deleteProduct()`, `addFavorite()`, `handleReport()`, `putOnline()` |
| **批量操作** 可能返回结果 DTO（如 `BatchAuditResultResponse`），因需要聚合成功率/失败信息

> 背景：务实混合约定——不是严格 CQRS，也不是 RESTful 完整资源返回。Spring Boot + TanStack Query 上下文下的最佳平衡。

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
| 覆盖率报告 | JaCoCo 0.8.12 | `prepare-package` 阶段生成报告 (`jacoco:report`)，门禁移到了 CI 层 |
| 依赖安全 | OWASP Dependency Check 12.1.0 | `verify` 阶段检查，CVSS ≥ 8 阻断构建 |

架构守卫测试位于 `easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java`。

**TestSecurityUtil 模式**：测试中设置 `SecurityContextHolder` 统一使用 `TestSecurityUtil.setSecurityContext(userId)`（位于 `easyorange-framework/src/main/java/.../framework/util/TestSecurityUtil.java`），替代 `mockStatic(SecurityContextUtil.class)`。`clearSecurityContext()` 必须在 `finally` 块中调用保证测试间隔离。

## Flyway 迁移规范

- DDL 脚本: `db/migration/V{N}__description.sql`
- 开发数据: `db/dev/R__insert_dev_test_data.sql`
- 禁止修改已执行的迁移脚本
- 新增字段必须可空或有默认值
- DDL 迁移中 DROP INDEX / ADD INDEX 使用 MySQL 8.0 原生 DDL（非阻塞 INPLACE 算法），允许生产环境在线执行

## 安全要点

> **标准 API 优先（STP）**: 认证/授权相关功能优先使用 Spring Security 标准机制。有 `oauth2ResourceServer()` 就不要手写 Filter；有 `JwtDecoder`/`JwtEncoder` 就注入使用，不要手写 JWT 工具类。参考：JwtAuthenticationFilter + JJWT → Spring Security OAuth2 Resource Server 迁移。

- JWT 双 Token: Access Token (短期) + Refresh Token (长期)
- 密码: BCrypt 加密存储
- 限流: `RateLimitFilter` 配置驱动，GET 走本地限流（默认 200次/60秒/IP），写操作走 Redis 分布式限流（默认 30次/60秒/IP），Redis 不可用时放行（fail-open）。支持 `@SkipRateLimit` 按 Controller 方法/类跳过
- 防重: `RateLimitFilter` 约定式拦截所有 POST/PUT/DELETE/PATCH（默认 3秒间隔），key 含请求体 hash 防误判，Redis 不可用时放行。支持 `@SkipRepeatSubmit` 按 Controller 方法/类跳过
- 操作日志: 约定式自动记录所有写操作 (@Order 3), 无需注解
- XSS: `XssFilter` + `XssHttpServletRequestWrapper`
- CORS: 生产环境严格白名单
- 全局认证: `SecurityConfig` 的 `.anyRequest().authenticated()` 已拦截所有未认证请求，Controller 上无需 `@PreAuthorize("isAuthenticated()")`

Filter 执行顺序: RateLimitFilter(0) → SecurityConfig.oauth2ResourceServer() (Spring Security Filter Chain) → XssFilter → OperLogAspect(AOP @Order 3)

JWT 认证由 Spring Security OAuth2 Resource Server 的 `JwtDecoder` + `JwtAuthenticationConverter` 处理，无需自定义 Servlet Filter。认证流程：`BearerTokenAuthenticationFilter` (Spring Security 内置) → `JwtDecoder` 验证签名 + 黑名单/强制登出检查 → `JwtAuthenticationConverter` 构造 `AuthUser` 并设置 `SecurityContext`。

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

## Java `var` 使用规范

局部变量推荐使用 `var` 的场景：同一类型构造器（`Foo x = new Foo()` → `var x = new Foo()`）、显式 cast（`Type x = (Type) expr` → `var x = (Type) expr`）、StringBuilder/ByteArrayOutputStream 等无泛型构造器。**不推荐**的场景：接口类型到实现类型的赋值（`List<X> x = new ArrayList<>()` → 保持 `List<X>`，使用 `var` 会丢失接口抽象）

## Controller 响应内联约定

Controller 方法中，当服务调用结果直接传递给 `Result.success()` 且无任何转换/条件逻辑时，**内联为单表达式**，不引入中间变量：

```java
// ✅ 推荐
return Result.success(authAppService.register(request.username(), request.password()));
return Result.success(queryService.getProductById(id));

// ❌ 避免
Long userId = authAppService.register(request.username(), request.password());
return Result.success(userId);
```

命名变量仅在以下场景保留：
- 调用与返回之间有**条件分支**或**后处理**
- 对返回值有**多步骤转换**（如 `builder().id(x).build()` 再 wrap）
- 变量名承载了**非显而易见的语义**（如已规范化的请求对象 `var normalized = request.normalized()` 在后续多个参数中使用）

## 踩坑警示

### MyBatis-Plus UUID TypeHandler

MyBatis-Plus **没有内置** `java.util.UUID` 的 TypeHandler。如果 PO 类中有 `UUID` 字段，直接 insert/update 会报：

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

如果移除 `JacksonConfig` 中的 `ParameterNamesModule` 或遗漏 `jackson-module-parameter-names` 依赖，RabbitMQ 消费者反序列化事件时会报：

```
InvalidDefinitionException: Cannot construct instance of XxxEvent (no Creators, like default constructor, exist)
```

**已配置位置**：
1. `framework/pom.xml` — `jackson-module-parameter-names` 依赖
2. `framework/.../JacksonConfig.java` — `mapper.registerModule(new ParameterNamesModule())`

**注意**：新增领域事件类时无需添加任何 Jackson 注解，遵循现有模式即可。

### Spring Boot 4 @WebMvcTest 路径变化

Spring Boot 4.0 将 `@WebMvcTest` / `@AutoConfigureMockMvc` 迁移到 `org.springframework.boot.webmvc.test.autoconfigure.web.servlet` 包。

**三条规则**：
1. 使用新 import 路径（`...webmvc.test...`）
2. 无 `@SpringBootConfiguration` 的模块在 test 下创建 `@SpringBootApplication` 空类
3. `@ComponentScan` 限制扫描范围为 web controller 包，否则拉入 persistence 类导致 web 切片失败

已修复示例：`easyorange-order` 的 `OrderTestApplication` 已限制为 `adapter.inbound.web`

### framework 模块集成测试

`easyorange-framework` 的集成测试（`RedisCacheImplIntegrationTest`、`RabbitMQDomainEventPublisherIT`）使用 Testcontainers，必须标注 `@Tag("integration")`。已配置 `surefire excludedGroups=integration`，默认 `mvn test` 跳过；需执行时使用 `-DexcludedGroups=""` （`./mvnw test -pl easyorange-framework -DexcludedGroups=""`）

### Port/Adapter IntelliJ 误报

IntelliJ Spring 插件将 domain port 接口文件也识别为 Spring Bean，与 `@Component` Adapter 冲突，误报 "存在多个 XxxPort 类型的 Bean"。

**修复**：Adapter 实现类上加 `@Primary`：

```java
@Primary
@Component
public class PasswordEncoderAdapter implements PasswordEncoderPort { ... }
```

`@Primary` 语义正确（Adapter 是 port 的默认实现）。新增 Port/Adapter 后按此方式处理。

### MapStruct + IntelliJ 误报

`@Mapper(componentModel = "spring")` 的接口和生成类都被 IntelliJ 计为 bean，误报 "存在多个 XxxMapper 类型的 Bean"。运行时只有 1 个 bean（`MapperScan` 只扫描 MyBatis 注解）。

**修复**：
- **构造器注入**（推荐）：参数加 `@Qualifier("xxxImpl")`
- **字段注入**：加 `@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")`

新增 MapStruct mapper 后按此方式处理。

### MyBatis SQL 注入：禁止在 @Select 中使用 ${} 拼接 IN 列表

`CategoryMapper.countProductsByCategoryIds` 曾使用 `IN (${ids})` 拼接逗号分隔字符串，存在 SQL 注入风险且阻止查询计划缓存。

**已修复（2026-05-25）**：改用 `<script>` + `<foreach item='id' collection='categoryIds' ...>#{id}</foreach>` + `List<Long>` 参数类型。

**注意**：新增 Mapper 的 IN 列表查询必须使用 `<foreach>` + `#{}` 参数化方式，禁止 `String` 类型的括号内 JSON/CSV 拼接。

### JDK 25 + Lombok Unsafe 终端弃用警告

启动或编译时出现以下警告，原因是 Lombok 的 `lombok.permit.Permit` 内部使用了 `sun.misc.Unsafe::objectFieldOffset`（JDK 23 起被标记为 terminally deprecated）：

```
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by lombok.permit.Permit
```

**根因**：JDK 25 默认开启 `--sun-misc-unsafe-memory-access=warn`（JEP 498），调用已弃用的 Unsafe 方法时打印警告。Lombok 1.18.46（当前最新版）仍使用 Unsafe，尚在迁移中。

**已生效的解决方案**：
1. 编译阶段：项目根 `.mvn/jvm.config` 已配置 `--sun-misc-unsafe-memory-access=allow`，Maven 构建时自动加载
2. 运行阶段：启动命令（alias `eobe`、`spring-boot-maven-plugin` 的 `jvmArguments`）均已包含该 flag

**未来**：JDK 26+ 该 flag 默认值将变为 `deny`（抛出异常），届时需升级兼容 JDK 26 的 Lombok 版本。
