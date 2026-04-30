-- ===================================================================
-- EasyOrange - V6 支付模块状态扩展
-- 扩展 eo_payment.status 以支持 DDD 两阶段支付/退款流程
-- ===================================================================

-- 1. 更新 eo_payment.status CHECK 约束
-- 旧值: 0=待支付, 1=支付中, 2=已支付, 3=已退款, 4=已关闭
-- 新值: 0=待支付, 1=已支付, 2=已退款, 3=部分退款, 4=支付失败, 5=已关闭, 6=支付中, 7=退款中
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'chk_payment_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist > 0, 'ALTER TABLE `eo_payment` DROP CHECK `chk_payment_status`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `eo_payment` ADD CONSTRAINT `chk_payment_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5, 6, 7));

-- 2. 更新 eo_payment.payment_method CHECK 约束（保持兼容）
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'chk_payment_method' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist > 0, 'ALTER TABLE `eo_payment` DROP CHECK `chk_payment_method`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `eo_payment` ADD CONSTRAINT `chk_payment_method` CHECK (`payment_method` IS NULL OR `payment_method` IN (1, 2, 3));

-- 3. 放宽 transaction_id 唯一约束（允许 NULL，退款记录可能无第三方流水号）
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'uk_eo_payment_transaction_id');
SET @sql := IF(@exist > 0, 'ALTER TABLE `eo_payment` DROP INDEX `uk_eo_payment_transaction_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 重新创建为允许 NULL 的唯一索引
ALTER TABLE `eo_payment` ADD UNIQUE KEY `uk_eo_payment_transaction_id` (`transaction_id`);
