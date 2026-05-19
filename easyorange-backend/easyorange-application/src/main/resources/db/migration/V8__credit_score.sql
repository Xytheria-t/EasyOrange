-- ===================================================================
-- EasyOrange 校园二手交易平台 - 信用评分表
-- Version: V8
-- 职责: 创建用户信用评分和变更日志表（纯 DDL）
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- ===================================================================

-- ===================================================================
-- 1. 用户信用评分表
-- ===================================================================

CREATE TABLE `eo_user_credit` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `credit_score` INT NOT NULL DEFAULT 100 COMMENT '信用评分（0-200）',
    `level` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '信用等级（EXCELLENT/GOOD/NORMAL/LOW/BLACKLIST）',
    `total_trades` INT NOT NULL DEFAULT 0 COMMENT '总交易数',
    `completed_trades` INT NOT NULL DEFAULT 0 COMMENT '已完成交易数',
    `cancelled_trades` INT NOT NULL DEFAULT 0 COMMENT '已取消交易数',
    `total_reports` INT NOT NULL DEFAULT 0 COMMENT '总举报数',
    `confirmed_reports` INT NOT NULL DEFAULT 0 COMMENT '已确认举报数',
    `review_avg_rating` DECIMAL(3,2) DEFAULT NULL COMMENT '评价平均分',
    `last_updated` DATETIME DEFAULT NULL COMMENT '最后评分更新时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_user_credit_user_id` (`user_id`),
    KEY `idx_eo_user_credit_score` (`credit_score`),
    KEY `idx_eo_user_credit_level` (`level`),
    KEY `idx_eo_user_credit_last_updated` (`last_updated`),
    CONSTRAINT `chk_eo_user_credit_score` CHECK (`credit_score` >= 0 AND `credit_score` <= 200),
    CONSTRAINT `chk_eo_user_credit_total_trades` CHECK (`total_trades` >= 0),
    CONSTRAINT `chk_eo_user_credit_completed_trades` CHECK (`completed_trades` >= 0),
    CONSTRAINT `chk_eo_user_credit_cancelled_trades` CHECK (`cancelled_trades` >= 0),
    CONSTRAINT `chk_eo_user_credit_total_reports` CHECK (`total_reports` >= 0),
    CONSTRAINT `chk_eo_user_credit_confirmed_reports` CHECK (`confirmed_reports` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信用评分表';

-- ===================================================================
-- 2. 信用变更日志表
-- ===================================================================

CREATE TABLE `eo_credit_change_log` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `change_amount` INT NOT NULL COMMENT '变更分值',
    `before_score` INT NOT NULL COMMENT '变更前评分',
    `after_score` INT NOT NULL COMMENT '变更后评分',
    `change_type` VARCHAR(30) NOT NULL COMMENT '变更类型（TRADE_COMPLETE/TRADE_CANCEL/REPORT_CONFIRMED/REVIEW_RATING/RECALCULATE/ADMIN_ADJUST）',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    `reference_id` BIGINT DEFAULT NULL COMMENT '关联业务 ID（订单ID/举报ID等）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_eo_credit_change_log_user_id` (`user_id`),
    KEY `idx_eo_credit_change_log_type_time` (`change_type`, `create_time` DESC),
    KEY `idx_eo_credit_change_log_create_time` (`create_time` DESC),
    CONSTRAINT `chk_eo_credit_change_log_change_amount` CHECK (`change_amount` <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信用变更日志表';