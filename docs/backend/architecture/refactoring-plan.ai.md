# AI-Readable Refactoring Plan: EasyOrange Backend Architecture

## Project Context
- **Framework**: Spring Boot 4.0.3 (Java 25)
- **Build Tool**: Maven multi-module
- **Data Access**: MyBatis-Plus 3.5.16
- **Security**: Spring Security + JWT
- **Cache**: Redis
- **Database**: MySQL

## Target Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Onion Architecture + DDD + CQRS              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Infrastructure Layer (Infrastructure)                  │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │   │
│  │  │  Controller │ │ Repository  │ │  EventPublisher │    │   │
│  │  │  (Web)      │ │   (DB)      │ │   (MQ)          │    │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Application Layer (Application)                        │   │
│  │  ┌─────────────────┐    ┌─────────────────────────────┐  │   │
│  │  │   Command Bus   │    │      Query Handler          │  │   │
│  │  │  ┌───────────┐  │    │  ┌─────────────────────┐    │  │   │
│  │  │  │{Action}   │  │    │  │   {Module}Query     │    │  │   │
│  │  │  │  Handler  │  │    │  │      Handler        │    │  │   │
│  │  │  └───────────┘  │    │  └─────────────────────┘    │  │   │
│  │  └─────────────────┘    └─────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Domain Layer (Domain)                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │   │
│  │  │  Aggregate  │  │ ValueObject │  │  Domain Service │   │   │
│  │  │  ({Module}) │  │ (Money,etc) │  │  ({Service})    │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────────────┐   │   │
│  │  │              Domain Events                          │   │   │
│  │  │   {Module}Created / {Module}Updated / etc          │   │   │
│  │  └─────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Dependency Rule: Outer → Inner (inward only)                   │
│  Domain Layer has NO framework dependencies                     │
└─────────────────────────────────────────────────────────────────┘
```

## Current Module Status

| Module    | Current Architecture    | Progress | Main Issues                                      |
|-----------|-------------------------|----------|--------------------------------------------------|
| product   | DDD + CQRS partial      | 70%      | Service layer mixed, Repository not fully separated |
| order     | DDD + CQRS implemented  | 90%      | Can be template for other modules                |
| payment   | DDD + CQRS partial      | 60%      | Missing strategy pattern, heavy Service layer    |
| user      | Traditional layered     | 30%      | No DDD, Service directly uses Mapper             |
| message   | Basic implementation    | 50%      | Missing CQRS separation                          |

## Refactoring Roadmap (8 Weeks)

```
Week 1-2        Week 3-4        Week 5-6        Week 7-8
   │               │               │               │
   ▼               ▼               ▼               ▼
┌──────┐      ┌──────┐      ┌──────┐      ┌──────┐
│ User │      │Payment│     │Product│     │Integration│
│Module│      │Module │     │Module │     │& Optimization│
└──────┘      └──────┘      └──────┘      └──────┘

Priority: P0   Priority: P0   Priority: P1   Priority: P1
Complexity: M  Complexity: H  Complexity: M  Complexity: L
```

## Phase 1: User Module (Week 1-2)

### Week 1: Domain Layer

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| U-001  | Create value object `Email`          | P0       | 2h         | Encapsulate email validation logic     |
| U-002  | Create value object `Phone`          | P0       | 2h         | Encapsulate phone validation logic     |
| U-003  | Create value object `Password`       | P0       | 2h         | Encapsulate password strength logic    |
| U-004  | Create value object `Nickname`       | P0       | 1h         | Encapsulate nickname length validation |
| U-005  | Create value object `UserId`         | P0       | 1h         | Encapsulate user ID type               |
| U-006  | Create `UserAggregate` aggregate root| P0       | 4h         | Contains user core business logic      |
| U-007  | Create domain event `UserRegisteredEvent` | P1  | 2h         | User registration domain event         |
| U-008  | Create domain event `PasswordChangedEvent`| P1  | 2h         | Password change domain event           |

**New Files:**
```
easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/
├── aggregate/
│   └── UserAggregate.java              // User aggregate root
├── valueobject/
│   ├── UserId.java                     // User ID value object
│   ├── Email.java                      // Email value object
│   ├── Phone.java                      // Phone value object
│   ├── Password.java                   // Password value object
│   └── Nickname.java                   // Nickname value object
├── event/
│   ├── UserRegisteredEvent.java        // Registration event
│   └── PasswordChangedEvent.java       // Password change event
└── repository/
    └── UserRepository.java             // Repository interface
