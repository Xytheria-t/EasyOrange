-- ===================================================================
-- EasyOrange - V7 Saga 状态表
-- 订单模块 Saga 分布式事务状态持久化
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

-- 添加索引以支持查询正在处理或需要重试的 Saga
CREATE INDEX `idx_saga_retry` ON `saga_status` (`state`, `retry_count`) 
    WHERE `state` IN ('COMPENSATING', 'FAILED') AND `retry_count` < 3;
