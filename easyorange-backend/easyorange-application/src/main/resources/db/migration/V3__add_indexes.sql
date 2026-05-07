-- ===================================================================
-- EasyOrange 校园二手交易平台 - 索引定义
-- Version: V3
-- 职责: 添加所有索引（UNIQUE/KEY/FULLTEXT）
-- 注意: FULLTEXT 索引使用 ngram 分词器，需要 MySQL 8.0 配置 ngram_token_size
-- ===================================================================

-- ===================================================================
-- 1. 用户模块 - 索引
-- ===================================================================

ALTER TABLE `eo_user`
    ADD UNIQUE INDEX `uk_eo_user_username` (`username`),
    ADD UNIQUE INDEX `uk_eo_user_email` (`email`),
    ADD UNIQUE INDEX `uk_eo_user_phone` (`phonenumber`),
    ADD UNIQUE INDEX `uk_eo_user_student_id` (`student_id`),
    ADD INDEX `idx_eo_user_status_del` (`status`, `del_flag`, `create_time` DESC),
    ADD INDEX `idx_eo_user_type_status` (`user_type`, `status`, `del_flag`),
    ADD INDEX `idx_eo_user_create_time` (`create_time`);

-- ===================================================================
-- 2. 商品模块 - 索引
-- ===================================================================

ALTER TABLE `eo_category`
    ADD INDEX `idx_eo_category_parent_id` (`parent_id`),
    ADD INDEX `idx_eo_category_status_sort` (`status`, `del_flag`, `sort_order`);

ALTER TABLE `eo_product`
    ADD INDEX `idx_eo_product_user_time` (`user_id`, `create_time` DESC),
    ADD INDEX `idx_eo_product_category_status_time` (`category_id`, `status`, `create_time` DESC),
    ADD INDEX `idx_eo_product_status_price` (`status`, `price`),
    ADD INDEX `idx_eo_product_search` (`status`, `del_flag`, `category_id`, `create_time` DESC),
    ADD INDEX `idx_eo_product_status_del_price` (`status`, `del_flag`, `price`),
    ADD INDEX `idx_eo_product_user_status_del` (`user_id`, `status`, `del_flag`, `create_time` DESC);

-- FULLTEXT 索引需要单独创建（InnoDB 不支持同时创建多个 FULLTEXT 索引）
ALTER TABLE `eo_product` ADD FULLTEXT INDEX `ft_eo_product_name` (`name`) WITH PARSER ngram;
ALTER TABLE `eo_product` ADD FULLTEXT INDEX `ft_eo_product_search_text` (`search_text`) WITH PARSER ngram;

ALTER TABLE `eo_product_image`
    ADD INDEX `idx_eo_product_image_product_sort` (`product_id`, `sort_order`);

ALTER TABLE `eo_product_report`
    ADD INDEX `idx_eo_product_report_product_id` (`product_id`),
    ADD INDEX `idx_eo_product_report_reporter_id` (`reporter_id`),
    ADD INDEX `idx_eo_product_report_status_time` (`status`, `create_time` DESC);

ALTER TABLE `eo_favorite`
    ADD UNIQUE INDEX `uk_eo_favorite_user_product` (`user_id`, `product_id`),
    ADD INDEX `idx_eo_favorite_user_time` (`user_id`, `create_time` DESC),
    ADD INDEX `idx_eo_favorite_product_count` (`product_id`, `del_flag`);

-- ===================================================================
-- 3. 搜索模块 - 索引
-- ===================================================================

ALTER TABLE `eo_search_history`
    ADD UNIQUE INDEX `uk_eo_search_history_user_keyword` (`user_id`, `keyword`),
    ADD INDEX `idx_eo_search_history_user_time` (`user_id`, `search_time` DESC),
    ADD INDEX `idx_eo_search_history_keyword` (`keyword`);

ALTER TABLE `eo_hot_keyword`
    ADD UNIQUE INDEX `uk_eo_hot_keyword_keyword` (`keyword`),
    ADD INDEX `idx_eo_hot_keyword_count` (`search_count` DESC),
    ADD INDEX `idx_eo_hot_keyword_last_time` (`last_search_time`);

-- ===================================================================
-- 4. 订单模块 - 索引
-- ===================================================================

ALTER TABLE `eo_order`
    ADD UNIQUE INDEX `uk_eo_order_order_no` (`order_no`),
    ADD INDEX `idx_eo_order_buyer_status_time` (`buyer_id`, `status`, `del_flag`, `create_time` DESC),
    ADD INDEX `idx_eo_order_seller_status_time` (`seller_id`, `status`, `del_flag`, `create_time` DESC),
    ADD INDEX `idx_eo_order_product_id` (`product_id`),
    ADD INDEX `idx_eo_order_payment_status` (`payment_status`),
    ADD INDEX `idx_eo_order_status_payment` (`status`, `payment_status`, `create_time` DESC);

