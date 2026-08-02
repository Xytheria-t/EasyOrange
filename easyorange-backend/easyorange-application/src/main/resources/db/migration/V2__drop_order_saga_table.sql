-- 订单创建不再使用 Saga（见 doc/adr/0007-order-saga-single-tx-observability.md）：
-- 单库单事务 + 分布式锁 + Outbox 已覆盖原子性与并发控制，删除 Saga 状态表。
DROP TABLE IF EXISTS `eo_saga_status`;
