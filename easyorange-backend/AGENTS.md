# EasyOrange Backend - Agent 协作指南

> 本文档定义了 EasyOrange 后端项目中 Agent 的使用规范和最佳实践

## 项目架构概览

**技术栈**：
- Spring Boot 4.0.3 + Java 25
- DDD (Domain-Driven Design) 分层架构
- MyBatis Plus 3.5.16
- MapStruct 1.6.3
- JWT 认证
- Testcontainers 集成测试

**模块结构**：
```
easyorange-backend/
├── easyorange-common-core      # 核心工具类
├── easyorange-common-domain    # 领域事件基础设施
├── easyorange-common           # 通用组件
├── easyorange-framework        # 框架配置
├── easyorange-user             # 用户模块
├── easyorange-product          # 商品模块
├── easyorange-order            # 订单模块
├── easyorange-payment          # 支付模块
├── easyorange-message          # 消息模块
├── easyorange-favorite         # 收藏模块
└── easyorange-application      # 应用启动入口
```

## 核心 Agent 使用场景

### 1. 新功能开发 (Feature Development)

**触发场景**：实现新的业务功能、添加新的 API 端点、创建新的领域模型

**推荐工作流**：

```
用户请求新功能
    │
    ▼
┌─────────────────────────┐
│ springboot-tdd-expert   │  ← TDD 方式开发 Spring Boot 功能
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  java-code-reviewer     │  ← Java/Spring Boot 代码审查
└────────┬────────────────┘
         │
         ▼ (安全相关代码)
┌─────────────────────────┐
│  security-reviewer      │  ← 安全审计
└─────────────────────────┘
```

**使用示例**：
```
用户: "实现用户注册功能，包含邮箱验证和密码加密"

Agent 调用顺序:
1. springboot-tdd-expert - 使用 TDD 方式实现注册功能
2. java-code-reviewer - 审查 Spring Boot 最佳实践
3. security-reviewer - 审查密码加密、输入验证等安全问题
```

### 2. Bug 修复 (Bug Fix)

**触发场景**：修复生产环境问题、测试失败、功能异常

**推荐工作流**：

```
Bug 报告
    │
    ▼
┌─────────────────────────┐
│ springboot-tdd-expert   │  ← 先写失败测试，再修复
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  java-code-reviewer     │  ← 确保修复符合规范
└─────────────────────────┘
```

**关键原则**：
- 先写失败的测试用例重现 Bug
- 修复代码使测试通过
- 确保测试覆盖率 ≥ 80%

### 3. 构建失败 (Build Failure)

**触发场景**：Maven 编译失败、测试失败、依赖问题

**推荐工作流**：

```
构建失败
    │
    ▼
┌─────────────────────────┐
│ java-build-resolver     │  ← 诊断并修复 Java 构建错误
└─────────────────────────┘
```

**常见问题**：
- MapStruct 注解处理器配置问题
- Lombok 编译问题
- MyBatis Plus 兼容性问题
- Spring Boot 4.x 新特性适配

### 4. 数据库变更 (Database Changes)

**触发场景**：添加新表、修改字段、数据迁移

**推荐工作流**：

```
数据库变更需求
    │
    ▼
┌─────────────────────────┐
│ database-migration-expert│  ← 规划安全的数据库迁移
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ springboot-tdd-expert   │  ← 更新实体和 Repository
└─────────────────────────┘
```

**注意事项**：
- 使用 Testcontainers 进行集成测试
- 遵循零停机迁移原则
- 更新相关的领域模型和值对象

### 5. 代码重构 (Refactoring)

**触发场景**：优化代码结构、消除技术债务、提升代码质量

**推荐工作流**：

```
重构需求
    │
    ▼
┌─────────────────────────┐
│  code-simplifier        │  ← 简化复杂代码
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ springboot-tdd-expert   │  ← 确保测试通过
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  refactor-cleaner       │  ← 清理无用代码
└─────────────────────────┘
```

### 6. 安全审计 (Security Audit)

