-- ===================================================================
-- EasyOrange - 支付配置种子数据
-- 说明：此文件包含系统启动所需的支付渠道配置
-- 用途：通过 Spring Boot data.sql 或手动执行加载
-- ===================================================================

INSERT INTO `eo_payment_config` (
    `id`, `channel_code`, `channel_name`, `app_id`, `sandbox`, `status`, `remark`, `create_time`, `update_time`
) VALUES
(1, 'wechat', '微信支付', 'wx_dev_test_app_id',    1, 1, '微信支付沙箱环境', NOW(), NOW()),
(2, 'alipay', '支付宝',   'alipay_dev_test_app_id', 1, 1, '支付宝沙箱环境',   NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `channel_name` = VALUES(`channel_name`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`),
    `update_time` = NOW();
