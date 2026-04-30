-- ===================================================================
-- EasyOrange - V5 领域事件表
-- 支付模块 DDD 领域事件持久化
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
