-- ===================================================================
-- EasyOrange AI 资产管理平台 - ID 字段类型迁移
-- Version: V5
-- 职责: 将所有主键/外键/引用 ID 从 BIGINT 改为 VARCHAR(36)
-- 背景: Snowflake → UUID v7（RFC 9562），类型从 Long → String
-- 说明: MySQL 会自动将现有 BIGINT 值转换为字符串（1 → '1'）
--       新增数据由 UuidV7IdGenerator 生成 UUID v7 格式字符串
-- Database: MySQL 8.0
-- ===================================================================

-- ===================================================================
-- 1. 用户模块
-- ===================================================================

ALTER TABLE `eo_user`
    MODIFY COLUMN `user_id`   VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

-- ===================================================================
-- 2. 商品模块
-- ===================================================================

ALTER TABLE `eo_category`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `parent_id` VARCHAR(36) NOT NULL DEFAULT '0' COMMENT '父分类 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_product`
    MODIFY COLUMN `id`          VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`     VARCHAR(36) NOT NULL COMMENT '发布者 ID',
    MODIFY COLUMN `category_id` VARCHAR(36) DEFAULT NULL COMMENT '分类 ID',
    MODIFY COLUMN `create_by`   VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`   VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_product_audit_log`
    MODIFY COLUMN `id`          VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `product_id`  VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `operator_id` VARCHAR(36) NOT NULL COMMENT '操作人 ID';

ALTER TABLE `eo_product_detail`
    MODIFY COLUMN `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_product_image`
    MODIFY COLUMN `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_product_review`
    MODIFY COLUMN `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `user_id`    VARCHAR(36) NOT NULL COMMENT '评价用户 ID',
    MODIFY COLUMN `order_id`   VARCHAR(36) NOT NULL COMMENT '关联订单 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_product_report`
    MODIFY COLUMN `id`          VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `product_id`  VARCHAR(36) NOT NULL COMMENT '被举报商品 ID',
    MODIFY COLUMN `reporter_id` VARCHAR(36) NOT NULL COMMENT '举报人 ID',
    MODIFY COLUMN `create_by`   VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`   VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_report_handle_history`
    MODIFY COLUMN `id`          VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `report_id`   VARCHAR(36) NOT NULL COMMENT '举报ID',
    MODIFY COLUMN `operator_id` VARCHAR(36) NOT NULL COMMENT '操作人ID',
    MODIFY COLUMN `create_by`   VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`   VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_favorite`
    MODIFY COLUMN `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`    VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者';

-- ===================================================================
-- 3. 搜索模块
-- ===================================================================

ALTER TABLE `eo_search_history`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`   VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_hot_keyword`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

-- ===================================================================
-- 4. 订单模块
-- ===================================================================

ALTER TABLE `eo_order`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `buyer_id`  VARCHAR(36) NOT NULL COMMENT '认领方 ID',
    MODIFY COLUMN `seller_id` VARCHAR(36) NOT NULL COMMENT '资产方 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_order_item`
    MODIFY COLUMN `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `order_id`   VARCHAR(36) NOT NULL COMMENT '订单 ID',
    MODIFY COLUMN `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者';

-- ===================================================================
-- 5. 支付模块
-- ===================================================================

