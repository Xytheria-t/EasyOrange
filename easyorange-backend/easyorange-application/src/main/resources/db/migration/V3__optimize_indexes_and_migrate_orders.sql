-- ===================================================================
-- EasyOrange AI 智能托管平台 - 索引优化 + 订单结构迁移
-- Version: V3 (合并原 V3__optimize_indexes + V3__migrate_existing_orders_to_items)
-- 职责: 删除冗余索引、优化复合索引、订单数据迁移、eo_order 表结构调整
-- Database: MySQL 8.0
-- ===================================================================

-- ===================================================================
-- 1. 删除冗余索引（不含 eo_order，结构调整在 3.2 统一处理）
-- ===================================================================

-- 1.1 eo_product: idx_eo_product_user_time 是 idx_eo_product_user_status_del 的前缀索引
DROP INDEX `idx_eo_product_user_time` ON `eo_product`;

-- 1.2 eo_payment: idx_eo_payment_user_time 是 idx_eo_payment_user_status 的前缀索引
DROP INDEX `idx_eo_payment_user_time` ON `eo_payment`;

-- 1.3 eo_message_subscription: idx_eo_message_subscription_user 被唯一键覆盖
DROP INDEX `idx_eo_message_subscription_user` ON `eo_message_subscription`;

-- ===================================================================
-- 2. 优化复合索引
-- ===================================================================

-- 2.1 eo_product_review: 替换冗余单列索引为复合索引
DROP INDEX `idx_eo_product_review_product_id` ON `eo_product_review`;
DROP INDEX `idx_eo_product_review_create_time` ON `eo_product_review`;
ALTER TABLE `eo_product_review`
    ADD INDEX `idx_eo_product_review_product_status_del_time` (`product_id`, `status`, `del_flag`, `create_time` DESC);

-- 2.2 eo_message: 替换为覆盖 type 字段的索引（支持 GROUP BY type）
DROP INDEX `idx_eo_message_receiver_read_time` ON `eo_message`;
ALTER TABLE `eo_message`
    ADD INDEX `idx_eo_message_receiver_read_type_del_time` (`receiver_id`, `is_read`, `del_flag`, `type`, `create_time` DESC);

-- 2.3 eo_product: 添加按浏览量排序的热门商品查询索引
ALTER TABLE `eo_product`
    ADD INDEX `idx_eo_product_status_del_view` (`status`, `del_flag`, `view_count` DESC);

-- ===================================================================
-- 3. 订单行项数据迁移 + eo_order 表结构调整
-- ===================================================================

-- 3.1 将已有单商品订单数据迁移为行项
SET @next_item_id := (SELECT COALESCE(MAX(id), 0) FROM eo_order_item);

INSERT INTO eo_order_item (id, order_id, product_id, product_snapshot, unit_price, quantity, subtotal,
                           create_time, update_time, create_by, update_by, del_flag, version)
SELECT
    (@next_item_id := @next_item_id + 1) AS id,
    o.id AS order_id,
    o.product_id,
    '{}' AS product_snapshot,
    o.amount AS unit_price,
    1 AS quantity,
    o.amount AS subtotal,
    o.create_time, o.update_time, o.create_by, o.update_by, o.del_flag, o.version
FROM eo_order o
ORDER BY o.id;

SET @next_item_id := NULL;

-- 3.2 改造 eo_order 表结构：删除已废弃的 product_id 相关索引和列，重命名 amount 为 total_amount
-- 注意：必须先删除引用 amount 列的 check constraint（MySQL 8.4+ 不允许重命名被约束引用的列）
ALTER TABLE eo_order
    DROP CHECK `chk_eo_order_amount`;
ALTER TABLE eo_order
    DROP INDEX `idx_eo_order_product_id`,
    DROP INDEX `idx_eo_order_product_del`,
    DROP INDEX `idx_eo_order_payment_status`,
    DROP COLUMN `product_id`,
    CHANGE COLUMN `amount` `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额';
