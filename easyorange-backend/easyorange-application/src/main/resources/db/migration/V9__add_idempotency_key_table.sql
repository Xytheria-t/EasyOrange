-- 添加幂等性键表
CREATE TABLE IF NOT EXISTS `eo_idempotency_key` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `idempotency_key` VARCHAR(255)    NOT NULL                 COMMENT '幂等性键',
    `user_id`         BIGINT          NOT NULL                 COMMENT '用户ID',
    `request_hash`    VARCHAR(64)     NOT NULL                 COMMENT '请求哈希',
    `response_data`   TEXT            DEFAULT NULL             COMMENT '响应数据(JSON)',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/COMPLETED/FAILED)',
    `expires_at`      DATETIME        NOT NULL                 COMMENT '过期时间',
    `del_flag`        TINYINT         NOT NULL DEFAULT 0       COMMENT '删除标志(0=正常 2=已删除)',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    `create_by`       BIGINT          DEFAULT NULL             COMMENT '创建人ID',
    `update_by`       BIGINT          DEFAULT NULL             COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    KEY `idx_user_expires` (`user_id`, `expires_at`),
    CONSTRAINT `chk_idempotency_status` CHECK (`status` IN ('PENDING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等性键表';
