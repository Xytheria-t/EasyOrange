-- ===================================================================
-- EasyOrange - V3 补充 V1 缺失的字段和约束
-- 对齐 init.sql 的完整 schema
-- ===================================================================

-- 注意：Flyway 不支持 DELIMITER 语法，使用条件执行确保幂等
-- MySQL 8.0+ 支持 IF EXISTS / IF NOT EXISTS 语法

-- 1. eo_order 表补充缺失字段
-- 使用存储过程方式会导致 Flyway 解析失败，改用直接 ALTER + 忽略错误
-- 如果字段已存在会报错，但不影响后续语句执行

-- 添加 cancel_reason 字段 (如果不存在)
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND COLUMN_NAME = 'cancel_reason');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD COLUMN `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT ''取消原因'' AFTER `remark`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 cancel_time 字段 (如果不存在)
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND COLUMN_NAME = 'cancel_time');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD COLUMN `cancel_time` DATETIME DEFAULT NULL COMMENT ''取消时间'' AFTER `cancel_reason`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加外键约束
-- product 表外键
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND CONSTRAINT_NAME = 'fk_product_category');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- product_image 表外键
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_image' AND CONSTRAINT_NAME = 'fk_image_product');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product_image` ADD CONSTRAINT `fk_image_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_order 表外键
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND CONSTRAINT_NAME = 'fk_order_buyer');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD CONSTRAINT `fk_order_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `sys_user`(`user_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND CONSTRAINT_NAME = 'fk_order_seller');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD CONSTRAINT `fk_order_seller` FOREIGN KEY (`seller_id`) REFERENCES `sys_user`(`user_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND CONSTRAINT_NAME = 'fk_order_product');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD CONSTRAINT `fk_order_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_payment 表外键
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'fk_payment_order');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_payment` ADD CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `eo_order`(`id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'fk_payment_user');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_payment` ADD CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`user_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_message 表外键
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_message' AND CONSTRAINT_NAME = 'fk_message_sender');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_message` ADD CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `sys_user`(`user_id`) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_message' AND CONSTRAINT_NAME = 'fk_message_receiver');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_message` ADD CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `sys_user`(`user_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_file 表外键
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'upload_file' AND CONSTRAINT_NAME = 'fk_file_uploader');
SET @sql := IF(@exist = 0, 'ALTER TABLE `upload_file` ADD CONSTRAINT `fk_file_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `sys_user`(`user_id`) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加 CHECK 约束 (MySQL 8.0+)
-- eo_order CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND CONSTRAINT_NAME = 'chk_order_amount' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD CONSTRAINT `chk_order_amount` CHECK (`amount` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND CONSTRAINT_NAME = 'chk_order_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD CONSTRAINT `chk_order_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_order' AND CONSTRAINT_NAME = 'chk_order_payment_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_order` ADD CONSTRAINT `chk_order_payment_status` CHECK (`payment_status` IN (0, 1, 2))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_payment CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'chk_payment_amount' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_payment` ADD CONSTRAINT `chk_payment_amount` CHECK (`amount` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'chk_payment_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_payment` ADD CONSTRAINT `chk_payment_status` CHECK (`status` IN (0, 1, 2, 3, 4))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_payment' AND CONSTRAINT_NAME = 'chk_payment_method' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_payment` ADD CONSTRAINT `chk_payment_method` CHECK (`payment_method` IS NULL OR `payment_method` IN (1, 2, 3))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- product CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND CONSTRAINT_NAME = 'chk_product_price' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD CONSTRAINT `chk_product_price` CHECK (`price` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND CONSTRAINT_NAME = 'chk_product_original_price' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD CONSTRAINT `chk_product_original_price` CHECK (`original_price` IS NULL OR `original_price` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND CONSTRAINT_NAME = 'chk_product_stock' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD CONSTRAINT `chk_product_stock` CHECK (`stock` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND CONSTRAINT_NAME = 'chk_product_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD CONSTRAINT `chk_product_status` CHECK (`status` IN (0, 1, 2, 3))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND CONSTRAINT_NAME = 'chk_product_condition' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD CONSTRAINT `chk_product_condition` CHECK (`condition_level` IS NULL OR (`condition_level` >= 1 AND `condition_level` <= 10))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND CONSTRAINT_NAME = 'chk_product_view_count' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `product` ADD CONSTRAINT `chk_product_view_count` CHECK (`view_count` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- sys_user CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND CONSTRAINT_NAME = 'chk_user_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `sys_user` ADD CONSTRAINT `chk_user_status` CHECK (`status` IN (''0'', ''1'', ''2''))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND CONSTRAINT_NAME = 'chk_user_sex' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `sys_user` ADD CONSTRAINT `chk_user_sex` CHECK (`sex` IS NULL OR `sex` IN (''0'', ''1'', ''2''))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- category CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND CONSTRAINT_NAME = 'chk_category_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `category` ADD CONSTRAINT `chk_category_status` CHECK (`status` IN (0, 1))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_message CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_message' AND CONSTRAINT_NAME = 'chk_message_is_read' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_message` ADD CONSTRAINT `chk_message_is_read` CHECK (`is_read` IN (0, 1))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- hot_keyword CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hot_keyword' AND CONSTRAINT_NAME = 'chk_hot_keyword_count' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `hot_keyword` ADD CONSTRAINT `chk_hot_keyword_count` CHECK (`search_count` >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- sys_oper_log CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_oper_log' AND CONSTRAINT_NAME = 'chk_oper_log_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `sys_oper_log` ADD CONSTRAINT `chk_oper_log_status` CHECK (`status` IN (0, 1))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- eo_offline_message CHECK 约束
SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eo_offline_message' AND CONSTRAINT_NAME = 'chk_offline_message_push_status' AND CONSTRAINT_TYPE = 'CHECK');
SET @sql := IF(@exist = 0, 'ALTER TABLE `eo_offline_message` ADD CONSTRAINT `chk_offline_message_push_status` CHECK (`push_status` IN (0, 1, 2))', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
