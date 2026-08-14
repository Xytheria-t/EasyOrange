-- ===================================================================
-- EasyOrange - 用户长期画像表
-- Description: Agent 长期记忆 — 从对话中提取的用户偏好（风格/成色/价格区间），
--              聊天时注入 prompt，记忆可跨会话持久
-- ===================================================================

CREATE TABLE IF NOT EXISTS `eo_user_preference` (
    `id`          VARCHAR(36)  NOT NULL COMMENT '主键 UUID v7',
    `user_id`     VARCHAR(36)  NOT NULL COMMENT '用户 ID',
    `pref_key`    VARCHAR(64)  NOT NULL COMMENT '偏好键（如 style/condition/price_range）',
    `pref_value`  VARCHAR(255) NOT NULL COMMENT '偏好值',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(36)  NULL COMMENT '创建人',
    `update_by`   VARCHAR(36)  NULL COMMENT '更新人',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0/1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_pref` (`user_id`, `pref_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户长期画像（Agent 长期记忆，聊天气氛注入）';
