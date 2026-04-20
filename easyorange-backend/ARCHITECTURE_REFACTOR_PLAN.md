# EasyOrange 后端架构改造计划：简易 DDD + CQRS

## 一、现状分析

### 1.1 当前架构概况

```
easyorange-backend/
├── easyorange-application     # 启动类、架构测试
├── easyorange-common          # 公共基础设施
├── easyorange-framework       # 框架层(AOP/Config/Security)
├── easyorange-user            # 用户模块 (传统Service+Mapper)
├── easyorange-product         # 商品模块 (混合模式)
├── easyorange-order           # 订单模块 (CQRS + DDD)
├── easyorange-payment         # 支付模块 (CQRS + DDD)
└── easyorange-message         # 消息模块 (传统Service+Mapper)
```

### 1.2 模块架构成熟度分级

| 模块 | 架构模式 | 成熟度 |
|------|---------|--------|
| **Order** (订单) | CQRS + DDD Aggregate + EventSourcing | ⭐⭐⭐⭐⭐ 领先 |
| **Payment** (支付) | CQRS + DDD Aggregate | ⭐⭐⭐⭐⭐ 领先 |
| **Product** (商品) | CQRS + DDD Aggregate | ⭐⭐⭐⭐ 良好 |
| **User** (用户) | 传统 Service + MyBatis-Plus | ⭐⭐ 基础 |
| **Message** (消息) | 传统 Service + MyBatis-Plus | ⭐⭐ 基础 |

### 1.3 已有的 DDD/CQRS 基础设施

#### 领先模块已实现：

**Order 模块示例结构：**
```
order/
├── domain/
│   ├── aggregate/
│   │   └── OrderAggregate.java          # 聚合根 + 领域事件注册
│   ├── valueobject/                      # 21个值对象
│   │   ├── OrderId, OrderNo, OrderAmount
│   │   ├── BuyerId, SellerId
│   │   └── ShippingContact, CancellationReason
│   ├── event/                           # 领域事件
│   │   ├── OrderCreatedEvent
│   │   ├── OrderPaidEvent
│   │   └── OrderShippedEvent...
│   └── repository/                       # 仓储接口
│       ├── OrderRepository.java
│       ├── OrderReadRepository.java
│       └── MybatisOrderRepository.java
├── application/
│   ├── command/
│   │   ├── OrderCommandHandler.java     # 命令处理器
│   │   └── CreateOrderCommand.java
│   └── query/
│       ├── OrderQueryHandler.java        # 查询处理器
│       └── OrderQuery.java
└── controller/
    ├── OrderCommandController.java       # 命令接口
    └── OrderQueryController.java        # 查询接口
```

**Product 模块示例结构：**
```
product/
├── domain/
│   ├── aggregate/
│   │   └── ProductAggregate.java
│   ├── valueobject/                      # 15个值对象
│   │   ├── Money, StockQuantity, Version
│   │   └── ProductTitle, ProductDescription
│   └── event/
│       ├── ProductCreatedEvent
│       └── StockDecreasedEvent
├── application/
│   ├── command/
│   │   ├── ProductCommandHandler.java
│   │   └── ProductQueryHandler.java
│   └── cache/
│       └── ProductReadCache.java         # 读写分离缓存
└── controller/
    └── ProductController.java
```

### 1.4 当前存在的问题

#### 问题 1: 模块间架构不一致 ⭐⭐⭐⭐⭐
- **Order/Payment/Product** 已实现 CQRS+DDD
- **User/Message** 仍使用传统三层架构（Controller-Service-Mapper）
- **后果**：代码风格不统一，维护成本增加，新成员学习曲线陡峭

#### 问题 2: 缺失 BaseAggregateRoot 基类 ⭐⭐⭐⭐
- Order 和 Product 的 Aggregate 直接继承 `AggregateRoot<OrderId>`
- 但 `common` 模块中没有找到 `AggregateRoot.java` 文件
- **推测**：可能使用了外部依赖或存在于其他位置

#### 问题 3: User 模块无领域模型 ⭐⭐⭐⭐
- User 没有值对象（UserId 直接使用 Long）
- 没有 UserAggregate
- 所有业务逻辑在 UserServiceImpl 中（120+ 行）
- 密码校验/注册逻辑缺少领域验证

#### 问题 4: Message 模块无 CQRS 分离 ⭐⭐⭐
- 消息模块混合了命令和查询逻辑
- MessageService 既是命令处理又是查询处理
- 缺少 MessageCommandHandler/MessageQueryHandler

#### 问题 5: 缺少通用基础设施 ⭐⭐⭐
- `common` 模块有 `QueryResult.java` 但为空接口
- 没有统一的 Command/Query 基类
- 没有通用的事件发布机制（部分代码引用了 DomainEventPublisher 但未找到实现）