-- ===================================================================
-- 5. 支付模块 - 索引
-- ===================================================================

ALTER TABLE `eo_payment`
    ADD UNIQUE INDEX `uk_eo_payment_payment_no` (`payment_no`),
    ADD UNIQUE INDEX `uk_eo_payment_transaction_id` (`transaction_id`),
    ADD INDEX `idx_eo_payment_order_id` (`order_id`),
    ADD INDEX `idx_eo_payment_user_time` (`user_id`, `create_time` DESC),
    ADD INDEX `idx_eo_payment_status_method` (`status`, `payment_method`, `create_time` DESC),
    ADD INDEX `idx_eo_payment_user_status` (`user_id`, `status`, `create_time` DESC);

ALTER TABLE `eo_payment_config`
    ADD UNIQUE INDEX `uk_eo_payment_config_channel` (`channel_code`);

-- ===================================================================
-- 6. 消息模块 - 索引
-- ===================================================================

ALTER TABLE `eo_message`
    ADD INDEX `idx_eo_message_sender_time` (`sender_id`, `create_time` DESC),
    ADD INDEX `idx_eo_message_receiver_read_time` (`receiver_id`, `is_read`, `create_time` DESC),
    ADD INDEX `idx_eo_message_conversation_time` (`conversation_id`, `create_time` DESC),
    ADD INDEX `idx_eo_message_business_id` (`business_id`);

ALTER TABLE `eo_message_archive`
    ADD INDEX `idx_eo_message_archive_receiver` (`receiver_id`),
    ADD INDEX `idx_eo_message_archive_time` (`archived_at`);

ALTER TABLE `eo_message_subscription`
    ADD UNIQUE INDEX `uk_eo_message_subscription_user_type_channel` (`user_id`, `message_type`, `push_channel`),
    ADD INDEX `idx_eo_message_subscription_user` (`user_id`);

ALTER TABLE `eo_message_template`
    ADD UNIQUE INDEX `uk_eo_message_template_code` (`template_code`),
    ADD INDEX `idx_eo_message_template_type` (`template_type`);

ALTER TABLE `eo_offline_message`
    ADD INDEX `idx_eo_offline_message_user_status` (`user_id`, `push_status`),
    ADD INDEX `idx_eo_offline_message_message_id` (`message_id`),
    ADD INDEX `idx_eo_offline_message_retry` (`push_status`, `retry_count`, `create_time` DESC);

-- ===================================================================
-- 7. 文件模块 - 索引
-- ===================================================================

ALTER TABLE `eo_upload_file`
    ADD INDEX `idx_eo_upload_file_md5` (`md5`),
    ADD INDEX `idx_eo_upload_file_business` (`business_type`, `business_id`),
    ADD INDEX `idx_eo_upload_file_uploader` (`uploader_id`);

-- ===================================================================
-- 8. 日志模块 - 索引
-- ===================================================================

ALTER TABLE `eo_oper_log`
    ADD INDEX `idx_eo_oper_log_time` (`oper_time`),
    ADD INDEX `idx_eo_oper_log_name_time` (`oper_name`, `oper_time` DESC),
    ADD INDEX `idx_eo_oper_log_business_time` (`business_type`, `oper_time` DESC),
    ADD INDEX `idx_eo_oper_log_status_time` (`status`, `oper_time` DESC);

ALTER TABLE `eo_oper_log_archive`
    ADD INDEX `idx_eo_oper_log_archive_time` (`oper_time`),
    ADD INDEX `idx_eo_oper_log_archive_name` (`oper_name`),
    ADD INDEX `idx_eo_oper_log_archive_archived_at` (`archived_at`);

-- ===================================================================
-- 9. 领域事件表 - 索引
-- ===================================================================

ALTER TABLE `eo_domain_event`
    ADD UNIQUE INDEX `uk_eo_domain_event_event_id` (`event_id`),
    ADD INDEX `idx_eo_domain_event_aggregate` (`aggregate_type`, `aggregate_id`),
    ADD INDEX `idx_eo_domain_event_status_created` (`status`, `created_at`),
    ADD INDEX `idx_eo_domain_event_event_type` (`event_type`);

-- ===================================================================
-- 10. 幂等性键表 - 索引
-- ===================================================================

ALTER TABLE `eo_idempotency_key`
    ADD UNIQUE INDEX `uk_eo_idempotency_key_key` (`idempotency_key`),
    ADD INDEX `idx_eo_idempotency_key_user_expires` (`user_id`, `expires_at`);

-- ===================================================================
-- 11. Saga 分布式事务状态表 - 索引
-- ===================================================================

ALTER TABLE `eo_saga_status`
    ADD INDEX `idx_eo_saga_status_type_state` (`saga_type`, `state`),
    ADD INDEX `idx_eo_saga_status_state_created` (`state`, `created_at`),
    ADD INDEX `idx_eo_saga_status_created_at` (`created_at`);
