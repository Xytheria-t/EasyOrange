-- ===================================================================
-- EasyOrange 校园二手交易平台 - 测试数据清理脚本
-- 说明：此文件用于清理测试数据，恢复数据库到初始状态
-- 用途：测试完成后执行，清理测试数据
-- 注意：按照外键依赖的逆序删除数据
-- ===================================================================

-- ===================================================================
-- 1. 清理操作日志（无外键依赖）
-- ===================================================================

DELETE FROM `eo_oper_log` WHERE 1=1;
DELETE FROM `eo_oper_log_archive` WHERE 1=1;

-- ===================================================================
-- 2. 清理消息相关数据
-- ===================================================================

DELETE FROM `eo_offline_message` WHERE 1=1;
DELETE FROM `eo_message_subscription` WHERE 1=1;
DELETE FROM `eo_message` WHERE 1=1;
DELETE FROM `eo_message_template` WHERE 1=1;

-- ===================================================================
-- 3. 清理支付相关数据
-- ===================================================================

DELETE FROM `eo_idempotency_key` WHERE 1=1;
DELETE FROM `eo_payment` WHERE 1=1;
DELETE FROM `eo_payment_config` WHERE 1=1;

-- ===================================================================
-- 4. 清理订单相关数据
-- ===================================================================

DELETE FROM eo_saga_status WHERE 1=1;
DELETE FROM `eo_domain_event` WHERE 1=1;
DELETE FROM `eo_order` WHERE 1=1;

-- ===================================================================
-- 5. 清理收藏数据
-- ===================================================================

DELETE FROM `eo_favorite` WHERE 1=1;

-- ===================================================================
-- 6. 清理商品相关数据
-- ===================================================================

DELETE FROM `eo_product_report` WHERE 1=1;
DELETE FROM `eo_product_detail` WHERE 1=1;
DELETE FROM `eo_product_image` WHERE 1=1;
DELETE FROM `eo_product` WHERE 1=1;

-- ===================================================================
-- 7. 清理搜索相关数据
-- ===================================================================

DELETE FROM `eo_search_history` WHERE 1=1;
DELETE FROM `eo_hot_keyword` WHERE 1=1;

-- ===================================================================
-- 8. 清理分类数据（仅清理测试数据，保留种子数据）
-- ===================================================================

DELETE FROM `eo_category` WHERE `id` >= 10;

-- ===================================================================
-- 9. 清理用户数据
-- ===================================================================

DELETE FROM `eo_user` WHERE `user_id` >= 1;

-- ===================================================================
-- 10. 重置自增ID（可选）
-- ===================================================================

-- ALTER TABLE `eo_user` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_category` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_product` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_product_image` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_product_detail` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_favorite` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_order` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_payment` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_message` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_search_history` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_hot_keyword` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_product_report` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_message_template` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_message_subscription` AUTO_INCREMENT = 1;
-- ALTER TABLE `eo_payment_config` AUTO_INCREMENT = 1;

-- ===================================================================
-- 清理完成
-- ===================================================================
