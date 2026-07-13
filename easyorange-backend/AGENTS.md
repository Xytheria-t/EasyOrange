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
| **创建** (create/register/add) | `String` (ID) | 客户端需要获取新资源标识；服务端通过 `IdGenerator`（UUID v7）生成 | `createProduct()`, `register()`, `createReview()` |
| **命令/更新/删除** (update/delete/remove/handle/put/take/mark/submit/cancel/process) | `void` | 命令不返回值；前端通过 React Query 的 `invalidateQueries` 重新拉取最新数据 | `updateProduct()`, `deleteProduct()`, `addFavorite()`, `handleReport()`, `putOnline()` |
| **批量操作** 可能返回结果 DTO（如 `BatchAuditResultResponse`），因需要聚合成功率/失败信息

> 背景：务实混合约定——不是严格 CQRS，也不是 RESTful 完整资源返回。Spring Boot + TanStack Query 上下文下的最佳平衡。

## 数据对象基类

```java
public class BaseDO {
    @TableId(type = IdType.INPUT)
    private String id;
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
| 集成测试 | — | 已移除（WSL2 Docker 兼容性限制，全量改为单元测试） |
| 架构测试 | ArchUnit | DDD 分层合规、包依赖规则 |
| 控制器测试 | MockMvc | API 端点 |
| 覆盖率报告 | JaCoCo 0.8.12 | `prepare-package` 阶段生成报告 (`jacoco:report`)，门禁移到了 CI 层 |
| 依赖安全 | OWASP Dependency Check 12.1.0 | `verify` 阶段检查，CVSS ≥ 8 阻断构建 |

架构守卫测试位于 `easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java`。

**TestSecurityUtil 模式**：测试中设置 `SecurityContextHolder` 统一使用 `TestSecurityUtil.setSecurityContext(userId)`（位于 `easyorange-framework/src/main/java/.../framework/util/TestSecurityUtil.java`），替代 `mockStatic(SecurityContextUtil.class)`。`clearSecurityContext()` 必须在 `finally` 块中调用保证测试间隔离。

## Flyway 迁移规范

- DDL 脚本: `db/migration/V{N}__description.sql`
- 开发数据: `db/dev/R__insert_dev_test_data.sql`
- 项目开发阶段的所有 V 迁移已合并为单个 `V1__init_schema.sql`（完整当前 DDL）
- 后续 DDL 变更按递增版本号添加 `V{N+1}__description.sql`
- **禁止修改已执行的迁移脚本**（生产环境原则；开发阶段若需重置，清库重跑即可）
- 新增字段必须可空或有默认值
- DDL 迁移中 DROP INDEX / ADD INDEX 使用 MySQL 8.0 原生 DDL（非阻塞 INPLACE 算法），允许生产环境在线执行

## 安全要点

> **标准 API 优先（STP）**: 认证/授权相关功能优先使用 Spring Security 标准机制。有 `oauth2ResourceServer()` 就不要手写 Filter；有 `JwtDecoder`/`JwtEncoder` 就注入使用，不要手写 JWT 工具类。参考：JwtAuthenticationFilter + JJWT → Spring Security OAuth2 Resource Server 迁移。

- JWT 双 Token: Access Token (短期) + Refresh Token (长期)
- 密码: BCrypt 加密存储
- 限流: `RateLimitFilter` 配置驱动，GET 走本地限流（默认 200次/60秒/IP），写操作走 Redis 分布式限流（默认 30次/60秒/IP），Redis 不可用时放行（fail-open）。支持 `@SkipRateLimit` 按 Controller 方法/类跳过
- 防重（短时间连点防护）: `RateLimitFilter` 约定式拦截所有 POST/PUT/DELETE/PATCH（默认 3秒间隔），key 含请求体 hash，Redis 不可用时放行。支持 `@SkipRepeatSubmit` 跳过低级别防重
- 幂等（协议级防重放）: `@Idempotent` 注解 + `IdempotencyAspect(@Order=1)`，客户端提供 `Idempotency-Key` 头（UUID），服务端缓存成功响应 24h。与 `RateLimitFilter` 的短时间防重互补——前者防连点，后者防重放。支持自定义 header 名称和 TTL
- 审计日志: 约定式自动记录所有写操作 (@Order 3), 无需注解, 异步持久化, 敏感字段自动掩码
- XSS: `Content-Security-Policy` 头 (`default-src 'none'`)，已废除 `X-XSS-Protection`
- CORS: 生产环境严格白名单
- 全局认证: `SecurityConfig` 的 `.anyRequest().authenticated()` 已拦截所有未认证请求，Controller 上无需 `@PreAuthorize("isAuthenticated()")`

Filter 执行顺序: RateLimitFilter(0) → SecurityConfig.oauth2ResourceServer() (Spring Security Filter Chain) → AuditLogAspect(AOP @Order 3)

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
String userId = authAppService.register(request.username(), request.password());
return Result.success(userId);
```

