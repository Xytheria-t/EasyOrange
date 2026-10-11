-- KV 前缀缓存命中观测：命中输入 token 单独成列，报表才能出「命中率」这个可引用数字。
-- 口径与 token_input/token_output 一致：只认供应商回报（标准 prompt_tokens_details.cached_tokens 或
-- DeepSeek 非标 prompt_cache_hit_tokens），未回报记 0 不估算。
ALTER TABLE `eo_ai_call_log`
    ADD COLUMN `cache_read_tokens` INT NOT NULL DEFAULT 0 COMMENT '缓存命中输入 token（供应商回报；未回报为 0，不估算）' AFTER `token_output`;
