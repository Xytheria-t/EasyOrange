# easyorange-order Module Agents

Professional agent configuration for the order management module.

## Module Overview

The `easyorange-order` module handles the complete order lifecycle including:
- Order creation with Saga distributed transactions
- Order payment, shipping, receipt confirmation, cancellation, refund
- CQRS (Command Query Responsibility Segregation) architecture
- Order timeout handling and auto-confirmation
- Inventory deduction and restoration via domain events

## Available Agents

### 1. **order-saga-agent**

**Purpose**: Handle distributed transaction orchestration

**When to use**:
- Implementing new Saga workflows
- Modifying compensation logic
- Adding retry mechanisms for failed Sagas
- Distributed lock management

**Capabilities**:
- Saga pattern implementation
- Compensation transaction design
- Redis distributed locking
- Saga state persistence and recovery

**Example**:
```
"Add Saga workflow for bulk order creation"
"Implement Saga retry for failed payments"
"Add inventory reservation in order Saga"
```

### 2. **order-cqrs-agent**

**Purpose**: Handle CQRS command and query separation

**When to use**:
- Adding new order commands (write operations)
- Adding new order queries (read operations)
- Optimizing read models
- Cache invalidation strategies

**Capabilities**:
- Command handler design
- Query handler optimization
- Read model assembly
- Cache-aside pattern with Redis

**Example**:
```
"Add query for seller order statistics"
"Optimize order list query with caching"
"Add command for order batch cancellation"
```

### 3. **order-domain-agent**

**Purpose**: Handle domain model and business rules

**When to use**:
- Modifying order aggregate root
- Adding new order status transitions
- Implementing domain invariants
- Adding domain events

**Capabilities**:
- Aggregate root design
- State machine implementation
- Domain event publishing
- Value object design

**Example**:
```
"Add order dispute status and transitions"
"Implement order splitting logic"
"Add delivery tracking domain events"
```

### 4. **order-cache-agent**

**Purpose**: Handle order caching and performance

**When to use**:
- Implementing order cache strategies
- Cache invalidation logic
- Performance optimization for queries
- Redis integration for order data

**Capabilities**:
- Redis cache patterns
- Cache invalidation on order changes
- Read-through and write-through strategies
- Cache warming strategies

**Example**:
```
"Cache seller order list"
"Implement cache for order details"
"Add cache warming for hot orders"
```

## Agent Usage Patterns

### Standard Development Workflow

```
1. Identify the feature/bug
   ↓
2. Choose appropriate agent
   ↓
3. Agent analyzes existing patterns
   ↓
4. Agent implements following TDD
   ↓
5. Code review with java-code-reviewer
   ↓
6. Test and verify
```

### Agent Selection Matrix

| Task Type | Primary Agent | Secondary Agent |
|-----------|--------------|-----------------|
| New order workflow | order-saga-agent | order-domain-agent |
| Order query optimization | order-cqrs-agent | order-cache-agent |
| Status transition change | order-domain-agent | order-saga-agent |
| Cache implementation | order-cache-agent | order-cqrs-agent |
| Bulk order operations | order-saga-agent | order-cqrs-agent |
| Order statistics | order-cqrs-agent | order-cache-agent |

## Architecture Patterns

### CQRS Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Interface Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  OrderCommandController│   │  OrderQueryController    │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                  Application Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  OrderCommandHandler  │    │  OrderQueryHandler       │  │
│  │  - CreateOrder        │    │  - GetOrderById          │  │
│  │  - PayOrder           │    │  - ListMyOrders          │  │
│  │  - CancelOrder        │    │  - ListSoldOrders        │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  OrderAggregate (Immutable)                          │  │
│  │  - createOrder() → OrderAggregate                    │  │
│  │  - pay() → OrderAggregate                            │  │
│  │  - ship() → OrderAggregate                           │  │
│  │  - cancel() → OrderAggregate                         │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  CreateOrderSaga                                     │  │
│  │  - execute() with compensation                       │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  Domain Events                                       │  │
│  │  - OrderCreatedEvent, OrderPaidEvent, etc.           │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│               Infrastructure Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Persistence         │    │  Cache                   │  │
│  │  - MybatisOrderRepo  │    │  - OrderCacheService     │  │
│  │  - MybatisOrderReadRepo│  │  - OrderReadCache        │  │
│  └──────────────────────┘    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
order/
├── application/
│   ├── command/              # Command handlers
│   │   ├── OrderCommandHandler.java
│   │   ├── CreateOrderCommand.java
│   │   └── ...
│   └── query/                # Query handlers
│       ├── OrderQueryHandler.java
│       └── ...
├── domain/
│   ├── aggregate/
│   │   └── OrderAggregate.java
│   ├── saga/
│   │   └── CreateOrderSaga.java
│   ├── event/
│   │   ├── OrderCreatedEvent.java
│   │   └── ...
│   ├── valueobject/
│   │   ├── OrderId.java
│   │   ├── Money.java
│   │   └── ...
│   └── repository/
│       ├── OrderRepository.java
│       └── OrderReadRepository.java
├── infrastructure/
│   ├── persistence/
│   │   ├── MybatisOrderRepository.java
│   │   └── ...
│   ├── cache/
│   │   ├── OrderCacheService.java
│   │   └── OrderReadCache.java
│   └── scheduler/
│       ├── OrderTimeoutTask.java
│       └── OrderAutoConfirmTask.java
└── interfaces/
    ├── rest/
    │   ├── OrderCommandController.java
    │   └── OrderQueryController.java
    └── assembler/
        └── OrderVOAssembler.java
```

## Code Conventions

### Naming Conventions

- **Controllers**: `*CommandController`, `*QueryController`
- **Handlers**: `*CommandHandler`, `*QueryHandler`
- **Aggregates**: `*Aggregate` (e.g., `OrderAggregate`)
- **Sagas**: `*Saga` (e.g., `CreateOrderSaga`)
- **Events**: `*Event` (e.g., `OrderCreatedEvent`)
- **Value Objects**: `*` (e.g., `OrderId`, `Money`)

### Immutable Aggregates

```java
// OrderAggregate returns new instances on state changes
public OrderAggregate pay(Money amount) {
    if (!canPay()) {
        throw new OrderStatusException("Cannot pay order in status: " + status);
    }
    return new OrderAggregate(this.id, OrderStatus.PAID, ...);
}
```

### Saga Pattern

```java
// Saga with compensation
public class CreateOrderSaga {
    public void execute(CreateOrderCommand command) {
        // 1. Deduct inventory
        // 2. Create payment
        // 3. Save order
        // On failure: compensate in reverse order
    }
}
```

## Testing Requirements

- **Unit Tests**: Domain aggregate behavior, Saga logic
- **Integration Tests**: Repository with Testcontainers
- **Cache Tests**: Cache hit/miss scenarios
- **Coverage Target**: 80%+

## Integration Points

- **easyorange-product**: Inventory deduction via events
- **easyorange-payment**: Payment creation via ports
- **easyorange-user**: User info via ports
- **easyorange-framework**: Redis, Security, Event publishing