```

**Before/After Code Pattern:**

```java
// BEFORE (UserServiceImpl.java)
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

// AFTER - Domain Layer (UserAggregate.java)
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
            "Invalid old password");
        this.password = newPassword;
        registerEvent(new PasswordChangedEvent(this.id));
    }
}

// AFTER - Application Layer (RegisterUserHandler.java)
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

// AFTER - Adapter Layer (UserController.java)
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

### Week 2: Application & Adapter Layer

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| U-009  | Create `RegisterUserCommand`         | P0       | 2h         | User registration command              |
| U-010  | Create `RegisterUserHandler`         | P0       | 4h         | Registration command handler           |
| U-011  | Create `UpdateUserCommand`           | P0       | 2h         | User info update command               |
| U-012  | Create `UpdateUserHandler`           | P0       | 3h         | Update command handler                 |
| U-013  | Create `ChangePasswordCommand`       | P0       | 2h         | Password change command                |
| U-014  | Create `ChangePasswordHandler`       | P0       | 3h         | Password change handler                |
| U-015  | Create `GetUserQuery` and `GetUserHandler` | P0 | 3h    | User query                             |
| U-016  | Refactor `UserRepositoryImpl`        | P0       | 4h         | Implement repository interface         |
| U-017  | Refactor `UserController`            | P0       | 4h         | Use Command/Query pattern              |
| U-018  | Write unit tests                     | P1       | 6h         | Domain layer coverage 80%+             |

## Phase 2: Payment Module (Week 3-4)

### Week 3: Strategy Pattern & Domain Layer

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| P-001  | Create `PaymentStrategy` interface   | P0       | 2h         | Payment strategy abstraction           |
| P-002  | Create `MockPaymentStrategy`         | P0       | 4h         | Mock payment implementation            |
| P-003  | Create `AlipayStrategy`              | P1       | 6h         | Alipay implementation                  |
| P-004  | Create `WechatPayStrategy`           | P1       | 6h         | WeChat Pay implementation              |
| P-005  | Create `PaymentStrategyFactory`      | P0       | 2h         | Strategy factory                       |
| P-006  | Complete `PaymentAggregate` state machine | P0  | 4h         | State transition control               |
| P-007  | Create `PaymentAmount` value object  | P0       | 2h         | Amount encapsulation                   |
| P-008  | Create `PaymentMethod` value object  | P0       | 2h         | Payment method encapsulation           |

**New Files:**
```
easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/
├── domain/
│   ├── strategy/
│   │   ├── PaymentStrategy.java        // Strategy interface
│   │   ├── PaymentResult.java          // Payment result
│   │   └── RefundResult.java           // Refund result
│   └── valueobject/
│       ├── PaymentAmount.java          // Amount value object
│       └── PaymentMethodVO.java        // Payment method value object
├── adapter/outbound/payment/
│   ├── AlipayPaymentAdapter.java       // Alipay adapter
│   └── WechatPayAdapter.java           // WeChat Pay adapter
└── application/
    └── factory/
        └── PaymentStrategyFactory.java // Strategy factory
```

**State Machine Implementation:**

