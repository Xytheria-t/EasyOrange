-- ===================================================================
-- V2: Agent 多步循环步级轨迹表
-- 职责: sprint W1（单步 ReAct → 多步工具循环）每轮决策落一行（含 finish 轮），
--       作为平均步数 / 降级率 / 步级延迟 p95 三个口径的数据源。
--       一次对话请求共用一个 trace_id，COUNT(*) GROUP BY trace_id 即该请求的决策轮数。
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- ===================================================================

CREATE TABLE `eo_agent_step_trace` (
    `id`          VARCHAR(36)  NOT NULL COMMENT '主键 UUID v7',
    `trace_id`    VARCHAR(36)  NOT NULL COMMENT '循环轨迹 ID（一次对话请求一个）',
    `session_id`  VARCHAR(64)  NOT NULL COMMENT '会话 ID',
    `user_id`     VARCHAR(36)  NULL COMMENT '用户 ID（匿名对话为空）',
    `step_index`  INT          NOT NULL COMMENT '步序（1 起，含 finish 轮）',
    `tool`        VARCHAR(32)  NOT NULL COMMENT '工具（knowledge_search/product_search/product_detail/finish）',
    `tool_input`  VARCHAR(512) NULL COMMENT '工具入参（检索词 / 资产 ID）',
    `thought`     VARCHAR(255) NULL COMMENT '模型决策理由（步骤可视化文案）',
    `observation` VARCHAR(512) NULL COMMENT '观察摘要（命中数 / 命中标题 / 详情摘要）',
    `latency_ms`  BIGINT       NOT NULL DEFAULT 0 COMMENT '该步耗时毫秒（finish 轮为 0）',
    `success`     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '工具是否执行成功 1/0',
    `error_msg`   VARCHAR(512) NULL COMMENT '失败原因',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_agent_step_trace_trace` (`trace_id`),
    KEY `idx_agent_step_trace_tool` (`tool`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Agent 多步循环步级轨迹（平均步数 / 降级率 / 步级延迟 p95 数据源）';
