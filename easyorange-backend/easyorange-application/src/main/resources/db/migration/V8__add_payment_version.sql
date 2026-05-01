-- 添加乐观锁版本号字段到支付表
ALTER TABLE eo_payment ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 为现有数据初始化版本号
UPDATE eo_payment SET version = 0 WHERE version IS NULL;
