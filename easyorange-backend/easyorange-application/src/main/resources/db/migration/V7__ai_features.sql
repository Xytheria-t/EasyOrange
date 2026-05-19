-- ===================================================================
-- EasyOrange 校园二手交易平台 - AI 功能表
-- Version: V7
-- 职责: 创建 AI 问答和审核建议相关表（纯 DDL）
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- ===================================================================

-- ===================================================================
-- 1. 商品问答表
-- ===================================================================

CREATE TABLE `eo_product_question` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `question` TEXT NOT NULL COMMENT '问题内容',
    `answer` TEXT DEFAULT NULL COMMENT 'AI 回答内容',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 待回答 1 已回答 2 已驳回）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_question_product_id` (`product_id`),
    KEY `idx_eo_product_question_user_id` (`user_id`),
    KEY `idx_eo_product_question_status_time` (`status`, `create_time` DESC),
    CONSTRAINT `chk_eo_product_question_status` CHECK (`status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品问答表';

-- ===================================================================
-- 2. AI 审核建议表
-- ===================================================================

CREATE TABLE `eo_audit_suggestion` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID',
    `suggestion_type` VARCHAR(50) NOT NULL COMMENT '建议类型（PRICE_AUDIT/DESCRIPTION_AUDIT/CATEGORY_AUDIT/IMAGE_AUDIT）',
    `suggestion_content` JSON DEFAULT NULL COMMENT '建议内容（JSON）',
    `confidence` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '置信度（0.00-1.00）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 待处理 1 已采纳 2 已忽略）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_eo_audit_suggestion_product_id` (`product_id`),
    KEY `idx_eo_audit_suggestion_type_status` (`suggestion_type`, `status`, `create_time` DESC),
    CONSTRAINT `chk_eo_audit_suggestion_status` CHECK (`status` IN (0, 1, 2)),
    CONSTRAINT `chk_eo_audit_suggestion_confidence` CHECK (`confidence` >= 0.00 AND `confidence` <= 1.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 审核建议表';