# ADR-0014: Payment 模块简化版 DDD+CQRS 架构

**Date**: 2026-04-18
**Status**: accepted
**Deciders**: EasyOrange Team

## Context

Payment 模块存在半 CQRS + 半 DDD 的混合架构：
- `PaymentAggregate` 聚合根包含领域逻辑（create, pay, fail, refund, close）
- `PaymentCommandHandler` / `PaymentQueryHandler` 处理命令/查询
- 但 `PaymentServiceImpl` 独立实现，直接操作 DB，未使用聚合根

这导致：
1. **双轨并行**：两套命令处理路径，职责不清
2. **N+1 查询**：`PaymentQueryHandler.toPaymentVO()` 存在冗余查询（line 45）
3. **数据不一致**：`PaymentAggregate.create()` 缺少 amount 校验（Entity 有但 Aggregate 无）
4. **接口污染**：`PaymentService` 继承 `IService<Payment>` 引入 MyBatis-Plus 耦合

## Decision

采用**简化版 DDD+CQRS**：

1. **统一命令路径**：废弃 `PaymentServiceImpl`，所有写操作经由 `PaymentCommandHandler`
2. **修复 N+1**：在聚合根中直接包含 `createTime`/`updateTime`，避免二次查询
3. **保持简单**：不引入 Saga、Event Sourcing 等复杂模式
4. **保留仓储模式**：继续使用 `PaymentRepository` 接口 + `MybatisPaymentRepository` 实现

### 架构分层

```
Application Layer
├── PaymentCommandController  → 命令入口（新增）
├── PaymentQueryController    → 查询入口（新增）
├── PaymentCommandHandler      → 执行命令，调用聚合根，发布事件
└── PaymentQueryHandler        → 执行查询，Entity → VO

Domain Layer
├── PaymentAggregate    → 聚合根，包含所有领域逻辑
├── PaymentRepository  → 仓储接口
└── Domain Events      → 领域事件（PaymentCreatedEvent, PaymentSucceededEvent 等）

Infrastructure Layer
└── MybatisPaymentRepository → 持久化适配器（基于 MyBatis-Plus）
```

### 类职责

| 类 | 职责 | 变化 |
|----|------|------|
| `PaymentCommandController` | 接收 HTTP 命令请求 | 新增 |
| `PaymentQueryController` | 接收 HTTP 查询请求 | 新增 |
| `PaymentCommandHandler` | 唯一命令处理入口，调用聚合根方法，发布领域事件 | 改造 |
| `PaymentQueryHandler` | 唯一查询处理入口，直接从 Repository 读取 | 改造 |
| `PaymentAggregate` | 领域逻辑、状态校验、状态流转 | 改造 |
| `PaymentRepository` | 数据访问抽象接口 | 保持 |
| `MybatisPaymentRepository` | MyBatis-Plus 持久化实现 | 保持 |
| `Payment` | 数据实体 | 保持 |
| `PaymentVO` | 视图对象 | 改造 |

## Alternatives Considered

### Alternative 1: 保留 ServiceImpl + CommandHandler 双轨
- **Pros**: 无需修改现有 Controller
- **Cons**: 职责不清，维护困难，两套命令路径容易产生不一致
- **Why not**: 违背单一职责原则，长期维护成本高

### Alternative 2: 引入完整 Event Sourcing
- **Pros**: 完整的事件溯源能力
- **Cons**: 过度设计，学习成本高，事件存储额外复杂度
- **Why not**: 本模块不需要事件溯源，当前业务场景不涉及审计追溯

### Alternative 3: 直接废弃聚合根，使用 ServiceImpl
- **Pros**: 简单直接
- **Cons**: 退化为贫血模型，丧失 DDD 优点
- **Why not**: 聚合根已有完整的领域逻辑，废弃是倒退

## Consequences

### Positive
- 单一命令路径，职责清晰
- N+1 查询问题解决
- 聚合根完整包含领域逻辑
- `PaymentService` 废弃，消除 MyBatis-Plus 泄漏到应用层

### Negative
- 需要新增 `PaymentCommandController` 和 `PaymentQueryController`
- 需要迁移现有 Controller 对 `PaymentService` 的引用
- 分页查询需要重新实现

## Risks
- 迁移过程中需要并行维护新旧两套代码一段时间
- 现有 Controller 引用需要逐一迁移

## Migration Plan

| Phase | 内容 | 验证方式 |
|-------|------|----------|
| Phase 0 | 创建 Controller 层骨架 | 编译通过 |
| Phase 1 | 统一 CommandHandler，消除双轨 | 支付流程端到端测试 |
| Phase 2 | 修复 QueryHandler N+1 | 查询接口性能测试 |
| Phase 3 | 清理废弃代码 | 全量编译+测试 |
