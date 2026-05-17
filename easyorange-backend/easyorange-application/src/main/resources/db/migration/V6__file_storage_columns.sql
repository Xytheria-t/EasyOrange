-- ===================================================================
-- File: V6__file_storage_columns.sql
-- Description: Add storage_type and storage_key columns to eo_upload_file
-- ===================================================================

ALTER TABLE `eo_upload_file`
    ADD COLUMN `storage_type` VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型（LOCAL/S3/OSS）' AFTER `md5`,
    ADD COLUMN `storage_key` VARCHAR(500) DEFAULT NULL COMMENT '存储后端标识键' AFTER `storage_type`;
