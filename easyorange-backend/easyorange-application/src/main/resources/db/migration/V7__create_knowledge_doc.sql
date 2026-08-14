-- ===================================================================
-- EasyOrange - RAG 知识库文档表
-- Description: 文档摄入管线入口（解析 → 分块 → embed → ES 索引），
--              status 记录索引状态，chunk_count 记录分块数（摄入后回填）
-- ===================================================================

CREATE TABLE IF NOT EXISTS `eo_knowledge_doc` (
    `id`          VARCHAR(36)  NOT NULL COMMENT '主键 UUID v7',
    `title`       VARCHAR(200) NOT NULL COMMENT '文档标题',
    `content`     LONGTEXT     NOT NULL COMMENT '文档正文（markdown/纯文本）',
    `source`      VARCHAR(64)  NULL COMMENT '来源（运营/规则/商品详情等）',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '索引状态 PENDING/INDEXED/FAILED',
    `chunk_count` INT          NOT NULL DEFAULT 0 COMMENT '分块数量（索引后回填）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(36)  NULL COMMENT '创建人',
    `update_by`   VARCHAR(36)  NULL COMMENT '更新人',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0/1',
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_doc_status` (`status`, `del_flag`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'RAG 知识库文档（摄入后分块进 ES，支持启动补索引）';
