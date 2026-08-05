-- ---------------------------------------------------------------------------
-- 移除支付模块遗留的幂等键表。
-- 该表由已被删除的 payment application/idempotency 死代码创建，无任何代码写入；
-- 幂等保护统一由 framework 的 IdempotencyKeyFilter + Redis 实现承载。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `eo_idempotency_key`;