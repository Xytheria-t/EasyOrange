-- ---------------------------------------------------------------------------
-- V3: AI 调用日志表 — LLM-as-Judge 离线评估数据源。
-- 每次 LLM/Embedding 调用由 AiCallLogRecorder 记录（不阻塞主链路，失败仅告警），
-- AiEvalScheduler 定时对未评审记录打分（1-5 + 评语），用于回答
-- 「怎么判断 AI 输出质量」——把 AI 输出质量从「感觉还行」变成「可量化、可回归」。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `eo_ai_call_log` (
    `id`            VARCHAR(36)  NOT NULL COMMENT '主键 UUID v7',
    `scope`         VARCHAR(32)  NOT NULL COMMENT 'AI 调用场景 (PRICING/REVIEW/COPY/AUTO_LISTING/SEMANTIC/QA/SEARCH_ENHANCE)',
    `model`         VARCHAR(64)  NOT NULL COMMENT '模型标识',
    `prompt_hash`   CHAR(32)     NOT NULL COMMENT 'system+user prompt 摘要 MD5（去重与回归用）',
    `response_text` TEXT         NULL COMMENT '模型输出文本',
    `latency_ms`    BIGINT       NOT NULL DEFAULT 0 COMMENT '调用耗时毫秒',
    `success`       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否成功 1/0',
    `error_msg`     VARCHAR(512) NULL COMMENT '失败原因',
    `judge_score`   TINYINT      NULL COMMENT 'LLM-as-Judge 质量评分 1-5（NULL=待评估）',
    `judge_comment` VARCHAR(255) NULL COMMENT '评审评语',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_call_log_scope` (`scope`, `created_at`),
    KEY `idx_ai_call_log_judge` (`judge_score`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'AI 调用日志（LLM-as-Judge 离线评估数据源）';
