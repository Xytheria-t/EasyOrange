-- ===================================================================
-- EasyOrange 校园二手交易平台 - 添加商品评价表
-- Version: V3
-- 职责: 创建商品评价相关表结构、索引、约束（纯 DDL）
-- ===================================================================

CREATE TABLE `eo_product_review` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID',
    `user_id` BIGINT NOT NULL COMMENT '评价用户 ID',
    `order_id` BIGINT NOT NULL COMMENT '关联订单 ID',
    `rating` TINYINT NOT NULL DEFAULT 5 COMMENT '评分（1-5）',
    `content` TEXT NOT NULL COMMENT '评价内容',
    `reply_content` TEXT DEFAULT NULL COMMENT '卖家回复内容',
    `reply_time` DATETIME DEFAULT NULL COMMENT '卖家回复时间',
    `likes` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0 隐藏 1 显示 2 待审核）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_review_product_id` (`product_id`),
    KEY `idx_eo_product_review_user_id` (`user_id`),
    KEY `idx_eo_product_review_order_id` (`order_id`),
    KEY `idx_eo_product_review_create_time` (`create_time` DESC),
    CONSTRAINT `chk_eo_product_review_rating` CHECK (`rating` >= 1 AND `rating` <= 5),
    CONSTRAINT `chk_eo_product_review_status` CHECK (`status` IN (0, 1, 2)),
    CONSTRAINT `chk_eo_product_review_likes` CHECK (`likes` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';
