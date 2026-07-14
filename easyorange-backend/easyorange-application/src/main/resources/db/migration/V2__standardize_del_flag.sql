-- ===================================================================
-- V2: 标准化逻辑删除标志位 del_flag
-- 职责: 将 del_flag 的取值范围从 {0, 2} 迁移为 {0, 1}
-- 原因: delval="2" 跳过了常规的 1，创造了一个未定义的"预留"状态。
--       0/1 是社区事实标准，无歧义，更简洁。
-- Database: MySQL 8.0
-- 2026-07-14
-- ===================================================================

-- 将所有 del_flag = 2 的记录更新为 del_flag = 1
-- 这是数据层面的行为一致性迁移（无业务语义变化）

UPDATE eo_user              SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_category          SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_product           SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_product_image     SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_product_detail    SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_product_audit_log SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_product_review    SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_product_report    SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_report_handle_history SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_upload_file       SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_favorite          SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_search_history    SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_hot_keyword       SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_message           SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_message_template  SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_offline_message   SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_message_subscription SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_credit_score      SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_order             SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_order_item        SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_payment           SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_payment_config    SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_idempotency_key   SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_user_credit       SET del_flag = 1 WHERE del_flag = 2;
UPDATE eo_audit_log         SET del_flag = 1 WHERE del_flag = 2;
