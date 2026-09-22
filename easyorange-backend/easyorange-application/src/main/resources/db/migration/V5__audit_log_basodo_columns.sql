-- ===================================================================
-- V5: 审计日志表补齐 BaseDO 四列（update_time / create_by / update_by / del_flag）
-- 背景: ProductAuditLogDO extends BaseDO，MetaObjectHandler 与 @TableLogic 会读写这四列，
--       但 V1 建表只有 create_time —— 新库跑「提交审核/审核」INSERT/SELECT 直接 500。
-- 幂等: MySQL 8 不支持 ADD COLUMN IF NOT EXISTS；逐列查 INFORMATION_SCHEMA 再决定
--       是否生成 ALTER（已补列的存量库四次全部跳过，可重复执行）。
-- 说明: eo_ai_call_log 经核对不缺列（11 列与 AiCallLogRecorder 手写 INSERT 一一对应，
--       其 WARN 真因是只读连接拒绝写入，与 schema 无关），故本迁移只动审计日志表。
-- Database: MySQL 8.0
-- ===================================================================

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `eo_product_audit_log` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'eo_product_audit_log'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `eo_product_audit_log` ADD COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT ''创建者''',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'eo_product_audit_log'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `eo_product_audit_log` ADD COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT ''更新者''',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'eo_product_audit_log'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `eo_product_audit_log` ADD COLUMN `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT ''删除标志（0 正常 1 删除）''',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'eo_product_audit_log'
      AND COLUMN_NAME = 'del_flag'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