```java
// PaymentAggregate.java
public class PaymentAggregate {
    private PaymentId id;
    private PaymentStatus status;
    private PaymentAmount amount;
    private PaymentMethod method;
    
    public void pay(PaymentStrategy strategy) {
        BizRequire.isTrue(status == PaymentStatus.PENDING, 
            "Invalid payment status: " + status);
        
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
            "Cannot refund unpaid order");
        BizRequire.isTrue(refundAmount.isLessThanOrEqualTo(amount), 
            "Refund amount cannot exceed payment amount");
        
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

### Week 4: Application Layer Refactor & Integration

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| P-009  | Refactor `CreatePaymentHandler`      | P0       | 3h         | Use strategy factory                   |
| P-010  | Refactor `PayCommandHandler`         | P0       | 4h         | Call strategy pattern                  |
| P-011  | Refactor `RefundPaymentHandler`      | P0       | 3h         | Refund flow                            |
| P-012  | Refactor `PaymentController`         | P0       | 3h         | Use Command/Query                      |
| P-013  | Add payment callback handling        | P1       | 4h         | Async payment result handling          |
| P-014  | Write unit tests                     | P1       | 6h         | Strategy pattern tests                 |

## Phase 3: Product Module (Week 5-6)

### Week 5: Repository & Domain Layer Optimization

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| PR-001 | Refactor `ProductRepository` interface | P0     | 3h         | Pure domain interface                  |
| PR-002 | Create `ProductRepositoryImpl`       | P0       | 4h         | MyBatis implementation                 |
| PR-003 | Create `ProductQueryRepository`      | P0       | 3h         | Query-specific repository              |
| PR-004 | Optimize `ProductAggregate`          | P1       | 3h         | Complete business methods              |
| PR-005 | Create `CategoryAggregate`           | P1       | 4h         | Category aggregate root                |
| PR-006 | Refactor `SearchService`             | P0       | 4h         | Use QueryRepository                    |

**Problem & Solution Pattern:**

```java
// PROBLEM (SearchServiceImpl.java)
@Service
public class SearchServiceImpl extends ServiceImpl<SearchHistoryMapper, SearchHistory> 
    implements SearchService {
    
    // Issue 1: Direct Mapper dependency, bypassing Repository layer
    private final ProductMapper productMapper;
    
    // Issue 2: VO conversion in Service
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

// SOLUTION - Query Repository Interface
public interface ProductQueryRepository {
    Page<ProductListDTO> searchByCondition(ProductSearchCondition condition);
    Optional<ProductDetailDTO> findDetailById(ProductId id);
}

// SOLUTION - Implementation Layer
@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    private final ProductMapper productMapper;
    
    @Override
    public Page<ProductListDTO> searchByCondition(ProductSearchCondition condition) {
        return productMapper.selectProductListDTO(page, condition);
    }
}

// SOLUTION - Query Handler
@Component
@RequiredArgsConstructor
public class ProductSearchQueryHandler {
    private final ProductQueryRepository queryRepository;
    
    public Page<ProductListDTO> handle(ProductSearchQuery query) {
        return queryRepository.searchByCondition(query.toCondition());
    }
}
```

### Week 6: Service Layer Cleanup & CQRS Completion

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| PR-007 | Remove `ProductAttachmentService`    | P0       | 3h         | Merge into CommandHandler              |
| PR-008 | Refactor `CategoryService`           | P1       | 3h         | Use CQRS pattern                       |
| PR-009 | Refactor `ProductReportService`      | P1       | 3h         | Report feature CQRS                    |
| PR-010 | Refactor `ProductImageService`       | P1       | 3h         | Image service refactor                 |
| PR-011 | Write unit tests                     | P1       | 6h         | Domain layer coverage 80%+             |

## Phase 4: Integration Testing & Optimization (Week 7-8)

### Week 7: Cross-Module Integration

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| I-001  | Implement Order Saga orchestrator    | P0       | 8h         | Order creation distributed transaction |
| I-002  | Complete domain event subscription   | P0       | 4h         | Cross-module event handling            |
| I-003  | Integration test: Order flow         | P0       | 6h         | End-to-end test pass                   |
| I-004  | Integration test: Payment flow       | P0       | 6h         | Payment callback test pass             |

**Saga Orchestrator Implementation:**

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
            // Step 1: Create order
            OrderAggregate order = OrderAggregate.create(command);
            orderRepository.save(order);
            
            // Step 2: Deduct stock
            for (OrderItem item : order.getItems()) {
                ProductAggregate product = productRepository
                    .findById(item.getProductId())
                    .orElseThrow();
                product.decreaseStock(item.getQuantity());
                productRepository.save(product);
            }
            
            // Step 3: Create payment
            PaymentId paymentId = paymentService.create(order.getId(), order.getAmount());
            order.assignPayment(paymentId);
            orderRepository.save(order);
            
            order.getDomainEvents().forEach(eventPublisher::publish);
            
            return order.getId();
            
        } catch (Exception e) {
            compensate(command);
            throw new OrderCreationException("Order creation failed", e);
        }
    }
    
    private void compensate(CreateOrderCommand command) {
        // Compensation logic: release stock, etc.
    }
}
```

