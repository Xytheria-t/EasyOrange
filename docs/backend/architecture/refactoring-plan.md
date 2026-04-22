# EasyOrange 后端架构重构计划

> **版本**: v1.0  
> **日期**: 2026-04-22  
> **状态**: 待评审

---

## 目录

1. [项目现状分析](#1-项目现状分析)
2. [重构总览](#2-重构总览)
3. [详细任务清单](#3-详细任务清单)
4. [文件重构映射表](#4-文件重构映射表)
5. [验收标准](#5-验收标准)
6. [里程碑](#6-里程碑)
7. [工具与脚本](#7-工具与脚本)

---

## 1. 项目现状分析

### 1.1 当前架构状态

| 模块 | 当前架构 | 完成度 | 主要问题 |
|------|----------|--------|----------|
| **product** | DDD + CQRS 部分实施 | 70% | Service 层混杂，Repository 未完全分离 |
| **order** | DDD + CQRS 已实施 | 90% | 可作为其他模块模板 |
| **payment** | DDD + CQRS 部分实施 | 60% | 缺少策略模式实现，Service 层过重 |
| **user** | 传统分层架构 | 30% | 无 DDD，Service 直接操作 Mapper |
| **message** | 基础实现 | 50% | 缺少 CQRS 分离 |

### 1.2 技术栈

- **框架**: Spring Boot 4.0.3 (Java 25)
- **构建工具**: Maven 多模块项目
- **数据访问**: MyBatis-Plus 3.5.16
- **安全**: Spring Security + JWT
- **缓存**: Redis
- **数据库**: MySQL

---

## 2. 重构总览

### 2.1 目标架构

采用**洋葱架构**结合 **DDD + CQRS** 模式：

```
┌─────────────────────────────────────────────────────────────────┐
│                    洋葱架构 + DDD + CQRS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  基础设施层 (Infrastructure)                              │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │   │
│  │  │  Controller │ │ Repository  │ │  EventPublisher │    │   │
│  │  │  (Web适配器) │ │   (DB适配器) │ │   (MQ适配器)     │    │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  应用层 (Application)                                    │   │
│  │  ┌─────────────────┐    ┌─────────────────────────────┐  │   │
│  │  │   Command Bus   │    │      Query Handler          │  │   │
│  │  │  ┌───────────┐  │    │  ┌─────────────────────┐    │  │   │
│  │  │  │CreateOrder│  │    │  │   OrderQueryHandler │    │  │   │
│  │  │  │  Handler  │  │    │  │                     │    │  │   │
│  │  │  └───────────┘  │    │  └─────────────────────┘    │  │   │
│  │  └─────────────────┘    └─────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  领域层 (Domain)                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │   │
│  │  │  Aggregate  │  │ ValueObject │  │  Domain Service │   │   │
│  │  │  (Order)    │  │ (Money, etc)│  │  (OrderService) │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────────────┐   │   │
│  │  │              Domain Events                          │   │   │
│  │  │   OrderCreated / OrderPaid / OrderShipped          │   │   │
│  │  └─────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  依赖规则：外层 → 内层（只能向内依赖）                              │
│  领域层不依赖任何框架（Spring、MyBatis等）                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 重构路线图（8周）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         重构路线图（8周）                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Week 1-2          Week 3-4          Week 5-6          Week 7-8        │
│    │                  │                  │                  │          │
│    ▼                  ▼                  ▼                  ▼          │
│ ┌──────┐         ┌──────┐         ┌──────┐         ┌──────┐           │
│ │User  │         │Pay-  │         │Prod- │         │集成  │           │
│ │模块  │         │ment  │         │uct   │         │测试  │           │
│ │重构  │         │模块  │         │模块  │         │&优化 │           │
│ └──────┘         └──────┘         └──────┘         └──────┘           │
│                                                                         │
│  优先级: P0        优先级: P0        优先级: P1        优先级: P1        │
│  复杂度: 中        复杂度: 高        复杂度: 中        复杂度: 低        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 详细任务清单

### Phase 1: User 模块重构（Week 1-2）

**目标**：将传统分层架构改造为轻量 DDD 架构

#### Week 1: 领域层建设

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| U-001 | 创建领域值对象 `Email` | P0 | 2h | 封装邮箱格式校验逻辑 |
| U-002 | 创建领域值对象 `Phone` | P0 | 2h | 封装手机号格式校验逻辑 |
| U-003 | 创建领域值对象 `Password` | P0 | 2h | 封装密码强度校验逻辑 |
| U-004 | 创建领域值对象 `Nickname` | P0 | 1h | 封装昵称长度校验 |
| U-005 | 创建领域值对象 `UserId` | P0 | 1h | 封装用户ID类型 |
| U-006 | 创建 `UserAggregate` 聚合根 | P0 | 4h | 包含用户核心业务逻辑 |
| U-007 | 创建领域事件 `UserRegisteredEvent` | P1 | 2h | 用户注册领域事件 |
| U-008 | 创建领域事件 `PasswordChangedEvent` | P1 | 2h | 密码修改领域事件 |

**新增文件清单**：

```
easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/
├── aggregate/
│   └── UserAggregate.java              // 用户聚合根
├── valueobject/
│   ├── UserId.java                     // 用户ID值对象
│   ├── Email.java                      // 邮箱值对象
│   ├── Phone.java                      // 手机号值对象
│   ├── Password.java                   // 密码值对象
│   └── Nickname.java                   // 昵称值对象
├── event/
│   ├── UserRegisteredEvent.java        // 注册事件
│   └── PasswordChangedEvent.java       // 密码修改事件
└── repository/
    └── UserRepository.java             // 仓储接口
```

**代码示例**：

```java
// 重构前 (UserServiceImpl.java)
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        BizRequire.isFalse(lambdaQuery()
            .eq(User::getUsername, request.getUsername())
            .exists(), UserResultCode.USERNAME_EXISTS);
        
        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();
        save(user);
    }
}

// 重构后

// 1. 领域层 - UserAggregate.java
public class UserAggregate {
    private final UserId id;
    private final Username username;
    private Password password;
    private Email email;
    private Phone phone;
    
    public static UserAggregate register(Username username, Password password) {
        return new UserAggregate(UserId.generate(), username, password);
    }
    
    public void changePassword(Password oldPassword, Password newPassword, 
                               PasswordEncoder encoder) {
        BizRequire.isTrue(encoder.matches(oldPassword.value(), this.password.value()), 
            "原密码错误");
        this.password = newPassword;
        registerEvent(new PasswordChangedEvent(this.id));
    }
}

// 2. 应用层 - RegisterUserHandler.java
@Component
@RequiredArgsConstructor
public class RegisterUserHandler {
    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public UserId handle(RegisterUserCommand command) {
        BizRequire.isFalse(userRepository.existsByUsername(command.username()),
            UserResultCode.USERNAME_EXISTS);
        
        UserAggregate user = UserAggregate.register(
            command.username(),
            Password.encode(command.password(), passwordEncoder)
        );
        
        userRepository.save(user);
        user.getDomainEvents().forEach(eventPublisher::publish);
        
        return user.getId();
    }
}

// 3. 适配层 - UserController.java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    
    @PostMapping("/register")
    public Result<UserId> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = request.toCommand();
        UserId userId = commandBus.execute(command);
        return Result.success(userId);
    }
}
```

#### Week 2: 应用层与适配层重构

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| U-009 | 创建 `RegisterUserCommand` | P0 | 2h | 用户注册命令 |
| U-010 | 创建 `RegisterUserHandler` | P0 | 4h | 注册命令处理器 |
| U-011 | 创建 `UpdateUserCommand` | P0 | 2h | 用户信息更新命令 |
| U-012 | 创建 `UpdateUserHandler` | P0 | 3h | 更新命令处理器 |
| U-013 | 创建 `ChangePasswordCommand` | P0 | 2h | 修改密码命令 |
| U-014 | 创建 `ChangePasswordHandler` | P0 | 3h | 修改密码处理器 |
| U-015 | 创建 `GetUserQuery` 和 `GetUserHandler` | P0 | 3h | 用户查询 |
| U-016 | 重构 `UserRepositoryImpl` | P0 | 4h | 实现仓储接口 |
| U-017 | 重构 `UserController` | P0 | 4h | 使用 Command/Query |
| U-018 | 编写单元测试 | P1 | 6h | 领域层测试覆盖 80%+ |

---

### Phase 2: Payment 模块重构（Week 3-4）

**目标**：完善 DDD + CQRS，引入策略模式支持多支付方式

#### Week 3: 策略模式与领域层完善

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| P-001 | 创建 `PaymentStrategy` 接口 | P0 | 2h | 支付策略抽象 |
| P-002 | 创建 `MockPaymentStrategy` | P0 | 4h | 模拟支付实现 |
| P-003 | 创建 `AlipayStrategy` | P1 | 6h | 支付宝支付实现 |
| P-004 | 创建 `WechatPayStrategy` | P1 | 6h | 微信支付实现 |
| P-005 | 创建 `PaymentStrategyFactory` | P0 | 2h | 策略工厂 |
| P-006 | 完善 `PaymentAggregate` 状态机 | P0 | 4h | 状态流转控制 |
| P-007 | 创建 `PaymentAmount` 值对象 | P0 | 2h | 金额封装 |
| P-008 | 创建 `PaymentMethod` 值对象 | P0 | 2h | 支付方式封装 |

**新增文件清单**：

```
easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/
├── domain/
│   ├── strategy/
│   │   ├── PaymentStrategy.java        // 策略接口
│   │   ├── PaymentResult.java          // 支付结果
│   │   └── RefundResult.java           // 退款结果
│   └── valueobject/
│       ├── PaymentAmount.java          // 金额值对象
│       └── PaymentMethodVO.java        // 支付方式值对象
├── adapter/outbound/payment/
│   ├── AlipayPaymentAdapter.java       // 支付宝适配器
│   └── WechatPayAdapter.java           // 微信支付适配器
└── application/
    └── factory/
        └── PaymentStrategyFactory.java // 策略工厂
```

**状态机实现**：

```java
// PaymentAggregate.java
public class PaymentAggregate {
    private PaymentId id;
    private PaymentStatus status;
    private PaymentAmount amount;
    private PaymentMethod method;
    
    public void pay(PaymentStrategy strategy) {
        BizRequire.isTrue(status == PaymentStatus.PENDING, 
            "支付状态错误，当前状态: " + status);
        
        PaymentResult result = strategy.pay(amount, method);
        
        if (result.isSuccess()) {
            this.status = PaymentStatus.SUCCESS;
            this.transactionId = result.getTransactionId();
            registerEvent(new PaymentSucceededEvent(this.id, amount));
        } else {
            this.status = PaymentStatus.FAILED;
            this.failReason = result.getErrorMessage();
            registerEvent(new PaymentFailedEvent(this.id, result.getErrorMessage()));
        }
    }
    
    public void refund(PaymentAmount refundAmount, PaymentStrategy strategy) {
        BizRequire.isTrue(status == PaymentStatus.SUCCESS, 
            "未支付成功不能退款");
        BizRequire.isTrue(refundAmount.isLessThanOrEqualTo(amount), 
            "退款金额不能超过支付金额");
        
        RefundResult result = strategy.refund(transactionId, refundAmount);
        
        if (result.isSuccess()) {
            this.refundedAmount = this.refundedAmount.add(refundAmount);
            if (this.refundedAmount.equals(amount)) {
                this.status = PaymentStatus.REFUNDED;
            } else {
                this.status = PaymentStatus.PARTIALLY_REFUNDED;
            }
            registerEvent(new PaymentRefundedEvent(this.id, refundAmount));
        }
    }
}
```

#### Week 4: 应用层重构与集成

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| P-009 | 重构 `CreatePaymentHandler` | P0 | 3h | 使用策略工厂 |
| P-010 | 重构 `PayCommandHandler` | P0 | 4h | 调用策略模式 |
| P-011 | 重构 `RefundPaymentHandler` | P0 | 3h | 退款流程 |
| P-012 | 重构 `PaymentController` | P0 | 3h | 使用 Command/Query |
| P-013 | 添加支付回调处理 | P1 | 4h | 异步支付结果处理 |
| P-014 | 编写单元测试 | P1 | 6h | 策略模式测试 |

---

### Phase 3: Product 模块优化（Week 5-6）

**目标**：完善 Repository 分离，清理 Service 层

#### Week 5: Repository 与领域层优化

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| PR-001 | 重构 `ProductRepository` 接口 | P0 | 3h | 纯领域接口 |
| PR-002 | 创建 `ProductRepositoryImpl` | P0 | 4h | MyBatis 实现 |
| PR-003 | 创建 `ProductQueryRepository` | P0 | 3h | 查询专用仓储 |
| PR-004 | 优化 `ProductAggregate` | P1 | 3h | 完善业务方法 |
| PR-005 | 创建 `CategoryAggregate` | P1 | 4h | 分类聚合根 |
| PR-006 | 重构 `SearchService` | P0 | 4h | 使用 QueryRepository |

**当前问题与重构示例**：

```java
// 问题代码 (SearchServiceImpl.java)
@Service
public class SearchServiceImpl extends ServiceImpl<SearchHistoryMapper, SearchHistory> 
    implements SearchService {
    
    // 问题1: 直接依赖 Mapper，跳过 Repository 层
    private final ProductMapper productMapper;
    
    // 问题2: 在 Service 中处理 VO 转换
    @Override
    public Page<ProductVO> searchProducts(ProductSearchRequest request) {
        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        Page<ProductVO> voPage = new Page<>();
        voPage.setRecords(productPage.getRecords().stream()
            .map(p -> ProductVO.builder()
                .id(p.getId())
                .title(p.getName())
                .build())
            .collect(Collectors.toList()));
        return voPage;
    }
}

// 重构后

// 1. Query Repository 接口
public interface ProductQueryRepository {
    Page<ProductListDTO> searchByCondition(ProductSearchCondition condition);
    Optional<ProductDetailDTO> findDetailById(ProductId id);
}

// 2. 实现层
@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    private final ProductMapper productMapper;
    
    @Override
    public Page<ProductListDTO> searchByCondition(ProductSearchCondition condition) {
        return productMapper.selectProductListDTO(page, condition);
    }
}

// 3. Query Handler
@Component
@RequiredArgsConstructor
public class ProductSearchQueryHandler {
    private final ProductQueryRepository queryRepository;
    
    public Page<ProductListDTO> handle(ProductSearchQuery query) {
        return queryRepository.searchByCondition(query.toCondition());
    }
}
```

#### Week 6: Service 层清理与 CQRS 完善

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| PR-007 | 移除 `ProductAttachmentService` | P0 | 3h | 功能合并到 CommandHandler |
| PR-008 | 重构 `CategoryService` | P1 | 3h | 使用 CQRS 模式 |
| PR-009 | 重构 `ProductReportService` | P1 | 3h | 举报功能 CQRS 化 |
| PR-010 | 重构 `ProductImageService` | P1 | 3h | 图片服务重构 |
| PR-011 | 编写单元测试 | P1 | 6h | 领域层测试覆盖 80%+ |

---

### Phase 4: 集成测试与优化（Week 7-8）

#### Week 7: 跨模块集成

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| I-001 | 实现 Order Saga 编排器 | P0 | 8h | 订单创建分布式事务 |
| I-002 | 完善领域事件订阅 | P0 | 4h | 跨模块事件处理 |
| I-003 | 集成测试：下单流程 | P0 | 6h | 端到端测试通过 |
| I-004 | 集成测试：支付流程 | P0 | 6h | 支付回调测试通过 |

**Saga 编排器实现**：

```java
@Component
@RequiredArgsConstructor
public class CreateOrderSaga {
    private final ProductRepository productRepository;
    private final PaymentServiceClient paymentService;
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    
    @Transactional
    public OrderId execute(CreateOrderCommand command) {
        try {
            // Step 1: 创建订单
            OrderAggregate order = OrderAggregate.create(command);
            orderRepository.save(order);
            
            // Step 2: 扣减库存
            for (OrderItem item : order.getItems()) {
                ProductAggregate product = productRepository
                    .findById(item.getProductId())
                    .orElseThrow();
                product.decreaseStock(item.getQuantity());
                productRepository.save(product);
            }
            
            // Step 3: 创建支付
            PaymentId paymentId = paymentService.create(order.getId(), order.getAmount());
            order.assignPayment(paymentId);
            orderRepository.save(order);
            
            order.getDomainEvents().forEach(eventPublisher::publish);
            
            return order.getId();
            
        } catch (Exception e) {
            compensate(command);
            throw new OrderCreationException("订单创建失败", e);
        }
    }
    
    private void compensate(CreateOrderCommand command) {
        // 补偿逻辑：释放库存等
    }
}
```

#### Week 8: 性能优化与文档

| 任务ID | 任务描述 | 优先级 | 预估工时 | 验收标准 |
|--------|----------|--------|----------|----------|
| O-001 | 添加 Redis 缓存层 | P1 | 6h | 热点数据缓存 |
| O-002 | 查询优化：添加索引 | P1 | 4h | 慢查询优化 |
| O-003 | 异步处理：@Async | P1 | 4h | 非关键操作异步化 |
| O-004 | 更新架构文档 | P1 | 4h | 文档与代码一致 |
| O-005 | Code Review 清单 | P2 | 2h | 团队规范文档 |

---

## 4. 文件重构映射表

### 4.1 User 模块文件映射

| 当前文件 | 操作 | 新文件 |
|----------|------|--------|
| `service/UserService.java` | 删除 | - |
| `service/impl/UserServiceImpl.java` | 删除 | - |
| - | 新增 | `domain/aggregate/UserAggregate.java` |
| - | 新增 | `domain/valueobject/Email.java` |
| - | 新增 | `domain/valueobject/Phone.java` |
| - | 新增 | `domain/valueobject/Password.java` |
| - | 新增 | `application/command/RegisterUserCommand.java` |
| - | 新增 | `application/handler/RegisterUserHandler.java` |
| - | 新增 | `application/query/GetUserQuery.java` |
| - | 新增 | `application/handler/GetUserQueryHandler.java` |
| `controller/UserController.java` | 修改 | `adapter/in/web/UserController.java` |
| `mapper/UserMapper.java` | 保留 | `adapter/out/persistence/UserMapper.java` |
| - | 新增 | `adapter/out/persistence/UserRepositoryImpl.java` |

### 4.2 Payment 模块文件映射

| 当前文件 | 操作 | 新文件 |
|----------|------|--------|
| `service/MockPaymentService.java` | 删除 | - |
| `service/impl/MockPaymentServiceImpl.java` | 删除 | - |
| - | 新增 | `domain/strategy/PaymentStrategy.java` |
| - | 新增 | `domain/strategy/MockPaymentStrategy.java` |
| - | 新增 | `domain/strategy/AlipayStrategy.java` |
| - | 新增 | `domain/strategy/WechatPayStrategy.java` |
| - | 新增 | `application/factory/PaymentStrategyFactory.java` |
| `controller/PaymentCommandController.java` | 修改 | `adapter/in/web/PaymentController.java` |
| `controller/PaymentQueryController.java` | 删除 | 合并到 PaymentController |

### 4.3 Product 模块文件映射

| 当前文件 | 操作 | 新文件 |
|----------|------|--------|
| `service/ProductAttachmentService.java` | 删除 | - |
| `service/impl/ProductAttachmentServiceImpl.java` | 删除 | - |
| `service/SearchService.java` | 删除 | - |
| `service/impl/SearchServiceImpl.java` | 删除 | - |
| - | 新增 | `application/port/out/ProductQueryRepository.java` |
| - | 新增 | `adapter/out/persistence/ProductQueryRepositoryImpl.java` |
| `controller/SearchController.java` | 修改 | 合并到 ProductController |

---

## 5. 验收标准

### 5.1 代码质量标准

| 检查项 | 目标值 | 检查方式 |
|--------|--------|----------|
| 领域层代码覆盖率 | ≥ 80% | JaCoCo 报告 |
| 代码复杂度 (Cyclomatic) | ≤ 10 | SonarQube |
| 方法行数 | ≤ 50 行 | Checkstyle |
| 类行数 | ≤ 400 行 | Checkstyle |
| 循环依赖 | 0 | ArchUnit 测试 |

### 5.2 架构标准

- [ ] 所有领域层类不依赖 Spring 注解
- [ ] Repository 接口定义在 domain 层，实现在 adapter 层
- [ ] Command 和 Query 完全分离
- [ ] 领域事件发布与业务逻辑在同一事务中
- [ ] 所有 Service 改造为 CommandHandler/QueryHandler

### 5.3 功能验证

- [ ] 用户注册/登录/修改信息功能正常
- [ ] 商品创建/更新/查询功能正常
- [ ] 订单创建/支付/退款流程正常
- [ ] 搜索功能正常
- [ ] 所有集成测试通过

---

## 6. 里程碑

| 里程碑 | 日期 | 交付物 |
|--------|------|--------|
| M1 - User 模块完成 | Week 2 结束 | User 模块重构完成，测试通过 |
| M2 - Payment 模块完成 | Week 4 结束 | 策略模式实现，多支付方式支持 |
| M3 - Product 模块完成 | Week 6 结束 | Repository 分离完成 |
| M4 - 集成完成 | Week 8 结束 | Saga 编排实现，全流程测试通过 |

---

## 7. 工具与脚本

### 7.1 重构检查脚本

```bash
#!/bin/bash
# refactor-check.sh

echo "=== 架构重构检查 ==="

# 1. 检查领域层是否依赖 Spring
echo "检查领域层 Spring 依赖..."
grep -r "org.springframework" easyorange-*/src/main/java/**/domain/ || echo "✅ 领域层无 Spring 依赖"

# 2. 检查 Repository 接口位置
echo "检查 Repository 接口位置..."
find easyorange-*/src/main/java -path "*/domain/repository/*.java" | wc -l

# 3. 检查 Service 是否还存在
echo "检查遗留 Service..."
find easyorange-*/src/main/java -path "*/service/*.java" | grep -v "impl" | wc -l

# 4. 代码覆盖率
echo "运行测试..."
mvn test jacoco:report
```

### 7.2 重构模板

```java
// Command Handler 模板
@Component
@RequiredArgsConstructor
public class {Action}{Module}Handler {
    private final {Module}Repository {module}Repository;
    private final DomainEventPublisher eventPublisher;
    
    @Transactional
    public {Result} handle({Action}{Module}Command command) {
        // 1. 加载聚合
        {Module}Aggregate aggregate = {module}Repository
            .findById(command.id())
            .orElseThrow(() -> new {Module}NotFoundException(command.id()));
        
        // 2. 执行业务操作
        {DomainEvent} event = aggregate.{action}(command.params());
        
        // 3. 保存
        {module}Repository.save(aggregate);
        
        // 4. 发布事件
        aggregate.getDomainEvents().forEach(eventPublisher::publish);
        
        return {Result}.from(aggregate);
    }
}
```

---

## 附录

### A. 参考文档

- [洋葱架构详解](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/)
- [DDD 战略设计](https://dddcommunity.org/book/evans_2003/)
- [CQRS 模式](https://docs.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- [Saga 模式](https://microservices.io/patterns/data/saga.html)

### B. 术语表

| 术语 | 说明 |
|------|------|
| DDD | 领域驱动设计 (Domain-Driven Design) |
| CQRS | 命令查询职责分离 (Command Query Responsibility Segregation) |
| Saga | 分布式事务编排模式 |
| Aggregate | 领域聚合根，业务一致性边界 |
| Value Object | 值对象，无身份标识的不可变对象 |
| Domain Event | 领域事件，记录业务发生的事实 |

---

**文档维护者**: EasyOrange 技术团队  
**最后更新**: 2026-04-22
