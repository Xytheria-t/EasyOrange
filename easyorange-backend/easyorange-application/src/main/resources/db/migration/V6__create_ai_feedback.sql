-- ===================================================================
-- EasyOrange - AI 输出用户反馈表
-- Description: 👍/👎 反馈飞轮 — 用户反馈入库，可导出扩充金标准评测集
-- ===================================================================

CREATE TABLE IF NOT EXISTS `eo_ai_feedback` (
    `id`            VARCHAR(36)  NOT NULL COMMENT '主键 UUID v7',
    `scope`         VARCHAR(32)  NOT NULL COMMENT 'AI 调用场景 (QA/CHAT/SEMANTIC/...)',
    `query_text`    TEXT         NULL COMMENT '用户问题',
    `response_text` TEXT         NULL COMMENT 'AI 回答',
    `helpful`       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否有帮助 1/0',
    `comment`       VARCHAR(500) NULL COMMENT '用户补充意见',
    `call_log_id`   VARCHAR(36)  NULL COMMENT '关联 eo_ai_call_log.id（可为空）',
    `user_id`       VARCHAR(36)  NULL COMMENT '反馈用户 ID',
    `exported`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已导出进金标准评测集 1/0',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_feedback_exported` (`exported`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 输出用户反馈（反馈飞轮，导出后自动扩充金标准评测集）';
