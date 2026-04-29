-- ===================================================================
-- EasyOrange - V4 修复 schema 不一致问题
-- 对齐 Java 实体类与数据库 schema
-- 对齐 V1+V3 与 init.sql 差异
-- ===================================================================

-- 1. Payment: 补充 refunded_amount 列 (P0-4)
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND COLUMN_NAME = 'refunded_amount');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_payment` ADD COLUMN `refunded_amount` DECIMAL(10,2) DEFAULT 0 COMMENT ''已退款金额'' AFTER `amount`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Order: 补充 cancel_reason 和 cancel_time 列 (P0-3)
-- 注: V3 已添加但可能未执行成功，这里确保存在
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND COLUMN_NAME = 'cancel_reason');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD COLUMN `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT ''取消原因'' AFTER `remark`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND COLUMN_NAME = 'cancel_time');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD COLUMN `cancel_time` DATETIME DEFAULT NULL COMMENT ''取消时间'' AFTER `cancel_reason`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Product: 补充 price_update_time 列 (P2-5)
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'price_update_time');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD COLUMN `price_update_time` DATETIME DEFAULT NULL COMMENT ''价格最后更新时间'' AFTER `price`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. ProductDetail: 修复主键设计 (P0-5)
-- 删除独立 id 列，将 product_id 改为主键
-- 注意: 如果表已有数据且存在非空 id 值，此操作会丢失数据
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_detail' AND COLUMN_NAME = 'id');
SET @pk_exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_detail' AND CONSTRAINT_NAME = 'PRIMARY'
                  AND TABLE_SCHEMA IN (
                    SELECT TABLE_SCHEMA FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_NAME = 'product_detail' AND COLUMN_NAME = 'product_id' AND CONSTRAINT_NAME = 'PRIMARY'
                  ));
SET @sql := IF(@exist > 0 AND @pk_exist = 0, 'ALTER TABLE `product_detail` DROP PRIMARY KEY, DROP COLUMN `id`, ADD PRIMARY KEY (`product_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. SearchHistory: 补充唯一约束 (P1-1)
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'search_history' AND CONSTRAINT_NAME = 'uk_search_user_keyword');
SET @sql := IF(@exist = 0, 'ALTER TABLE `search_history` ADD CONSTRAINT `uk_search_user_keyword` UNIQUE (`user_id`, `keyword`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. SysUser: 补充 user_type CHECK 约束 (P1-3)
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND CONSTRAINT_NAME = 'chk_user_type' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `sys_user` ADD CONSTRAINT `chk_user_type` CHECK (`user_type` IN (''01'', ''02''))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. 补充缺失外键约束 (P1-4)
-- category.parent_id -> category.id
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND CONSTRAINT_NAME = 'fk_category_parent');
SET @sql := IF(@exist = 0, 'ALTER TABLE `category` ADD CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `category`(`id`) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- product_report.product_id -> product.id
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_report' AND CONSTRAINT_NAME = 'fk_report_product');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product_report` ADD CONSTRAINT `fk_report_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- product_report.reporter_id -> sys_user.user_id
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_report' AND CONSTRAINT_NAME = 'fk_report_reporter');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product_report` ADD CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `sys_user`(`user_id`) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_offline_message.user_id -> sys_user.user_id
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_offline_message' AND CONSTRAINT_NAME = 'fk_offline_user');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_offline_message` ADD CONSTRAINT `fk_offline_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`user_id`) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_offline_message.message_id -> eo_message.id
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_offline_message' AND CONSTRAINT_NAME = 'fk_offline_message');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_offline_message` ADD CONSTRAINT `fk_offline_message` FOREIGN KEY (`message_id`) REFERENCES `eo_message`(`id`) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8. 统一字段类型 (P2-1, P2-2)
-- sys_user.status: VARCHAR(1) -> CHAR(1)
SET @sql := 'ALTER TABLE `sys_user` MODIFY COLUMN `status` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''帐号状态（0 正常 1 禁用 2 锁定）''';
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- sys_user.sex: VARCHAR(1) -> CHAR(1)
SET @sql := 'ALTER TABLE `sys_user` MODIFY COLUMN `sex` CHAR(1) DEFAULT ''0'' COMMENT ''用户性别（0 未知 1 男 2 女）''';
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 9. 为 eo_payment.refunded_amount 添加 CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'chk_payment_refunded_amount' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_payment` ADD CONSTRAINT `chk_payment_refunded_amount` CHECK (`refunded_amount` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
