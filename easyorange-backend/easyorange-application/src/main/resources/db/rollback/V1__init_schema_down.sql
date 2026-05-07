-- ===================================================================
-- EasyOrange V1 回滚脚本 - 删除所有表
-- 执行顺序：依赖表先删，被依赖表后删
-- ===================================================================

DROP TABLE IF EXISTS `eo_idempotency_key`;
DROP TABLE IF EXISTS `eo_saga_status`;
DROP TABLE IF EXISTS `eo_domain_event`;
DROP TABLE IF EXISTS `eo_oper_log_archive`;
DROP TABLE IF EXISTS `eo_oper_log`;
DROP TABLE IF EXISTS `eo_upload_file`;
DROP TABLE IF EXISTS `eo_offline_message`;
DROP TABLE IF EXISTS `eo_message_template`;
DROP TABLE IF EXISTS `eo_message_subscription`;
DROP TABLE IF EXISTS `eo_message_archive`;
DROP TABLE IF EXISTS `eo_message`;
DROP TABLE IF EXISTS `eo_payment_config`;
DROP TABLE IF EXISTS `eo_payment`;
DROP TABLE IF EXISTS `eo_order`;
DROP TABLE IF EXISTS `eo_hot_keyword`;
DROP TABLE IF EXISTS `eo_search_history`;
DROP TABLE IF EXISTS `eo_favorite`;
DROP TABLE IF EXISTS `eo_product_report`;
DROP TABLE IF EXISTS `eo_product_image`;
DROP TABLE IF EXISTS `eo_product_detail`;
DROP TABLE IF EXISTS `eo_product`;
DROP TABLE IF EXISTS `eo_category`;
DROP TABLE IF EXISTS `eo_user`;
