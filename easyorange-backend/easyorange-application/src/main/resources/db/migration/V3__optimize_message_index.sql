-- ===================================================================
-- EasyOrange 校园二手交易平台 - 消息表索引优化
-- Version: V3
-- 职责: 优化消息表索引，添加 del_flag 字段
-- ===================================================================

-- 删除旧索引
ALTER TABLE `eo_message` DROP INDEX `idx_eo_message_receiver_read_time`;

-- 创建新索引（包含 del_flag）
ALTER TABLE `eo_message` 
    ADD INDEX `idx_eo_message_receiver_read_time` (`receiver_id`, `is_read`, `del_flag`, `create_time` DESC);