ALTER TABLE `eo_payment`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `order_id`  VARCHAR(36) NOT NULL COMMENT '订单 ID',
    MODIFY COLUMN `user_id`   VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_payment_config`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

-- ===================================================================
-- 6. 消息模块
-- ===================================================================

ALTER TABLE `eo_message`
    MODIFY COLUMN `id`              VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `sender_id`       VARCHAR(36) DEFAULT NULL COMMENT '发送者 ID',
    MODIFY COLUMN `receiver_id`     VARCHAR(36) NOT NULL COMMENT '接收者 ID',
    MODIFY COLUMN `business_id`     VARCHAR(36) DEFAULT NULL COMMENT '业务 ID',
    MODIFY COLUMN `conversation_id` VARCHAR(36) DEFAULT NULL COMMENT '会话 ID',
    MODIFY COLUMN `create_by`       VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`       VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_message_archive`
    MODIFY COLUMN `id`              VARCHAR(36) NOT NULL COMMENT '消息 ID',
    MODIFY COLUMN `sender_id`       VARCHAR(36) DEFAULT NULL COMMENT '发送者 ID',
    MODIFY COLUMN `receiver_id`     VARCHAR(36) NOT NULL COMMENT '接收者 ID',
    MODIFY COLUMN `business_id`     VARCHAR(36) DEFAULT NULL COMMENT '业务 ID',
    MODIFY COLUMN `conversation_id` VARCHAR(36) DEFAULT NULL COMMENT '会话 ID',
    MODIFY COLUMN `create_by`       VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`       VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_message_subscription`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`   VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_message_template`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_offline_message`
    MODIFY COLUMN `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`    VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `message_id` VARCHAR(36) NOT NULL COMMENT '消息 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者';

-- ===================================================================
-- 7. 文件模块
-- ===================================================================
-- 注意：file_size 是文件大小（字节数），不是 ID 字段，保持 BIGINT
-- 注意：storage_key 是存储后端标识键（字符串），保持 VARCHAR

ALTER TABLE `eo_upload_file`
    MODIFY COLUMN `id`          VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `business_id` VARCHAR(36) DEFAULT NULL COMMENT '业务 ID',
    MODIFY COLUMN `uploader_id` VARCHAR(36) DEFAULT NULL COMMENT '上传者 ID',
    MODIFY COLUMN `create_by`   VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`   VARCHAR(36) DEFAULT NULL COMMENT '更新者';

-- ===================================================================
-- 8. 日志模块
-- ===================================================================
-- 注意：eo_oper_log 的主键名为 oper_id，不是 id

ALTER TABLE `eo_oper_log`
    MODIFY COLUMN `oper_id` VARCHAR(36) NOT NULL COMMENT '日志主键';

-- ===================================================================
-- 9. AI 功能模块
-- ===================================================================

ALTER TABLE `eo_product_question`
    MODIFY COLUMN `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `user_id`    VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_audit_suggestion`
    MODIFY COLUMN `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    MODIFY COLUMN `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '处理者';

-- ===================================================================
-- 10. 信用评分模块
-- ===================================================================

ALTER TABLE `eo_user_credit`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`   VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者';

ALTER TABLE `eo_credit_change_log`
    MODIFY COLUMN `id`           VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`      VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `reference_id` VARCHAR(36) DEFAULT NULL COMMENT '关联业务 ID（订单ID/举报ID等）',
    MODIFY COLUMN `create_by`    VARCHAR(36) DEFAULT NULL COMMENT '操作人/系统 ID';

-- ===================================================================
-- 11. 领域事件表
-- ===================================================================
-- 注意：event_id 已是 CHAR(36) UUID，保持不变

ALTER TABLE `eo_domain_event`
    MODIFY COLUMN `id`           VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `aggregate_id` VARCHAR(36) NOT NULL COMMENT '聚合 ID',
    MODIFY COLUMN `create_by`    VARCHAR(36) DEFAULT NULL COMMENT '创建人 ID',
    MODIFY COLUMN `update_by`    VARCHAR(36) DEFAULT NULL COMMENT '更新人 ID';

-- ===================================================================
-- 12. Saga 状态表 — 无 BIGINT 列，跳过
--    saga_id 已是 CHAR(36) UUID
-- ===================================================================

-- ===================================================================
-- 13. 幂等性键表
-- ===================================================================

ALTER TABLE `eo_idempotency_key`
    MODIFY COLUMN `id`        VARCHAR(36) NOT NULL COMMENT '主键 ID',
    MODIFY COLUMN `user_id`   VARCHAR(36) NOT NULL COMMENT '用户 ID',
    MODIFY COLUMN `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建人 ID',
    MODIFY COLUMN `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新人 ID';
