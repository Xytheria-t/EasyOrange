-- ===================================================================
-- EasyOrange 校园二手交易平台 - 订单数据迁移
-- Version: V3
-- 职责: 将已有单商品订单数据迁移为行项，改造 eo_order 表结构
-- Database: MySQL 8.0
-- ===================================================================

-- ===================================================================
-- 1. 将已有单商品订单数据迁移为行项
-- ===================================================================

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

-- ===================================================================
-- 2. 改造 eo_order 表结构
--    先删除涉及 product_id 的索引，再删除列
--    然后将 amount 重命名为 total_amount
-- ===================================================================

ALTER TABLE eo_order
    DROP INDEX `idx_eo_order_product_id`,
    DROP INDEX `idx_eo_order_product_del`,
    DROP COLUMN `product_id`,
    CHANGE COLUMN `amount` `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额';