**触发场景**：涉及认证、授权、支付、用户敏感数据的代码变更

**推荐工作流**：

```
安全敏感代码
    │
    ▼
┌─────────────────────────┐
│  security-reviewer      │  ← 全面安全审查
└────────┬────────────────┘
         │
         ▼ (发现安全问题)
┌─────────────────────────┐
│ springboot-tdd-expert   │  ← 修复安全问题
└─────────────────────────┘
```

**重点检查**：
- JWT Token 安全性
- 密码加密和存储
- SQL 注入防护
- XSS 防护
- CSRF 防护
- 敏感数据脱敏

### 7. API 设计 (API Design)

**触发场景**：设计新的 REST API、优化现有 API

**推荐工作流**：

```
API 设计需求
    │
    ▼
┌─────────────────────────┐
│  api-designer           │  ← 设计 RESTful API
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ springboot-tdd-expert   │  ← 实现 API 端点
└─────────────────────────┘
```

**API 设计原则**：
- 遵循 RESTful 规范
- 统一响应格式 (Result<T>)
- 分页查询使用 PageResult<T>
- 完善的错误处理
- OpenAPI 文档

## Agent 快速参考

| Agent | 主要用途 | 触发时机 |
|-------|---------|---------|
| `springboot-tdd-expert` | Spring Boot TDD 开发 | 新功能、Bug 修复 |
| `java-code-reviewer` | Java/Spring Boot 代码审查 | 代码修改后 |
| `java-build-resolver` | Java 构建错误修复 | Maven 编译失败 |
| `security-reviewer` | 安全审计 | 认证、支付、敏感数据 |
| `database-migration-expert` | 数据库迁移 | Schema 变更 |
| `api-designer` | API 设计 | 新增或重构 API |
| `code-simplifier` | 代码简化 | 复杂逻辑重构 |
| `refactor-cleaner` | 代码清理 | 删除无用代码 |

## DDD 架构特定指南

### 领域层 (Domain Layer)

**负责 Agent**: `springboot-tdd-expert`

**关键实践**：
- 聚合根 (Aggregate Root) 必须保护不变性
- 值对象 (Value Object) 不可变
- 领域事件 (Domain Event) 用于解耦
- 领域服务 (Domain Service) 处理跨聚合逻辑

**示例**：
```java
// 聚合根
public class Order extends BaseEntity {
    private OrderId id;
    private OrderStatus status;
    private List<OrderItem> items;
    
    public void addItem(Product product, int quantity) {
        // 业务规则验证
        if (status != OrderStatus.DRAFT) {
            throw new OrderCannotModifyException();
        }
        items.add(new OrderItem(product, quantity));
        // 发布领域事件
        registerEvent(new OrderItemAddedEvent(this.id, product.getId()));
    }
}
```

### 应用层 (Application Layer)

**负责 Agent**: `springboot-tdd-expert`

**关键实践**：
- 应用服务 (Application Service) 编排业务流程
- 命令 (Command) 和查询 (Query) 分离
- 事务边界在应用服务层
- DTO 转换使用 MapStruct

**示例**：
```java
@Service
@Transactional
public class OrderCommandService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    public OrderId createOrder(CreateOrderCommand command) {
        // 编排业务流程
        Order order = Order.create(command.getUserId());
        for (OrderItemDTO item : command.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            order.addItem(product, item.getQuantity());
        }
        return orderRepository.save(order).getId();
    }
}
```

### 适配层 (Adapter Layer)

**负责 Agent**: `springboot-tdd-expert` + `api-designer`

**关键实践**：
- 入站适配器 (Inbound Adapter): REST Controller
- 出站适配器 (Outbound Adapter): Repository 实现、外部服务调用
- 数据转换: DO (Data Object) ↔ Domain Model

