-- ===================================================================
-- EasyOrange - V3 支付基础设施
-- 整合原 V5+V6+V7+V8+V9：领域事件 + Saga + 幂等性 + 支付状态扩展
-- ===================================================================

-- ===================================================================
-- 1. 领域事件表 (Outbox 模式)
-- ===================================================================

CREATE TABLE IF NOT EXISTS `eo_domain_event` (
    `id`              BIGINT          NOT NULL                 COMMENT '主键ID',
    `event_id`        CHAR(36)        NOT NULL                 COMMENT '事件唯一标识(UUID)',
    `aggregate_type`  VARCHAR(100)    NOT NULL                 COMMENT '聚合类型',
    `aggregate_id`    BIGINT          NOT NULL                 COMMENT '聚合ID',
    `event_type`      VARCHAR(100)    NOT NULL                 COMMENT '事件类型',
    `payload`         TEXT            DEFAULT NULL             COMMENT '事件载荷(JSON)',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/PUBLISHED/FAILED)',
    `created_at`      DATETIME(3)     NOT NULL                 COMMENT '事件创建时间',
    `published_at`    DATETIME(3)     DEFAULT NULL             COMMENT '事件发布时间',
    `del_flag`        TINYINT         NOT NULL DEFAULT 0       COMMENT '删除标志(0=正常 2=已删除)',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    `create_by`       BIGINT          DEFAULT NULL             COMMENT '创建人ID',
    `update_by`       BIGINT          DEFAULT NULL             COMMENT '更新人ID',
    `version`         INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_event_type` (`event_type`),
    CONSTRAINT `chk_domain_event_status` CHECK (`status` IN ('PENDING', 'PUBLISHED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领域事件表';

-- ===================================================================
-- 2. Saga 分布式事务状态表
-- ===================================================================

CREATE TABLE IF NOT EXISTS `saga_status` (
    `saga_id`        CHAR(36)        NOT NULL                 COMMENT 'Saga 实例唯一标识(UUID)',
    `saga_type`      VARCHAR(100)    NOT NULL                 COMMENT 'Saga 类型(如 CREATE_ORDER, CANCEL_ORDER)',
    `state`          VARCHAR(20)     NOT NULL                 COMMENT 'Saga 状态(STARTED/ORDER_CREATED/PAYMENT_CREATED/COMPLETED/COMPENSATING/COMPENSATED/FAILED)',
    `current_step`   VARCHAR(50)     DEFAULT NULL             COMMENT '当前执行步骤',
    `payload`        TEXT            DEFAULT NULL             COMMENT 'Saga 载荷(JSON格式存储命令数据)',
    `error_message`  TEXT            DEFAULT NULL             COMMENT '错误信息',
    `retry_count`    INT             NOT NULL DEFAULT 0       COMMENT '重试次数',
    `created_at`     DATETIME(3)     NOT NULL                 COMMENT 'Saga 创建时间',
    `updated_at`     DATETIME(3)     NOT NULL                 COMMENT 'Saga 更新时间',
    PRIMARY KEY (`saga_id`),
    KEY `idx_saga_type_state` (`saga_type`, `state`),
    KEY `idx_saga_state_created` (`state`, `created_at`),
    KEY `idx_saga_created_at` (`created_at`),
    CONSTRAINT `chk_saga_state` CHECK (
        `state` IN ('STARTED', 'ORDER_CREATED', 'PAYMENT_CREATED', 'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'FAILED')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Saga 分布式事务状态表';

-- ===================================================================
-- 3. 幂等性键表
-- ===================================================================

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
