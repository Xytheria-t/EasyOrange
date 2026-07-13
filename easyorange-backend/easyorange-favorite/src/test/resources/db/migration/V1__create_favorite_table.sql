CREATE TABLE `eo_favorite` (
    `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `user_id`    VARCHAR(36) NOT NULL COMMENT '用户 ID',
    `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`   TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version`    INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_favorite_user_product_del` (`user_id`, `product_id`, `del_flag`),
    KEY `idx_eo_favorite_user_time` (`user_id`, `create_time` DESC),
    KEY `idx_eo_favorite_product_count` (`product_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';
