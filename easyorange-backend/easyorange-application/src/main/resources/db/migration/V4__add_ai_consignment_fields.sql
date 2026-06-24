-- ===================================================================
-- EasyOrange AI 智能托管平台 - AI托管寄售相关字段
-- Version: V4
-- 职责: eo_product 表新增底价/寄售模式/上架时间/降价阶梯字段
-- Database: MySQL 8.0
-- ===================================================================

-- 新增 AI 托管寄售字段
ALTER TABLE eo_product
    ADD COLUMN floor_price DECIMAL(10, 2) NULL COMMENT '底价（AI议价/降价的最低价格）',
    ADD COLUMN consignment_mode TINYINT NOT NULL DEFAULT 0 COMMENT '寄售模式: 0=手动, 1=AI托管',
    ADD COLUMN listed_at DATETIME NULL COMMENT '上架时间（用于阶梯降价计算）',
    ADD COLUMN current_price_level TINYINT NOT NULL DEFAULT 0 COMMENT '当前降价阶梯: 0=原价,1=降5%,2=降10%,3=底价';

-- 为 AI 托管商品查询添加索引
CREATE INDEX idx_product_ai_managed ON eo_product(consignment_mode, status);