---

## 二、改造方案：简易 DDD + CQRS

### 2.1 改造原则

> **"求同存异，逐步演进，不强制一致"**

1. **领先模块（Order/Payment/Product）**：保持现状，持续优化
2. **基础模块（User/Message）**：引入 CQRS，分阶段引入 DDD
3. **通用基础设施**：在 common 中补全缺失的 DDD 基类

### 2.2 目标架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller Layer                       │
│   CommandController (写) ←→  QueryController (读)           │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│   CommandHandler (命令)    ←→    QueryHandler (查询)        │
│        ↓                            ↓                       │
│   Aggregate.create()         ReadRepository.find()          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│   Aggregate (聚合根)    ValueObject (值对象)    Event (事件)  │
│   Repository (仓储接口)  DomainService (领域服务)            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                      │
│   MybatisRepository    Cache (Redis)    DomainEventPublisher│
└─────────────────────────────────────────────────────────────┘
```

### 2.3 详细改造计划

---

## Phase 1: 通用基础设施补全（1-2周）

### 任务 1.1: 创建 DDD 基类

**文件**: `easyorange-common/src/main/java/com/cartethyia/easyorange/common/ddd/`

```java
// AggregateRoot.java - 聚合根基类
public abstract class AggregateRoot<ID extends ValueObject> {
    private final List<BaseDomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(BaseDomainEvent event) {
        domainEvents.add(event);
    }

    public List<BaseDomainEvent> releaseEvents() {
        List<BaseDomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public abstract ID id();
}

// Entity.java - 实体基类
public abstract class Entity<ID extends ValueObject> {
    protected ID id;
    public abstract ID id();
}

// ValueObject.java - 值对象基类
public abstract class ValueObject {
    @Override
    public abstract boolean equals(Object o);
    @Override
    public abstract int hashCode();
}
```

### 任务 1.2: 创建 CQRS 基类

**文件**: `easyorange-common/src/main/java/com/cartethyia/easyorange/common/cqrs/`

```java
// Command.java - 命令基类
public interface Command {
    Long getId();
}

// Query.java - 查询基类
public interface Query {
}

// CommandHandler.java - 命令处理器接口
public interface CommandHandler<C extends Command, R> {
    R handle(C command);
}

// QueryHandler.java - 查询处理器接口
public interface QueryHandler<Q extends Query, R> {
    R handle(Q query);
}

// CommandGateway.java - 命令网关（可选）
public interface CommandGateway {
    <C extends Command, R> R send(C command);
}
```

### 任务 1.3: 创建事件发布基础设施

**文件**: `easyorange-common/src/main/java/com/cartethyia/easyorange/common/event/`

```java
// BaseDomainEvent.java
public abstract class BaseDomainEvent implements Serializable {
    private final long timestamp = System.currentTimeMillis();
    public long getTimestamp() { return timestamp; }
}

// DomainEventPublisher.java
public interface DomainEventPublisher {
    void publish(BaseDomainEvent event);
    void publish(Collection<BaseDomainEvent> events);
}

// DomainEventSubscriber.java
public interface DomainEventSubscriber<T extends BaseDomainEvent> {
    Class<T> getEventType();
    void handle(T event);
}
```

---

## Phase 2: User 模块改造（2-3周）

### 任务 2.1: 创建 User 领域模型

```
user/domain/
├── aggregate/
│   └── UserAggregate.java              # 用户聚合根
├── valueobject/
│   ├── UserId.java
│   ├── Username.java
│   ├── EncryptedPassword.java
│   └── UserStatus.java
├── repository/
│   ├── UserCommandRepository.java     # 命令仓储
│   └── UserReadRepository.java         # 查询仓储
├── service/
│   └── PasswordVerificationService.java  # 领域服务(已有)
└── event/
    ├── UserRegisteredEvent.java
    └── UserPasswordChangedEvent.java
```

### 任务 2.2: 创建 User Command/Query 分离

```
user/application/
├── command/
│   ├── UserCommandHandler.java
│   ├── RegisterUserCommand.java
│   ├── UpdateUserCommand.java
│   └── ChangePasswordCommand.java
└── query/
    └── UserQueryHandler.java
```

### 任务 2.3: 重构 Controller

```java
// UserCommandController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCommandController {
    private final UserCommandHandler commandHandler;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterUserCommand command) {
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordCommand command) {
        commandHandler.handle(command);
        return Result.success();
    }
}

// UserQueryController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserQueryController {
    private final UserQueryHandler queryHandler;

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        return Result.success(queryHandler.getCurrentUserInfo());
    }
}
```

### 任务 2.4: UserService 重构为防腐层（可选）

保留 UserService 作为对遗留代码的防腐层，内部委托给 CommandHandler/QueryHandler。

---

## Phase 3: Message 模块改造（1-2周）

### 任务 3.1: 创建 Message 领域模型

```
message/domain/
├── aggregate/
│   └── MessageAggregate.java          # 消息聚合根
├── valueobject/
│   ├── MessageId.java
│   ├── MessageContent.java
│   └── ReadStatus.java
└── repository/
    ├── MessageCommandRepository.java
    └── MessageReadRepository.java
