-- ===================================================================
-- EasyOrange - 支付渠道种子数据
-- Description: Repeatable Migration - 支付渠道配置
-- Type: DML（可重复执行，ON DUPLICATE KEY UPDATE 保证幂等）
-- 注意：生产环境部署前应替换沙箱 app_id 为真实值
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