### Week 8: Performance Optimization & Documentation

| TaskID | Description                          | Priority | Est. Hours | Acceptance Criteria                    |
|--------|--------------------------------------|----------|------------|----------------------------------------|
| O-001  | Add Redis cache layer                | P1       | 6h         | Hot data caching                       |
| O-002  | Query optimization: add indexes      | P1       | 4h         | Slow query optimization                |
| O-003  | Async processing: @Async             | P1       | 4h         | Non-critical operations async          |
| O-004  | Update architecture documentation    | P1       | 4h         | Documentation matches code             |
| O-005  | Code review checklist                | P2       | 2h         | Team standards document                |

## File Refactoring Mapping

### User Module

| Current File                                      | Action   | New File                                                   |
|---------------------------------------------------|----------|------------------------------------------------------------|
| `service/UserService.java`                        | DELETE   | -                                                          |
| `service/impl/UserServiceImpl.java`               | DELETE   | -                                                          |
| -                                                 | CREATE   | `domain/aggregate/UserAggregate.java`                      |
| -                                                 | CREATE   | `domain/valueobject/Email.java`                            |
| -                                                 | CREATE   | `domain/valueobject/Phone.java`                            |
| -                                                 | CREATE   | `domain/valueobject/Password.java`                         |
| -                                                 | CREATE   | `application/command/RegisterUserCommand.java`             |
| -                                                 | CREATE   | `application/handler/RegisterUserHandler.java`             |
| -                                                 | CREATE   | `application/query/GetUserQuery.java`                      |
| -                                                 | CREATE   | `application/handler/GetUserQueryHandler.java`             |
| `controller/UserController.java`                  | MODIFY   | `adapter/in/web/UserController.java`                       |
| `mapper/UserMapper.java`                          | KEEP     | `adapter/out/persistence/UserMapper.java`                  |
| -                                                 | CREATE   | `adapter/out/persistence/UserRepositoryImpl.java`          |

### Payment Module

| Current File                                      | Action   | New File                                                   |
|---------------------------------------------------|----------|------------------------------------------------------------|
| `service/MockPaymentService.java`                 | DELETE   | -                                                          |
| `service/impl/MockPaymentServiceImpl.java`        | DELETE   | -                                                          |
| -                                                 | CREATE   | `domain/strategy/PaymentStrategy.java`                     |
| -                                                 | CREATE   | `domain/strategy/MockPaymentStrategy.java`                 |
| -                                                 | CREATE   | `domain/strategy/AlipayStrategy.java`                      |
| -                                                 | CREATE   | `domain/strategy/WechatPayStrategy.java`                   |
| -                                                 | CREATE   | `application/factory/PaymentStrategyFactory.java`          |
| `controller/PaymentCommandController.java`        | MODIFY   | `adapter/in/web/PaymentController.java`                    |
| `controller/PaymentQueryController.java`          | DELETE   | Merge into PaymentController                               |

### Product Module

