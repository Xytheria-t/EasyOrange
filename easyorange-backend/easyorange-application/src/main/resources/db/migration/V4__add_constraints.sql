-- ===================================================================
-- EasyOrange 校园二手交易平台 - CHECK 约束定义
-- Version: V4
-- 职责: 添加所有 CHECK 约束
-- 注意: MySQL 8.0.16+ 才支持 CHECK 约束
-- ===================================================================

-- ===================================================================
-- 1. 用户模块 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_user`
    ADD CONSTRAINT `chk_eo_user_status` CHECK (`status` IN (0, 1, 2)),
    ADD CONSTRAINT `chk_eo_user_sex` CHECK (`sex` IS NULL OR `sex` IN (0, 1, 2)),
    ADD CONSTRAINT `chk_eo_user_type` CHECK (`user_type` IN ('01', '02'));

-- ===================================================================
-- 2. 商品模块 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_category`
    ADD CONSTRAINT `chk_eo_category_status` CHECK (`status` IN (0, 1));

ALTER TABLE `eo_product`
    ADD CONSTRAINT `chk_eo_product_price` CHECK (`price` >= 0),
    ADD CONSTRAINT `chk_eo_product_original_price` CHECK (`original_price` IS NULL OR `original_price` >= 0),
    ADD CONSTRAINT `chk_eo_product_stock` CHECK (`stock` >= 0),
    ADD CONSTRAINT `chk_eo_product_status` CHECK (`status` IN (0, 1, 2, 3)),
    ADD CONSTRAINT `chk_eo_product_condition` CHECK (`condition_level` IS NULL OR (`condition_level` >= 1 AND `condition_level` <= 10)),
    ADD CONSTRAINT `chk_eo_product_view_count` CHECK (`view_count` >= 0);

ALTER TABLE `eo_product_report`
    ADD CONSTRAINT `chk_eo_product_report_status` CHECK (`status` IN (0, 1, 2));

-- ===================================================================
-- 3. 搜索模块 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_hot_keyword`
    ADD CONSTRAINT `chk_eo_hot_keyword_count` CHECK (`search_count` >= 0);

-- ===================================================================
-- 4. 订单模块 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_order`
    ADD CONSTRAINT `chk_eo_order_amount` CHECK (`amount` >= 0),
    ADD CONSTRAINT `chk_eo_order_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5)),
    ADD CONSTRAINT `chk_eo_order_payment_status` CHECK (`payment_status` IN (0, 1, 2));

-- ===================================================================
-- 5. 支付模块 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_payment`
    ADD CONSTRAINT `chk_eo_payment_amount` CHECK (`amount` >= 0),
    ADD CONSTRAINT `chk_eo_payment_refunded_amount` CHECK (`refunded_amount` >= 0),
    ADD CONSTRAINT `chk_eo_payment_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5, 6, 7)),
    ADD CONSTRAINT `chk_eo_payment_method` CHECK (`payment_method` IS NULL OR `payment_method` IN (1, 2, 3));

-- ===================================================================
-- 6. 消息模块 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_message`
    ADD CONSTRAINT `chk_eo_message_is_read` CHECK (`is_read` IN (0, 1));

ALTER TABLE `eo_offline_message`
    ADD CONSTRAINT `chk_eo_offline_message_push_status` CHECK (`push_status` IN (0, 1, 2));

-- ===================================================================
-- 7. 日志模块 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_oper_log`
    ADD CONSTRAINT `chk_eo_oper_log_status` CHECK (`status` IN (0, 1));

-- ===================================================================
-- 8. 领域事件表 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_domain_event`
    ADD CONSTRAINT `chk_eo_domain_event_status` CHECK (`status` IN ('PENDING', 'PUBLISHED', 'FAILED'));

-- ===================================================================
-- 9. 幂等性键表 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_idempotency_key`
    ADD CONSTRAINT `chk_eo_idempotency_key_status` CHECK (`status` IN ('PENDING', 'COMPLETED', 'FAILED'));

-- ===================================================================
-- 10. Saga 分布式事务状态表 - CHECK 约束
-- ===================================================================

ALTER TABLE `eo_saga_status`
    ADD CONSTRAINT `chk_eo_saga_status_state` CHECK (
        `state` IN ('STARTED', 'ORDER_CREATED', 'PAYMENT_CREATED', 'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'FAILED')
    );
