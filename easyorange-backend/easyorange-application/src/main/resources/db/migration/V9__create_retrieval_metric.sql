-- ===================================================================
-- EasyOrange - RAG 检索指标采样表
-- Description: 金标准集检索回归 — 每次采样记录 hit@5 / MRR 分量，
--              按 run_id 聚合即可算出整批命中率与平均倒数排名
-- ===================================================================

CREATE TABLE IF NOT EXISTS `eo_retrieval_metric` (
    `id`              VARCHAR(36)  NOT NULL COMMENT '主键 UUID v7',
    `run_id`          VARCHAR(36)  NOT NULL COMMENT '评测批次 ID',
    `case_id`         VARCHAR(64)  NOT NULL COMMENT '金标准用例 ID',
    `query_text`      TEXT         NULL COMMENT '检索查询',
    `gold_doc_ids`    VARCHAR(255) NULL COMMENT '期望命中文档 ID（逗号分隔）',
    `hit_at_5`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'top-5 是否命中期望文档 1/0',
    `reciprocal_rank` DECIMAL(6,4) NOT NULL DEFAULT 0 COMMENT '首个命中位置的倒数（MRR 分量，未命中为 0）',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_retrieval_metric_run` (`run_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'RAG 检索指标采样（hit@5 / MRR，金标准集回归数据源）';
