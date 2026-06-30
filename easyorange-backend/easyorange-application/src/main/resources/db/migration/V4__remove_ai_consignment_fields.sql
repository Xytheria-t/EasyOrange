-- ===================================================================
-- EasyOrange AI 资产管理平台 - AI 托管寄售相关字段回滚
-- Version: V4
-- 职责: 撤销 eo_product 表上的底价/寄售模式/上架时间/降价阶梯字段
-- 说明: 议价与阶梯降价功能已下线，移除相关字段以保持模型一致
-- Database: MySQL 8.0
-- ===================================================================

-- 删除 AI 托管寄售字段
ALTER TABLE `eo_product`
    DROP COLUMN `floor_price`,
    DROP COLUMN `consignment_mode`,
    DROP COLUMN `listed_at`,
    DROP COLUMN `current_price_level`;

-- 删除 AI 托管商品查询索引
DROP INDEX `idx_product_ai_managed` ON `eo_product`;
