-- ===================================================================
-- EasyOrange 校园二手交易平台 - 数据库初始化脚本
-- 数据库: MySQL 8.x
-- 数据库名: easyorange
-- 创建日期: 2026-04-01
-- ===================================================================
-- ⚠️ 部署前需在 MySQL 配置 (my.cnf/my.ini) 中添加:
--    ngram_token_size = 2
--    用于优化 FULLTEXT 中文搜索精度，减少索引体积
-- ===================================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `easyorange`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `easyorange`;

-- ===================================================================
-- 用户模块表 (sys_user)
-- ===================================================================
-- 说明：存储平台用户的基本信息，包括注册账号、联系方式、校园身份等
-- ===================================================================

DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
    `user_id`         BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID，主键自增',
    `username`       VARCHAR(30)     NOT NULL                 COMMENT '用户名，唯一标识',
    `password`        VARCHAR(100)    NOT NULL                 COMMENT '密码（BCrypt加密存储）',
    `user_type`       VARCHAR(2)      DEFAULT '01'             COMMENT '用户类型（01=普通用户）',
    `email`           VARCHAR(50)     DEFAULT NULL             COMMENT '邮箱地址，唯一',
    `phonenumber`     VARCHAR(11)     DEFAULT NULL             COMMENT '手机号，唯一',
    `student_id`      VARCHAR(20)     DEFAULT NULL             COMMENT '学号，校园身份标识',
    `real_name`       VARCHAR(20)     DEFAULT NULL             COMMENT '真实姓名',
    `nick_name`       VARCHAR(30)     DEFAULT NULL             COMMENT '昵称',
    `avatar`          VARCHAR(500)    DEFAULT NULL             COMMENT '头像URL',
    `sex`             CHAR(1)         DEFAULT '2'              COMMENT '性别（0=女 1=男 2=未知）',
    `status`          CHAR(1)         DEFAULT '0'              COMMENT '账户状态（0=正常 1=禁用 2=锁定）',
    `del_flag`        INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `login_ip`        VARCHAR(128)    DEFAULT NULL             COMMENT '最后登录IP地址',
    `login_date`      DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
    `pwd_update_date` DATETIME        DEFAULT NULL             COMMENT '密码最后更新时间',
    `create_by`       BIGINT          DEFAULT 0                COMMENT '创建者ID',
    `create_time`     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT          DEFAULT 0                COMMENT '最后更新者ID',
    `update_time`     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `remark`          VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `version`         INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phonenumber` (`phonenumber`),
    UNIQUE KEY `uk_student_id` (`student_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `chk_user_status` CHECK (`status` IN ('0', '1', '2')),
    CONSTRAINT `chk_user_sex` CHECK (`sex` IS NULL OR `sex` IN ('0', '1', '2'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- ===================================================================
-- EasyOrange 校园二手交易平台 - 完整数据库建表脚本
-- 版本: V2 (优化版)
-- 迁移日期: 2026-04-01
-- 说明: 包含所有业务表 + 优化索引 + 增强字段
-- ===================================================================

USE `easyorange`;

-- 禁用外键检查，避免已有表外键约束阻断 DROP TABLE
SET FOREIGN_KEY_CHECKS = 0;

-- 先删依赖其他表的子表，再删父表
DROP TABLE IF EXISTS `product_detail`;
DROP TABLE IF EXISTS `eo_order`;
DROP TABLE IF EXISTS `eo_payment`;
DROP TABLE IF EXISTS `eo_payment_config`;
DROP TABLE IF EXISTS `eo_message_archive`;
DROP TABLE IF EXISTS `eo_message`;
DROP TABLE IF EXISTS `eo_message_template`;
DROP TABLE IF EXISTS `eo_message_subscription`;
DROP TABLE IF EXISTS `eo_offline_message`;
DROP TABLE IF EXISTS `product_image`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `search_history`;
DROP TABLE IF EXISTS `search_history_archive`;
DROP TABLE IF EXISTS `hot_keyword`;
DROP TABLE IF EXISTS `upload_file`;
DROP TABLE IF EXISTS `sys_oper_log`;
DROP TABLE IF EXISTS `sys_oper_log_archive`;

-- ===================================================================
-- 1. 商品信息表 (product)
-- ===================================================================
CREATE TABLE `product` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL                 COMMENT '发布者ID',
    `category_id`       BIGINT          DEFAULT NULL             COMMENT '分类ID',
    `name`              VARCHAR(100)    NOT NULL                 COMMENT '商品名称',
    `search_text`       TEXT            DEFAULT NULL             COMMENT '全文搜索冗余字段（name + description前200字符）',
    `price`             DECIMAL(10,2)   NOT NULL                 COMMENT '售价',
    `price_update_time` DATETIME        DEFAULT NULL             COMMENT '价格最后更新时间',
    `original_price`    DECIMAL(10,2)   DEFAULT NULL             COMMENT '原价',
    `stock`             INT             DEFAULT 1                COMMENT '库存',
    `status`            INT             DEFAULT 0                COMMENT '状态（0=草稿 1=上架 2=已售 3=下架）',
    `view_count`        INT             DEFAULT 0                COMMENT '浏览次数',
    `condition_level`   INT             DEFAULT NULL             COMMENT '成色等级（1-10）',
    `location`          VARCHAR(100)    DEFAULT NULL             COMMENT '交易地点',
    `tags`              VARCHAR(200)    DEFAULT NULL             COMMENT '商品标签（逗号分隔）',
    `contact_method`    VARCHAR(50)     DEFAULT NULL             COMMENT '联系方式',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_create_time` (`user_id`, `create_time` DESC),
    KEY `idx_category_status_time` (`category_id`, `status`, `create_time` DESC),
    KEY `idx_condition_status` (`condition_level`, `status`),
    KEY `idx_status_price` (`status`, `price`),
    KEY `idx_status_del_flag_time` (`status`, `del_flag`, `create_time` DESC),
    KEY `idx_user_id_del_flag_time` (`user_id`, `del_flag`, `create_time` DESC),
    KEY `idx_user_status_create` (`user_id`, `status`, `create_time` DESC),
    FULLTEXT INDEX `ft_name` (`name`) WITH PARSER ngram COMMENT '商品名称全文搜索',
    FULLTEXT INDEX `ft_search_text` (`search_text`) WITH PARSER ngram COMMENT '全文搜索冗余字段索引',
    CONSTRAINT `chk_product_price` CHECK (`price` >= 0),
    CONSTRAINT `chk_product_original_price` CHECK (`original_price` IS NULL OR `original_price` >= 0),
    CONSTRAINT `chk_product_stock` CHECK (`stock` >= 0),
    CONSTRAINT `chk_product_status` CHECK (`status` IN (0, 1, 2, 3)),
    CONSTRAINT `chk_product_condition` CHECK (`condition_level` IS NULL OR (`condition_level` >= 1 AND `condition_level` <= 10)),
    CONSTRAINT `chk_product_view_count` CHECK (`view_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品信息表';

-- ===================================================================
-- 2. 商品详情表 (product_detail) - 垂直分表
-- ===================================================================
CREATE TABLE `product_detail` (
    `product_id`    BIGINT          NOT NULL COMMENT '关联商品ID',
    `description`   TEXT            DEFAULT NULL COMMENT '商品详细描述',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`product_id`),
    CONSTRAINT `fk_detail_product` FOREIGN KEY (`product_id`)
        REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品详情表（垂直分表）';

-- ===================================================================
-- 3. 商品图片表 (product_image)
-- ===================================================================
CREATE TABLE `product_image` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `product_id`    BIGINT          NOT NULL                 COMMENT '商品ID',
    `image_url`     VARCHAR(500)    NOT NULL                 COMMENT '图片URL',
    `sort_order`    INT             DEFAULT 0                COMMENT '排序序号',
    `is_main`       INT             DEFAULT 0                COMMENT '是否主图（0=否 1=是）',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_product_id_sort` (`product_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片表';

-- ===================================================================
-- 4. 分类表 (category)
-- ===================================================================
CREATE TABLE `category` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `name`          VARCHAR(50)     NOT NULL                 COMMENT '分类名称',
    `parent_id`     BIGINT          DEFAULT 0                COMMENT '父分类ID（0=顶级分类）',
    `level`         INT             DEFAULT 1                COMMENT '层级（1=一级 2=二级）',
    `icon`          VARCHAR(200)    DEFAULT NULL             COMMENT '图标URL',
    `sort_order`    INT             DEFAULT 0                COMMENT '排序序号',
    `status`        INT             DEFAULT 1                COMMENT '状态（0=禁用 1=启用）',
    `del_flag`      INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`     BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`       INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status_del_flag_sort` (`status`, `del_flag`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ===================================================================
-- 5. 搜索历史表 (search_history)
-- ===================================================================
CREATE TABLE `search_history` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `user_id`       BIGINT          NOT NULL                 COMMENT '用户ID',
    `keyword`       VARCHAR(100)    NOT NULL                 COMMENT '搜索关键词',
    `search_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    `del_flag`      INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_keyword` (`user_id`, `keyword`),
    KEY `idx_user_time` (`user_id`, `search_time` DESC),
    KEY `idx_del_flag_search_time` (`del_flag`, `search_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史表';

-- ===================================================================
-- 6. 热门关键词表 (hot_keyword)
-- ===================================================================
CREATE TABLE `hot_keyword` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `keyword`           VARCHAR(100)    NOT NULL                 COMMENT '关键词',
    `search_count`      INT             DEFAULT 0                COMMENT '搜索次数',
    `last_search_time`  DATETIME        DEFAULT NULL             COMMENT '最后搜索时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword` (`keyword`),
    KEY `idx_search_count` (`search_count` DESC),
    KEY `idx_last_search_time` (`last_search_time`),
    CONSTRAINT `chk_hot_keyword_count` CHECK (`search_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热门关键词表';

-- ===================================================================
-- 7. 订单表 (eo_order)
-- ===================================================================
CREATE TABLE `eo_order` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `order_no`          VARCHAR(32)     NOT NULL                 COMMENT '订单号',
    `buyer_id`          BIGINT          NOT NULL                 COMMENT '买家ID',
    `seller_id`         BIGINT          NOT NULL                 COMMENT '卖家ID',
    `product_id`        BIGINT          NOT NULL                 COMMENT '商品ID',
    `amount`            DECIMAL(10,2)   NOT NULL                 COMMENT '订单金额',
    `status`            INT             DEFAULT 0                COMMENT '订单状态（0=待付款 1=待发货 2=待收货 3=已完成 4=已取消 5=已退款）',
    `payment_status`    INT             DEFAULT 0                COMMENT '支付状态（0=未支付 1=已支付 2=已退款）',
    `address`           VARCHAR(200)    DEFAULT NULL             COMMENT '收货地址',
    `phone`             VARCHAR(20)     DEFAULT NULL             COMMENT '联系电话',
    `remark`            VARCHAR(500)    DEFAULT NULL             COMMENT '订单备注',
    `cancel_reason`     VARCHAR(200)    DEFAULT NULL             COMMENT '取消原因',
    `cancel_time`       DATETIME        DEFAULT NULL             COMMENT '取消时间',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_payment_status` (`payment_status`),
    KEY `idx_buyer_id_create_time` (`buyer_id`, `create_time` DESC),
    KEY `idx_seller_id_create_time` (`seller_id`, `create_time` DESC),
    KEY `idx_buyer_status_time` (`buyer_id`, `status`, `create_time` DESC),
    KEY `idx_seller_status_time` (`seller_id`, `status`, `create_time` DESC),
    KEY `idx_status_create_time` (`status`, `create_time` DESC),
    KEY `idx_product_id_status` (`product_id`, `status`),
    CONSTRAINT `chk_order_amount` CHECK (`amount` >= 0),
    CONSTRAINT `chk_order_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5)),
    CONSTRAINT `chk_order_payment_status` CHECK (`payment_status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ===================================================================
-- 8. 支付记录表 (eo_payment)
-- ===================================================================
CREATE TABLE `eo_payment` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `payment_no`        VARCHAR(32)     NOT NULL                 COMMENT '支付流水号',
    `order_id`          BIGINT          NOT NULL                 COMMENT '订单ID',
    `user_id`           BIGINT          NOT NULL                 COMMENT '用户ID',
    `amount`            DECIMAL(10,2)   NOT NULL                 COMMENT '支付金额',
    `payment_method`    INT             DEFAULT NULL             COMMENT '支付方式（1=微信 2=支付宝 3=余额）',
    `status`            INT             DEFAULT 0                COMMENT '支付状态（0=待支付 1=支付中 2=已支付 3=已退款 4=已关闭）',
    `transaction_id`    VARCHAR(64)     DEFAULT NULL             COMMENT '第三方支付交易号',
    `refund_reason`     VARCHAR(200)    DEFAULT NULL             COMMENT '退款原因',
    `refund_time`       DATETIME        DEFAULT NULL             COMMENT '退款时间',
    `attach`            TEXT            DEFAULT NULL             COMMENT '附加数据（JSON）',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    UNIQUE KEY `uk_transaction_id` (`transaction_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_id_create_time` (`user_id`, `create_time` DESC),
    KEY `idx_status` (`status`),
    KEY `idx_order_id_status` (`order_id`, `status`),
    KEY `idx_status_create_time` (`status`, `create_time` DESC),
    CONSTRAINT `chk_payment_amount` CHECK (`amount` >= 0),
    CONSTRAINT `chk_payment_status` CHECK (`status` IN (0, 1, 2, 3, 4)),
    CONSTRAINT `chk_payment_method` CHECK (`payment_method` IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ===================================================================
-- 9. 支付渠道配置表 (eo_payment_config)
-- ===================================================================
CREATE TABLE `eo_payment_config` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `channel_code`      VARCHAR(32)     NOT NULL                 COMMENT '渠道编码（alipay/wechat）',
    `channel_name`      VARCHAR(50)     NOT NULL                 COMMENT '渠道名称',
    `app_id`            VARCHAR(64)     DEFAULT NULL             COMMENT '应用ID',
    `private_key`       TEXT            DEFAULT NULL             COMMENT '商户私钥',
    `public_key`        TEXT            DEFAULT NULL             COMMENT '商户公钥',
    `sandbox`           TINYINT(1)      DEFAULT 0                COMMENT '是否沙箱环境',
    `status`            INT             DEFAULT 1                COMMENT '状态（0=禁用 1=启用）',
    `remark`            VARCHAR(200)    DEFAULT NULL             COMMENT '备注',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付渠道配置表';

-- ===================================================================
-- 10. 消息表 (eo_message)
-- ===================================================================
CREATE TABLE `eo_message` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `sender_id`         BIGINT          DEFAULT NULL             COMMENT '发送者ID（NULL=系统消息）',
    `receiver_id`       BIGINT          NOT NULL                 COMMENT '接收者ID',
    `type`              INT             DEFAULT 0                COMMENT '消息类型（0=系统 1=私聊 2=订单）',
    `title`             VARCHAR(100)    DEFAULT NULL             COMMENT '消息标题',
    `content`           TEXT            NOT NULL                 COMMENT '消息内容',
    `is_read`           INT             DEFAULT 0                COMMENT '是否已读（0=未读 1=已读）',
    `read_time`         DATETIME        DEFAULT NULL             COMMENT '已读时间',
    `business_id`       BIGINT          DEFAULT NULL             COMMENT '关联业务ID',
    `conversation_id`   BIGINT          DEFAULT NULL             COMMENT '会话ID',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_receiver_read_time` (`receiver_id`, `is_read`, `create_time`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_sender_id_create_time` (`sender_id`, `create_time` DESC),
    KEY `idx_type` (`type`),
    KEY `idx_business_id` (`business_id`),
    KEY `idx_receiver_type_is_read_time` (`receiver_id`, `type`, `is_read`, `create_time` DESC),
    KEY `idx_del_flag_create_time` (`del_flag`, `create_time` DESC),
    CONSTRAINT `chk_message_is_read` CHECK (`is_read` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ===================================================================
-- 10.1 消息归档表 (eo_message_archive)
-- ===================================================================
CREATE TABLE `eo_message_archive` (
    `id`                BIGINT          NOT NULL                 COMMENT '主键ID',
    `sender_id`         BIGINT          DEFAULT NULL             COMMENT '发送者ID（NULL=系统消息）',
    `receiver_id`       BIGINT          NOT NULL                 COMMENT '接收者ID',
    `type`              INT             DEFAULT 0                COMMENT '消息类型（0=系统 1=私聊 2=订单）',
    `title`             VARCHAR(100)    DEFAULT NULL             COMMENT '消息标题',
    `content`           TEXT            NOT NULL                 COMMENT '消息内容',
    `is_read`           INT             DEFAULT 0                COMMENT '是否已读（0=未读 1=已读）',
    `read_time`         DATETIME        DEFAULT NULL             COMMENT '已读时间',
    `business_id`       BIGINT          DEFAULT NULL             COMMENT '关联业务ID',
    `conversation_id`   BIGINT          DEFAULT NULL             COMMENT '会话ID',
    `create_time`       DATETIME        DEFAULT NULL             COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT NULL             COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `archived_at`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`id`),
    KEY `idx_archived_at` (`archived_at`),
    KEY `idx_receiver_id` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息归档表';

-- ===================================================================
-- 11. 消息模板表 (eo_message_template)
-- ===================================================================
CREATE TABLE `eo_message_template` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `template_code`     VARCHAR(50)     NOT NULL                 COMMENT '模板编码',
    `template_name`     VARCHAR(100)    NOT NULL                 COMMENT '模板名称',
    `template_type`     VARCHAR(32)     DEFAULT NULL             COMMENT '模板类型（system/order/chat）',
    `title`             VARCHAR(100)    DEFAULT NULL             COMMENT '标题模板',
    `content`           TEXT            NOT NULL                 COMMENT '内容模板（支持${}占位符）',
    `variables`         TEXT            DEFAULT NULL             COMMENT '变量定义（JSON）',
    `status`            INT             DEFAULT 1                COMMENT '状态（0=禁用 1=启用）',
    `remark`            VARCHAR(200)    DEFAULT NULL             COMMENT '备注',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_template_type` (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息模板表';

-- ===================================================================
-- 12. 消息订阅表 (eo_message_subscription)
-- ===================================================================
CREATE TABLE `eo_message_subscription` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL                 COMMENT '用户ID',
    `message_type`      VARCHAR(32)     NOT NULL                 COMMENT '消息类型',
    `push_channel`      VARCHAR(32)     NOT NULL                 COMMENT '推送渠道（websocket/email/sms）',
    `enabled`           TINYINT(1)      DEFAULT 1                COMMENT '是否启用',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_channel` (`user_id`, `message_type`, `push_channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息订阅表';

-- ===================================================================
-- 13. 离线消息表 (eo_offline_message)
-- ===================================================================
CREATE TABLE `eo_offline_message` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL                 COMMENT '用户ID',
    `message_id`        BIGINT          NOT NULL                 COMMENT '消息ID',
    `push_channel`      VARCHAR(32)     NOT NULL                 COMMENT '推送渠道',
    `push_status`       INT             DEFAULT 0                COMMENT '推送状态（0=待推送 1=已推送 2=失败）',
    `push_time`         DATETIME        DEFAULT NULL             COMMENT '推送时间',
    `retry_count`       INT             DEFAULT 0                COMMENT '重试次数',
    `max_retry_count`   INT             DEFAULT 3                COMMENT '最大重试次数',
    `last_retry_time`   DATETIME        DEFAULT NULL             COMMENT '最后重试时间',
    `del_flag`          INT             DEFAULT 0                COMMENT '删除标志（0=正常 2=已删除）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT          DEFAULT 0                COMMENT '创建人ID',
    `update_by`         BIGINT          DEFAULT 0                COMMENT '更新人ID',
    `version`           INT             DEFAULT 0                COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_push_status` (`push_status`),
    KEY `idx_user_id_push_status` (`user_id`, `push_status`),
    KEY `idx_push_retry_time` (`push_status`, `retry_count`, `create_time` DESC),
    CONSTRAINT `chk_offline_message_push_status` CHECK (`push_status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='离线消息表';

-- ===================================================================
-- 14. 文件上传表 (upload_file)
-- ===================================================================
CREATE TABLE `upload_file` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `file_name`         VARCHAR(200)    NOT NULL                 COMMENT '文件名',
    `file_path`         VARCHAR(500)    NOT NULL                 COMMENT '文件存储路径',
    `file_url`          VARCHAR(500)    DEFAULT NULL             COMMENT '文件访问URL',
    `file_size`         BIGINT          DEFAULT NULL             COMMENT '文件大小（字节）',
    `file_type`         VARCHAR(20)     DEFAULT NULL             COMMENT '文件扩展名',
    `mime_type`         VARCHAR(100)    DEFAULT NULL             COMMENT 'MIME类型',
    `md5`               VARCHAR(32)     DEFAULT NULL             COMMENT '文件MD5',
    `business_type`     VARCHAR(32)     DEFAULT NULL             COMMENT '业务类型',
    `business_id`       BIGINT          DEFAULT NULL             COMMENT '业务ID',
    `uploader_id`       BIGINT          DEFAULT NULL             COMMENT '上传者ID',
    `status`            INT             DEFAULT 1                COMMENT '状态（0=禁用 1=正常）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_md5` (`md5`),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_uploader_id` (`uploader_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_business_status` (`business_type`, `business_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';

-- ===================================================================
-- 15. 操作日志表 (sys_oper_log)
-- ===================================================================
CREATE TABLE `sys_oper_log` (
    `oper_id`           BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志主键',
    `title`             VARCHAR(50)     DEFAULT NULL             COMMENT '模块标题',
    `business_type`     INT             DEFAULT 0                COMMENT '业务类型（0=其它 1=新增 2=修改 3=删除）',
    `method`            VARCHAR(100)    DEFAULT NULL             COMMENT '方法名称',
    `request_method`    VARCHAR(10)     DEFAULT NULL             COMMENT '请求方式',
    `operator_type`     INT             DEFAULT 0                COMMENT '操作类别（0=后台用户 1=前台用户）',
    `oper_name`         VARCHAR(50)     DEFAULT NULL             COMMENT '操作人员',
    `oper_url`          VARCHAR(255)    DEFAULT NULL             COMMENT '请求URL',
    `oper_ip`           VARCHAR(128)    DEFAULT NULL             COMMENT '主机地址',
    `oper_location`     VARCHAR(255)    DEFAULT NULL             COMMENT '操作地点',
    `oper_param`        TEXT            DEFAULT NULL             COMMENT '请求参数',
    `json_result`       TEXT            DEFAULT NULL             COMMENT '返回参数',
    `status`            INT             DEFAULT 0                COMMENT '操作状态（0=正常 1=异常）',
    `error_msg`         VARCHAR(2000)   DEFAULT NULL             COMMENT '错误消息',
    `oper_time`         DATETIME        DEFAULT NULL             COMMENT '操作时间',
    `cost_time`         BIGINT          DEFAULT 0                COMMENT '消耗时间（毫秒）',
    PRIMARY KEY (`oper_id`),
    KEY `idx_oper_time` (`oper_time`),
    KEY `idx_oper_name` (`oper_name`),
    KEY `idx_oper_name_time` (`oper_name`, `oper_time` DESC),
    KEY `idx_status` (`status`),
    KEY `idx_business_type_time` (`business_type`, `oper_time` DESC),
    CONSTRAINT `chk_oper_log_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ===================================================================
-- 16. 搜索历史归档表 (search_history_archive)
-- ===================================================================
CREATE TABLE `search_history_archive` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `user_id`       BIGINT          NOT NULL                 COMMENT '用户ID',
    `keyword`       VARCHAR(100)    NOT NULL                 COMMENT '搜索关键词',
    `search_time`   DATETIME        NOT NULL                 COMMENT '搜索时间',
    `archived_at`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_search_time` (`search_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史归档表';

-- ===================================================================
-- 17. 操作日志归档表 (sys_oper_log_archive)
-- ===================================================================
CREATE TABLE `sys_oper_log_archive` (
    `oper_id`           BIGINT          NOT NULL                 COMMENT '日志主键',
    `title`             VARCHAR(50)     DEFAULT NULL             COMMENT '模块标题',
    `business_type`     INT             DEFAULT 0                COMMENT '业务类型（0=其它 1=新增 2=修改 3=删除）',
    `method`            VARCHAR(100)    DEFAULT NULL             COMMENT '方法名称',
    `request_method`    VARCHAR(10)     DEFAULT NULL             COMMENT '请求方式',
    `operator_type`     INT             DEFAULT 0                COMMENT '操作类别（0=后台用户 1=前台用户）',
    `oper_name`         VARCHAR(50)     DEFAULT NULL             COMMENT '操作人员',
    `oper_url`          VARCHAR(255)    DEFAULT NULL             COMMENT '请求URL',
    `oper_ip`           VARCHAR(128)    DEFAULT NULL             COMMENT '主机地址',
    `oper_location`     VARCHAR(255)    DEFAULT NULL             COMMENT '操作地点',
    `oper_param`        TEXT            DEFAULT NULL             COMMENT '请求参数',
    `json_result`       TEXT            DEFAULT NULL             COMMENT '返回参数',
    `status`            INT             DEFAULT 0                COMMENT '操作状态（0=正常 1=异常）',
    `error_msg`         VARCHAR(2000)   DEFAULT NULL             COMMENT '错误消息',
    `oper_time`         DATETIME        DEFAULT NULL             COMMENT '操作时间',
    `cost_time`         BIGINT          DEFAULT 0                COMMENT '消耗时间（毫秒）',
    `archived_at`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`oper_id`),
    KEY `idx_oper_time` (`oper_time`),
    KEY `idx_oper_name` (`oper_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志归档表';

-- ===================================================================
-- 迁移完成
-- ===================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- 添加外键约束
ALTER TABLE `product` ADD CONSTRAINT `fk_product_category` 
    FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL;

ALTER TABLE `product_image` ADD CONSTRAINT `fk_image_product`
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE;

ALTER TABLE `eo_order` ADD CONSTRAINT `fk_order_buyer`
    FOREIGN KEY (`buyer_id`) REFERENCES `sys_user`(`user_id`);

ALTER TABLE `eo_order` ADD CONSTRAINT `fk_order_seller`
    FOREIGN KEY (`seller_id`) REFERENCES `sys_user`(`user_id`);

ALTER TABLE `eo_order` ADD CONSTRAINT `fk_order_product`
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`);

ALTER TABLE `eo_payment` ADD CONSTRAINT `fk_payment_order`
    FOREIGN KEY (`order_id`) REFERENCES `eo_order`(`id`);

ALTER TABLE `eo_payment` ADD CONSTRAINT `fk_payment_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`user_id`);

ALTER TABLE `eo_message` ADD CONSTRAINT `fk_message_sender`
    FOREIGN KEY (`sender_id`) REFERENCES `sys_user`(`user_id`) ON DELETE SET NULL;

ALTER TABLE `eo_message` ADD CONSTRAINT `fk_message_receiver`
    FOREIGN KEY (`receiver_id`) REFERENCES `sys_user`(`user_id`);

ALTER TABLE `upload_file` ADD CONSTRAINT `fk_file_uploader`
    FOREIGN KEY (`uploader_id`) REFERENCES `sys_user`(`user_id`) ON DELETE SET NULL;

SELECT 'V2 - Complete database schema created successfully (optimized)' AS result;