**示例**：
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderCommandService commandService;
    private final OrderQueryService queryService;
    
    @PostMapping
    public Result<OrderId> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = request.toCommand();
        OrderId orderId = commandService.createOrder(command);
        return Result.success(orderId);
    }
}
```

## 并行 Agent 执行策略

### 多维度分析

对于复杂问题，并行调用多个 Agent 进行多角度分析：

```
复杂业务场景
    │
    ├─→ Agent 1 (springboot-tdd-expert):  实现方案验证
    ├─→ Agent 2 (security-reviewer):      安全风险评估
    ├─→ Agent 3 (java-code-reviewer):     代码质量审查
    └─→ Agent 4 (api-designer):           API 设计评审
```

### 示例场景

**用户请求**: "实现订单支付功能，支持微信支付和支付宝"

**并行 Agent 调用**:
1. `api-designer` - 设计支付 API 接口
2. `security-reviewer` - 评估支付安全风险
3. `springboot-tdd-expert` - 设计支付领域模型

## 测试策略

### 单元测试

**负责 Agent**: `springboot-tdd-expert`

**覆盖范围**：
- 领域模型业务逻辑
- 值对象验证
- 领域服务
- 工具类

**工具**：JUnit 5 + AssertJ + Mockito

### 集成测试

**负责 Agent**: `springboot-tdd-expert`

**覆盖范围**：
- Repository 实现
- 数据库操作
- 缓存操作
- 消息队列

**工具**：Testcontainers (MySQL + Redis)

### 架构测试

**负责 Agent**: `java-code-reviewer`

**覆盖范围**：
- DDD 分层规则
- 包依赖关系
- 命名规范

**工具**：ArchUnit

## 质量门禁

### 代码提交前检查清单

- [ ] 所有测试通过 (`./mvnw test`)
- [ ] 测试覆盖率 ≥ 80% (`./mvnw jacoco:report`)
- [ ] 无安全漏洞 (`./mvnw dependency-check:check`)
- [ ] 代码格式规范 (`./mvnw spotless:check`)
- [ ] 无重复代码
- [ ] 无循环依赖

### Agent 审查顺序

```
代码修改完成
    │
    ▼
┌─────────────────────────┐
│  java-code-reviewer     │  ← 代码质量审查
└────────┬────────────────┘
         │
         ▼ (安全敏感代码)
┌─────────────────────────┐
│  security-reviewer      │  ← 安全审计
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  build-error-resolver   │  ← 构建验证
└─────────────────────────┘
```

## 常见问题处理

### 1. MapStruct 映射错误

**Agent**: `java-build-resolver`

**常见原因**：
- Lombok 和 MapStruct 注解处理器顺序问题
- 缺少默认构造函数
- 类型不匹配

### 2. MyBatis Plus 查询性能问题

**Agent**: `java-code-reviewer`

**优化策略**：
- 使用索引优化
- 避免 N+1 查询
- 合理使用缓存

### 3. 领域事件未触发

**Agent**: `springboot-tdd-expert`

**检查点**：
- SpringDomainEventPublisher 配置
- 事件监听器注册
- 事务边界

### 4. JWT Token 验证失败

**Agent**: `security-reviewer`

**检查点**：
- Token 签名密钥配置
- Token 过期时间
- 权限声明

## 最佳实践

### 1. 始终使用 TDD

```
RED → GREEN → REFACTOR
  ↓      ↓         ↓
写测试  实现功能   优化代码
```

### 2. 遵循 DDD 分层

```
Controller (适配层)
    ↓
Application Service (应用层)
    ↓
Domain Model (领域层)
    ↓
Repository (基础设施层)
```

### 3. 优先不可变性

```java
// GOOD: 不可变值对象
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
}
```

### 4. 显式错误处理

```java
// GOOD: 显式异常处理
public Order findOrderById(OrderId id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new OrderNotFoundException(id));
}
```

## 相关文档

- [Spring Boot TDD 实践](./.trae/rules/java/testing.md)
- [Java 编码规范](./.trae/rules/java/coding-style.md)
- [安全指南](./.trae/rules/java/security.md)
- [数据库迁移](./.trae/rules/common/database-migrations.md)
