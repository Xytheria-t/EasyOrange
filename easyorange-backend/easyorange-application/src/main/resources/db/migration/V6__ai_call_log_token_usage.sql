-- AI 调用日志补用量与主体列
-- 背景：eo_ai_call_log 原先只有 latency_ms 与 response_text，没有 token 列 —— 能出「谁调用得多」，
-- 出不了「谁花得多」，也出不了按调用粒度的成本报表（见 doc/技术债务清单.md TD-015）。
-- 用量值来自供应商回报的 usage：AiModelSupport 记账时已经拿到了，只是过去只进内存预算器、没落库。
ALTER TABLE `eo_ai_call_log`
    ADD COLUMN `token_input`  INT         NOT NULL DEFAULT 0 COMMENT '输入 token（供应商回报；未回报为 0，不估算）' AFTER `latency_ms`,
    ADD COLUMN `token_output` INT         NOT NULL DEFAULT 0 COMMENT '输出 token（供应商回报；未回报为 0，不估算）' AFTER `token_input`,
    ADD COLUMN `subject_id`   VARCHAR(36) NULL COMMENT '调用主体（如商品 ID，可空 —— 部分调用发生在主体创建之前）' AFTER `token_output`,
    ADD KEY `idx_ai_call_log_subject` (`subject_id`, `scope`);
