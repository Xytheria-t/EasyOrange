-- ===================================================================
-- EasyOrange AI 智能托管平台 - 订单行项表
-- Version: V2
-- 职责: 创建 eo_order_item 订单行项表，支持多商品订单
-- Database: MySQL 8.0
-- ===================================================================

-- ===================================================================
-- 订单行项表
-- ===================================================================

CREATE TABLE `eo_order_item` (
    `id`               BIGINT          NOT NULL COMMENT '主键 ID',
    `order_id`         BIGINT          NOT NULL COMMENT '订单 ID',
    `product_id`       BIGINT          NOT NULL COMMENT '商品 ID',
    `product_snapshot` JSON            NOT NULL COMMENT '下单时商品信息快照',
    `unit_price`       DECIMAL(10,2)   NOT NULL COMMENT '单价',
    `quantity`         INT             NOT NULL DEFAULT 1 COMMENT '数量',
    `subtotal`         DECIMAL(10,2)   NOT NULL COMMENT '小计金额',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT          DEFAULT NULL COMMENT '创建者',
    `update_by`        BIGINT          DEFAULT NULL COMMENT '更新者',
    `del_flag`         TINYINT         NOT NULL DEFAULT 0 COMMENT '删除标志',
    `version`          INT             NOT NULL DEFAULT 0 COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY `idx_eo_order_item_order_id`   (`order_id`, `del_flag`) COMMENT '订单 ID 索引',
    KEY `idx_eo_order_item_product_id` (`product_id`, `del_flag`) COMMENT '商品 ID 索引',
    CONSTRAINT `chk_eo_order_item_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `chk_eo_order_item_subtotal` CHECK (`subtotal` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单行项表';
