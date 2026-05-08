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
| JJWT | 0.12.6 |
| ArchUnit | 1.4.1 |

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

## CQRS 模式

product、order、payment 模块使用 CQRS：

- **Command 侧**: `adapter/inbound/web/*CommandController` → `application/command/*CommandHandler` → `domain`
- **Query 侧**: `adapter/inbound/web/*QueryController` → `application/query/*QueryHandler` → `domain/repository/query/`

读写使用不同的 Repository 接口和数据模型。

## 领域事件机制

```java
@PublishEvent(type = "UserRegistered", extractor = "userRegisteredEventExtractor")
@Transactional(rollbackFor = Exception.class)
public Long register(RegisterRequest request) { ... }
```

- `@PublishEvent` 注解标记需要发布事件的方法
- AOP 切面在事务提交后异步发布事件
- EventExtractor 负责从方法参数/返回值提取事件数据
- 事件持久化到 `eo_domain_event` 表，保证可靠投递

## 跨模块通信

**当前状态**：部分模块存在直接 Maven 依赖（order→product/user/payment, product→user, message→user, favorite→product）

**隔离方式**：
- 被调用方定义 `port/output/` 接口（如 `ProductInventoryPort`）
- 调用方在 `adapter/outbound/messaging/` 实现适配器（如 `ProductInventoryAdapter`）
- 或通过 ACL 服务隔离（如 favorite 的 `ProductAclService`）

**演进方向**：逐步消除直接 Maven 依赖，改为事件驱动 + Outbox 模式

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
| 领域服务 | `*DomainService` | `PasswordDomainService` |
| 应用服务 | `*AppService` / `*CommandHandler` | `UserAppService` |
| 仓储接口 | `*Repository` | `UserRepository` |
| 仓储实现 | `*RepositoryImpl` / `Mybatis*Repository` | `UserRepositoryImpl` |
| 出站端口 | `*Port` | `PaymentGatewayPort` |
| 控制器 | `*Controller` | `AuthController` |
| 请求 DTO | `*Request` / `*Command` | `LoginRequest` |
| 响应 DTO | `*Response` / `*VO` | `UserVO` |
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