命名变量仅在以下场景保留：
- 调用与返回之间有**条件分支**或**后处理**
- 对返回值有**多步骤转换**（如 `builder().id(x).build()` 再 wrap）
- 变量名承载了**非显而易见的语义**（如 `var pageReq = PageRequest.builder().pageNum(pageNum).pageSize(pageSize).build()` 在后续多个操作中使用）

## 踩坑警示

### MyBatis-Plus UUID / Jackson 事件反序列化

MyBatis-Plus **无内置** `UUID` TypeHandler，PO 含 UUID 字段时 insert 报 `Type handler was null`。已配全局 `UuidTypeHandler`（`framework/config/database/`）+ `type-handlers-package`，新增 PO 的 UUID 字段无需额外配置。数据库列类型 `CHAR(36)`。

领域事件 record 无需 `@JsonCreator`，反序列化依赖 Jackson 3 的 `ParameterNamesModule`（由 Spring Boot 4 自动配置，无需显式声明依赖）。新增事件 record 实现 `DomainEvent` 接口即可，无需任何 Jackson 注解。

### Spring Boot 4 @WebMvcTest 路径变化

Spring Boot 4.0 迁移到 `org.springframework.boot.webmvc.test` 包。规则：① 新 import 路径；② 无 `@SpringBootConfiguration` 的模块在 test 下创建空 `@SpringBootApplication` 类；③ `@ComponentScan` 限于 web controller 包，否则拉入 persistence 类导致切片失败。参考 `easyorange-order` 的 `OrderTestApplication`。

### Port/Adapter / MapStruct IntelliJ 误报

IntelliJ 将 domain port 接口也识别为 Spring Bean，与 `@Component` Adapter 冲突。**修复**：Adapter 实现类加 `@Primary`。

`@Mapper(componentModel = "spring")` 接口同理。**修复**：构造器注入加 `@Qualifier("xxxImpl")`；字段注入加 `@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")`。

### MyBatis SQL 注入：禁止在 @Select 中使用 ${} 拼接 IN 列表

`CategoryMapper.countProductsByCategoryIds` 曾使用 `IN (${ids})` 拼接逗号分隔字符串，存在 SQL 注入风险且阻止查询计划缓存。

**已修复（2026-05-25，2026-06-29 改 List<String>）**：改用 `<script>` + `<foreach item='id' collection='categoryIds' ...>#{id}</foreach>` + 参数类型（Long→String 迁移后为 `List<String>`）。

**注意**：新增 Mapper 的 IN 列表查询必须使用 `<foreach>` + `#{}` 参数化方式，禁止 `String` 类型的括号内 JSON/CSV 拼接。

### JDK 25 + Lombok Unsafe 终端弃用

JDK 23+ 终端弃用 `sun.misc.Unsafe::objectFieldOffset`，Lombok 1.18.46 仍使用它。启动时打印 `WARNING: sun.misc.Unsafe::objectFieldOffset has been called by lombok.permit.Permit`。

**已配置**：编译阶段 `.mvn/jvm.config` + 运行阶段 `spring-boot-maven-plugin jvmArguments` 均已设置 `--sun-misc-unsafe-memory-access=allow`。JDK 26+ 默认变 `deny`，届时需升级 Lombok。

### CategoryCacheAdapter 熔断

Redis 连接失败时自动降级 DB，连续 5 次失败熔断（跳过 Redis 60s），成功后重置计数器。ERROR 日志含结构化字段。新增缓存适配器时遵循此模式。

### Admin 模块端口接口

Admin 模块**禁止直接依赖其他模块的 Mapper/DO**，必须通过 `domain/port/`（`AdminProductQueryPort`, `AdminUserQueryPort`, `AdminOrderQueryPort`）接口查询，适配器在 `easyorange-application/adapter/outbound/admin/` 实现。
