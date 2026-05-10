-- ===================================================================
-- EasyOrange 校园二手交易平台 - 索引优化
-- Version: V4
-- 职责: 优化商品表和订单表索引，添加 del_flag 字段
-- ===================================================================

-- 优化商品表索引（添加 del_flag）
ALTER TABLE `eo_product` 
    DROP INDEX `idx_eo_product_user_time`,
    ADD INDEX `idx_eo_product_user_time` (`user_id`, `del_flag`, `create_time` DESC);

ALTER TABLE `eo_product`
    DROP INDEX `idx_eo_product_category_status_time`,
    ADD INDEX `idx_eo_product_category_status_time` (`category_id`, `status`, `del_flag`, `create_time` DESC);

ALTER TABLE `eo_product`
    DROP INDEX `idx_eo_product_status_price`,
    ADD INDEX `idx_eo_product_status_del_price` (`status`, `del_flag`, `price`);

-- 优化订单表索引
ALTER TABLE `eo_order`
    ADD INDEX `idx_eo_order_product_del` (`product_id`, `del_flag`);
