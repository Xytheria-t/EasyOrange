# easyorange-payment Module Agents

Professional agent configuration for the payment processing module.

## Module Overview

The `easyorange-payment` module handles all payment-related functionality including:
- Payment order creation and lifecycle management
- Payment gateway integration (with mock adapter for dev)
- Refund processing (full and partial)
- Idempotency protection
- Distributed locking for concurrency control
- Outbox pattern for reliable event publishing
- Saga orchestration for multi-step payment flows
- Payment metrics and monitoring

## Available Agents

### 1. **payment-gateway-agent**

**Purpose**: Handle payment gateway integration and adapter design

**When to use**:
- Adding new payment methods (WeChat Pay, Alipay, etc.)
- Modifying gateway adapter implementations
- Adding payment callback/webhook handlers
- Implementing gateway-specific security

**Capabilities**:
- Payment gateway adapter pattern
- Callback signature verification
- Request/response transformation
- Mock gateway for development

**Example**:
```
"Add WeChat Pay gateway adapter"
"Implement payment callback verification"
"Add sandbox mode for testing"
```

### 2. **payment-saga-agent**

**Purpose**: Handle payment Saga orchestration

**When to use**:
- Implementing new payment workflows
- Modifying compensation logic
- Adding retry mechanisms
- Handling partial refund Sagas

**Capabilities**:
- Saga orchestration pattern
- Two-phase commit design
- Compensation transaction implementation
- Saga state tracking

**Example**:
```
"Add Saga for installment payments"
"Implement compensation for failed refunds"
"Add retry logic for gateway timeouts"
```

### 3. **payment-idempotency-agent**

**Purpose**: Handle idempotency and concurrency control

**When to use**:
- Implementing idempotent endpoints
- Adding distributed locking
- Handling duplicate requests
- Concurrency conflict resolution

**Capabilities**:
- SHA-256 request hashing
- Idempotency key management
- Redis distributed locking
- Conflict detection and handling

**Example**:
```
"Add idempotency for refund endpoint"
"Implement distributed lock for payment creation"
"Handle duplicate callback requests"
```

### 4. **payment-outbox-agent**

**Purpose**: Handle reliable event publishing with Outbox pattern

**When to use**:
- Adding new domain events
- Implementing event publishers
- Adding event compensation
- Optimizing Outbox polling

**Capabilities**:
- Outbox pattern implementation
- Event persistence design
- Async event publishing
- Event delivery guarantees

**Example**:
```
"Add Outbox support for payment failed events"
"Implement event publisher for refund notifications"
"Add compensation for undelivered events"
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
5. Code review with java-code-reviewer + security-reviewer
   ↓
6. Test and verify
```

### Agent Selection Matrix

| Task Type | Primary Agent | Secondary Agent |
|-----------|--------------|-----------------|
| New payment method | payment-gateway-agent | payment-saga-agent |
| Payment workflow | payment-saga-agent | payment-idempotency-agent |
| Refund processing | payment-saga-agent | payment-idempotency-agent |
| Duplicate prevention | payment-idempotency-agent | payment-outbox-agent |
| Event reliability | payment-outbox-agent | payment-saga-agent |
| Gateway integration | payment-gateway-agent | payment-idempotency-agent |

## Architecture Patterns

### Hexagonal Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Adapter Layer (Inbound)                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  PaymentCommandController                            │  │
│  │  PaymentQueryController                              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                  Application Layer                           │
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │  Command Handlers    │    │  Query Handlers          │  │
│  │  - CreatePayment     │    │  - GetPaymentById        │  │
│  │  - ProcessPayment    │    │  - ListMyPayments        │  │
│  │  - RefundPayment     │    │                          │  │
│  ├──────────────────────┤    └──────────────────────────┘  │
│  │  IdempotencyService                                  │  │
│  │  DistributedLockWrapper                              │  │
│  │  PaymentMetricsService                               │  │
│  └──────────────────────┘                                  │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  PaymentAggregate                                    │  │