| Current File                                      | Action   | New File                                                   |
|---------------------------------------------------|----------|------------------------------------------------------------|
| `service/ProductAttachmentService.java`           | DELETE   | -                                                          |
| `service/impl/ProductAttachmentServiceImpl.java`  | DELETE   | -                                                          |
| `service/SearchService.java`                      | DELETE   | -                                                          |
| `service/impl/SearchServiceImpl.java`             | DELETE   | -                                                          |
| -                                                 | CREATE   | `application/port/out/ProductQueryRepository.java`         |
| -                                                 | CREATE   | `adapter/out/persistence/ProductQueryRepositoryImpl.java`  |
| `controller/SearchController.java`                | MODIFY   | Merge into ProductController                               |

## Acceptance Criteria

### Code Quality Standards

| Check Item                     | Target | Check Method    |
|--------------------------------|--------|-----------------|
| Domain layer coverage          | ≥ 80%  | JaCoCo report   |
| Code complexity (Cyclomatic)   | ≤ 10   | SonarQube       |
| Method line count              | ≤ 50   | Checkstyle      |
| Class line count               | ≤ 400  | Checkstyle      |
| Circular dependencies          | 0      | ArchUnit test   |

### Architecture Standards

- [ ] All domain layer classes have NO Spring annotations
- [ ] Repository interfaces defined in domain layer, implementations in adapter layer
- [ ] Command and Query completely separated
- [ ] Domain events published in same transaction as business logic
- [ ] All Services refactored to CommandHandler/QueryHandler

### Functional Verification

- [ ] User register/login/update info working
- [ ] Product create/update/query working
- [ ] Order create/pay/refund flow working
- [ ] Search function working
- [ ] All integration tests passing

## Milestones

| Milestone              | Date          | Deliverable                                    |
|------------------------|---------------|------------------------------------------------|
| M1 - User Module Done  | End of Week 2 | User module refactor complete, tests passing   |
| M2 - Payment Module Done| End of Week 4| Strategy pattern, multi-payment support        |
| M3 - Product Module Done| End of Week 6| Repository separation complete                 |
| M4 - Integration Done  | End of Week 8 | Saga orchestration, full flow tests passing    |

## Command Handler Template

```java
@Component
@RequiredArgsConstructor
public class {Action}{Module}Handler {
    private final {Module}Repository {module}Repository;
    private final DomainEventPublisher eventPublisher;
    
    @Transactional
    public {Result} handle({Action}{Module}Command command) {
        // 1. Load aggregate
        {Module}Aggregate aggregate = {module}Repository
            .findById(command.id())
            .orElseThrow(() -> new {Module}NotFoundException(command.id()));
        
        // 2. Execute business operation
        {DomainEvent} event = aggregate.{action}(command.params());
        
        // 3. Save
        {module}Repository.save(aggregate);
        
        // 4. Publish events
        aggregate.getDomainEvents().forEach(eventPublisher::publish);
        
        return {Result}.from(aggregate);
    }
}
```

## Verification Script

```bash
#!/bin/bash
# refactor-check.sh

echo "=== Architecture Refactoring Check ==="

# 1. Check if domain layer depends on Spring
echo "Checking domain layer Spring dependencies..."
grep -r "org.springframework" easyorange-*/src/main/java/**/domain/ || echo "✅ Domain layer has no Spring dependencies"

# 2. Check Repository interface locations
echo "Checking Repository interface locations..."
find easyorange-*/src/main/java -path "*/domain/repository/*.java" | wc -l

# 3. Check if Service still exists
echo "Checking legacy Services..."
find easyorange-*/src/main/java -path "*/service/*.java" | grep -v "impl" | wc -l

# 4. Code coverage
echo "Running tests..."
mvn test jacoco:report
```

## Terminology

| Term         | Definition                                      |
|--------------|-------------------------------------------------|
| DDD          | Domain-Driven Design                            |
| CQRS         | Command Query Responsibility Segregation        |
| Saga         | Distributed transaction orchestration pattern   |
| Aggregate    | Domain aggregate root, business consistency boundary |
| Value Object | Value object, immutable object without identity |
| Domain Event | Domain event, records business facts            |
