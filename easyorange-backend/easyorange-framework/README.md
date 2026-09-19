# EasyOrange Framework

框架适配层：把 Spring / Redis / MyBatis-Plus / RabbitMQ / Security 的通用能力封装成各业务模块可直接依赖的基础设施。

```xml
<dependency>
    <groupId>com.cartethyia</groupId>
    <artifactId>easyorange-framework</artifactId>
</dependency>
```

## 能力清单

安全（OAuth2 Resource Server 双 Token）/ 缓存（Spring Cache 注解式 + Redis 单层，`CacheErrorHandler` fail-open）/ 分布式 ID（UUID v7）/ 分布式锁（Redisson RLock）/ 领域事件（Modulith Outbox → RabbitMQ）/ 限流与防重（`RateLimitFilter`）/ Idempotency-Key 幂等 / 审计日志（Outbox 异步入库）/ 全局异常 / 文件服务。

## 文档去向

| 想了解 | 读 |
|---|---|
| 各机制的实现要点、改动注意事项、踩坑 | [easyorange-backend/AGENTS.md](../AGENTS.md)（安全 / 过滤器链、缓存与 Redis、事件与 MQ 三节） |
| `easyorange.*` 配置项、环境变量、完整 yaml 示例 | [CONFIGURATION.md](./CONFIGURATION.md) |
| 全局硬约束（响应体 / 异常 / ID / 分层） | [根 AGENTS.md](../../AGENTS.md) |
| 依赖精确版本 | [架构-技术栈.md](../../doc/架构/架构-技术栈.md)（版本表唯一权威落点，`.githooks/check-version-drift.py` 校验） |
