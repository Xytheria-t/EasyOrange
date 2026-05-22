-- ===================================================================
-- EasyOrange 校园二手交易平台 - 支付渠道种子数据
-- Description: Repeatable Migration - 支付渠道配置
-- Type: DML（可重复执行）
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- 设计说明:
--   使用 ON DUPLICATE KEY UPDATE 保证幂等性
--   生产环境部署前应替换沙箱 app_id 为真实值
--   密钥存储在数据库中，生产环境建议使用密钥管理服务 (Vault/KMS)
-- ===================================================================

START TRANSACTION;

INSERT INTO `eo_payment_config` (
    `id`, `channel_code`, `channel_name`, `app_id`, `sandbox`, `status`, `remark`, `create_time`, `update_time`
) VALUES
(1, 'wechat', '微信支付', 'wx_dev_test_app_id',    1, 1, '微信支付沙箱环境', NOW(), NOW()),
(2, 'alipay', '支付宝',   'alipay_dev_test_app_id', 1, 1, '支付宝沙箱环境',   NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `channel_name` = new.`channel_name`,
    `status` = new.`status`,
    `remark` = new.`remark`,
    `update_time` = NOW();

COMMIT;