```

### 任务 3.2: 创建 Message Command/Query 分离

```
message/application/
├── command/
│   ├── MessageCommandHandler.java
│   ├── SendMessageCommand.java
│   └── MarkAsReadCommand.java
└── query/
    └── MessageQueryHandler.java
```

---

## Phase 4: 读写分离缓存优化（贯穿始终）

### 任务 4.1: 统一缓存接口

```java
// ReadCache.java - 读缓存接口
public interface ReadCache<K, V> {
    Optional<V> get(K key);
    void put(K key, V value);
    void invalidate(K key);
}

// RedisReadCache.java - Redis实现
@Component
public class RedisReadCache<K, V> implements ReadCache<K, V> {
    private final RedisTemplate<String, Object> redisTemplate;
    // 实现get/put/invalidate
}
```

### 任务 4.2: QueryHandler 统一使用缓存

所有 QueryHandler 的 `getXxxDetail()` 方法应优先从缓存读取，缓存miss时查库并回填缓存。

---

## Phase 5: 代码质量提升（贯穿始终）

### 任务 5.1: 统一异常处理

**问题**: 当前代码大量使用 `RuntimeException("错误信息")`

**改造**: 统一使用项目自定义异常

```java
// Good
throw new BusinessException(UserResultCode.USERNAME_ALREADY_EXISTS);

// Bad
throw new RuntimeException("用户名已存在");
```

### 任务 5.2: 统一校验

**问题**: OrderAggregate 中有 `BizRequire.isFalse()`

**建议**: 统一使用 `BizRequire` 进行业务规则校验

### 任务 5.3: 日志规范化

```java
// Good - 结构化日志
log.info("action=create_order orderId={} userId={}", orderId, userId);

// Bad - 字符串拼接
log.info("创建订单成功，订单ID：" + orderId);
```

---

## 三、优先级与时间估算

| 优先级 | 任务 | 工作量 | 风险 |
|--------|------|--------|------|
| P0 | Phase 1: DDD基类创建 | 1周 | 低 |
| P0 | Phase 2.1-2.2: User领域模型 | 1周 | 中 |
| P1 | Phase 2.3: User Controller重构 | 3天 | 中 |
| P1 | Phase 3: Message模块改造 | 2周 | 低 |
| P2 | Phase 4: 统一缓存 | 1周 | 低 |
| P2 | Phase 5: 代码质量 | 持续 | 低 |

---

## 四、关键优化建议

### 优化 1: 引入六边形架构（Ports & Adapters）

```
                    ┌─────────────────────┐
                    │   Driving Adapter    │
                    │  (Controller/API)   │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │    Application      │
                    │   (Command/Query)   │
                    └──────────┬──────────┘
                               ↓
         ┌─────────────────────┼─────────────────────┐
         ↓                                         ↓
┌─────────────────┐                       ┌─────────────────┐
│  Domain Model   │                       │     Ports       │
│  (Aggregates)   │←─────────────────────→│ (Repositories)  │
└─────────────────┘                       └────────┬────────┘
                                                     ↓
                                            ┌─────────────────┐
                                            │ Driven Adapter  │
                                            │ (MyBatis/Redis) │
                                            └─────────────────┘
```

### 优化 2: 引入 Saga 模式处理跨聚合事务

当前 Order 创建时依赖 ProductSnapshotPort 获取商品快照，这是跨聚合调用。建议：

1. 使用事件驱动：Product 价格变化 → 发布 ProductPriceChangedEvent
2. Order 创建时订阅事件，确保数据一致性

### 优化 3: 引入 CQRS EventStore（可选）

当前 DomainEvent 仅发布到内存，若系统复杂度提升，可考虑：

1. 持久化 DomainEvent 到 EventStore
2. 支持 Event Replay 实现最终一致性

---

## 五、架构成熟度目标

| 阶段 | Order | Payment | Product | User | Message |
|------|-------|---------|---------|------|---------|
| 当前 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| Phase 1后 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| Phase 2后 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| Phase 3后 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 最终目标 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 六、立即可执行的下一步

1. **确认 AggregateRoot 基类位置**：搜索整个项目确认 `AggregateRoot.java` 实际位置
2. **创建 common/ddd 包**：在 easyorange-common 中创建 DDD 基类
3. **改造 User 模块**：从 Register/ChangePassword 开始引入 CQRS
4. **统一异常类**：将 `RuntimeException` 替换为项目自定义异常
