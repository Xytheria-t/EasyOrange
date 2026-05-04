-- ===================================================================
-- EasyOrange 校园二手交易平台 - 数据库初始化脚本
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- ===================================================================
-- 整合原 V1~V7：基线 + eo_ 前缀 + TINYINT 状态 + 雪花ID + 无外键 + 优化索引
-- ===================================================================
-- ⚠️ 部署前需在 MySQL 配置 (my.cnf/my.ini) 中添加:
--    ngram_token_size = 2
--    用于优化 FULLTEXT 中文搜索精度，减少索引体积
-- ===================================================================

CREATE DATABASE IF NOT EXISTS `easyorange`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `easyorange`;

SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================================
-- 1. 用户模块
-- ===================================================================

CREATE TABLE `eo_user` (
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `username` VARCHAR(30) NOT NULL COMMENT '用户账号',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `user_type` VARCHAR(2) NOT NULL DEFAULT '01' COMMENT '用户类型（01 普通用户 02 管理员）',
    `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
    `phonenumber` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
    `student_id` VARCHAR(20) DEFAULT NULL COMMENT '学号',
    `real_name` VARCHAR(30) DEFAULT NULL COMMENT '真实姓名',
    `nick_name` VARCHAR(30) DEFAULT NULL COMMENT '用户昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
    `sex` TINYINT NOT NULL DEFAULT 0 COMMENT '用户性别（0 未知 1 男 2 女）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '帐号状态（0 正常 1 禁用 2 锁定）',
    `login_ip` VARCHAR(128) DEFAULT NULL COMMENT '最后登录 IP',
    `login_date` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `pwd_update_date` DATETIME DEFAULT NULL COMMENT '密码最后更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_eo_user_username` (`username`),
    UNIQUE KEY `uk_eo_user_email` (`email`),
    UNIQUE KEY `uk_eo_user_phone` (`phonenumber`),
    UNIQUE KEY `uk_eo_user_student_id` (`student_id`),
    KEY `idx_eo_user_create_time` (`create_time`),
    KEY `idx_eo_user_status_del` (`status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_user_type_status` (`user_type`, `status`, `del_flag`),
    CONSTRAINT `chk_eo_user_status` CHECK (`status` IN (0, 1, 2)),
    CONSTRAINT `chk_eo_user_sex` CHECK (`sex` IN (0, 1, 2)),
    CONSTRAINT `chk_eo_user_type` CHECK (`user_type` IN ('01', '02'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';

-- ===================================================================
-- 2. 商品模块
-- ===================================================================

CREATE TABLE `eo_category` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类 ID',
    `level` TINYINT NOT NULL DEFAULT 1 COMMENT '分类层级',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 启用）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_category_parent_id` (`parent_id`),
    KEY `idx_eo_category_status_sort` (`status`, `del_flag`, `sort_order`),
    CONSTRAINT `chk_eo_category_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

CREATE TABLE `eo_product` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者 ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `stock` INT NOT NULL DEFAULT 1 COMMENT '库存数量',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '商品状态（0 草稿 1 上架 2 已售 3 下架）',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `condition_level` TINYINT DEFAULT NULL COMMENT '新旧程度（1-10）',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '交易地点',
    `contact_method` VARCHAR(200) DEFAULT NULL COMMENT '联系方式',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签',
    `search_text` TEXT DEFAULT NULL COMMENT '搜索文本冗余字段',
    `price_update_time` DATETIME DEFAULT NULL COMMENT '价格最后更新时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_user_time` (`user_id`, `create_time` DESC),
    KEY `idx_eo_product_category_status_time` (`category_id`, `status`, `create_time` DESC),
    KEY `idx_eo_product_status_price` (`status`, `price`),
    KEY `idx_eo_product_search` (`status`, `del_flag`, `category_id`, `create_time` DESC),
    KEY `idx_eo_product_status_del_price` (`status`, `del_flag`, `price`),
    KEY `idx_eo_product_user_status_del` (`user_id`, `status`, `del_flag`, `create_time` DESC),
    FULLTEXT KEY `ft_eo_product_name` (`name`) WITH PARSER ngram,
    FULLTEXT KEY `ft_eo_product_search_text` (`search_text`) WITH PARSER ngram,
    CONSTRAINT `chk_eo_product_price` CHECK (`price` >= 0),
    CONSTRAINT `chk_eo_product_original_price` CHECK (`original_price` IS NULL OR `original_price` >= 0),
    CONSTRAINT `chk_eo_product_stock` CHECK (`stock` >= 0),
    CONSTRAINT `chk_eo_product_status` CHECK (`status` IN (0, 1, 2, 3)),
    CONSTRAINT `chk_eo_product_condition` CHECK (`condition_level` IS NULL OR (`condition_level` >= 1 AND `condition_level` <= 10)),
    CONSTRAINT `chk_eo_product_view_count` CHECK (`view_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息表';

CREATE TABLE `eo_product_detail` (
    `product_id` BIGINT NOT NULL COMMENT '商品 ID',
    `description` TEXT DEFAULT NULL COMMENT '商品详情描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品详情表';

CREATE TABLE `eo_product_image` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片 URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `is_main` TINYINT NOT NULL DEFAULT 0 COMMENT '是否主图（0 否 1 是）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_image_product_sort` (`product_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品图片表';

CREATE TABLE `eo_product_report` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `product_id` BIGINT NOT NULL COMMENT '被举报商品 ID',
    `reporter_id` BIGINT NOT NULL COMMENT '举报人 ID',
    `reason` VARCHAR(500) NOT NULL COMMENT '举报原因',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态（0 待处理 1 已处理 2 已忽略）',
    `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_report_product_id` (`product_id`),
    KEY `idx_eo_product_report_reporter_id` (`reporter_id`),
    KEY `idx_eo_product_report_status_time` (`status`, `create_time` DESC),
    CONSTRAINT `chk_eo_product_report_status` CHECK (`status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品举报表';

CREATE TABLE `eo_favorite` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_favorite_user_product` (`user_id`, `product_id`),
    KEY `idx_eo_favorite_user_time` (`user_id`, `create_time` DESC),
    KEY `idx_eo_favorite_product_count` (`product_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';

-- ===================================================================
-- 3. 搜索模块
-- ===================================================================

CREATE TABLE `eo_search_history` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `keyword` VARCHAR(100) NOT NULL COMMENT '搜索关键词',
    `search_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_search_history_user_keyword` (`user_id`, `keyword`),
    KEY `idx_eo_search_history_user_time` (`user_id`, `search_time` DESC),
    KEY `idx_eo_search_history_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索历史表';

CREATE TABLE `eo_hot_keyword` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `keyword` VARCHAR(100) NOT NULL COMMENT '关键词',
    `search_count` INT NOT NULL DEFAULT 0 COMMENT '搜索次数',
    `last_search_time` DATETIME DEFAULT NULL COMMENT '最后搜索时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_hot_keyword_keyword` (`keyword`),
    KEY `idx_eo_hot_keyword_count` (`search_count` DESC),
    KEY `idx_eo_hot_keyword_last_time` (`last_search_time`),
    CONSTRAINT `chk_eo_hot_keyword_count` CHECK (`search_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='热门关键词表';

-- ===================================================================
-- 4. 订单模块
-- ===================================================================

CREATE TABLE `eo_order` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `buyer_id` BIGINT NOT NULL COMMENT '买家 ID',
    `seller_id` BIGINT NOT NULL COMMENT '卖家 ID',
    `product_id` BIGINT NOT NULL COMMENT '商品 ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态（0 待付款 1 待发货 2 待收货 3 已完成 4 已取消 5 已退款）',
    `payment_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态（0 未支付 1 已支付 2 已退款）',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_order_order_no` (`order_no`),
    KEY `idx_eo_order_buyer_status_time` (`buyer_id`, `status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_order_seller_status_time` (`seller_id`, `status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_order_product_id` (`product_id`),
    KEY `idx_eo_order_payment_status` (`payment_status`),
    KEY `idx_eo_order_status_payment` (`status`, `payment_status`, `create_time` DESC),
    CONSTRAINT `chk_eo_order_amount` CHECK (`amount` >= 0),
    CONSTRAINT `chk_eo_order_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5)),
    CONSTRAINT `chk_eo_order_payment_status` CHECK (`payment_status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

-- ===================================================================
-- 5. 支付模块
-- ===================================================================

CREATE TABLE `eo_payment` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `payment_no` VARCHAR(64) NOT NULL COMMENT '支付流水号',
    `order_id` BIGINT NOT NULL COMMENT '订单 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `refunded_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已退款金额',
    `payment_method` TINYINT DEFAULT NULL COMMENT '支付方式（1 微信 2 支付宝 3 余额）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态（0 待支付 1 已支付 2 已退款 3 部分退款 4 支付失败 5 已关闭 6 支付中 7 退款中）',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '第三方支付流水号',
    `refund_reason` VARCHAR(500) DEFAULT NULL COMMENT '退款原因',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
    `attach` TEXT DEFAULT NULL COMMENT '附加数据',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_payment_payment_no` (`payment_no`),
    UNIQUE KEY `uk_eo_payment_transaction_id` (`transaction_id`),
    KEY `idx_eo_payment_order_id` (`order_id`),
    KEY `idx_eo_payment_user_time` (`user_id`, `create_time` DESC),
    KEY `idx_eo_payment_status_method` (`status`, `payment_method`, `create_time` DESC),
    KEY `idx_eo_payment_user_status` (`user_id`, `status`, `create_time` DESC),
    CONSTRAINT `chk_eo_payment_amount` CHECK (`amount` >= 0),
    CONSTRAINT `chk_eo_payment_refunded_amount` CHECK (`refunded_amount` >= 0),
    CONSTRAINT `chk_eo_payment_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5, 6, 7)),
    CONSTRAINT `chk_eo_payment_method` CHECK (`payment_method` IS NULL OR `payment_method` IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

CREATE TABLE `eo_payment_config` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
    `channel_name` VARCHAR(100) NOT NULL COMMENT '渠道名称',
    `app_id` VARCHAR(100) DEFAULT NULL COMMENT '应用 ID',
    `private_key` TEXT DEFAULT NULL COMMENT '商户私钥',
    `public_key` TEXT DEFAULT NULL COMMENT '商户公钥',
    `sandbox` TINYINT NOT NULL DEFAULT 0 COMMENT '是否沙箱环境',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 启用）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_payment_config_channel` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付渠道配置表';

-- ===================================================================
-- 6. 消息模块
-- ===================================================================

CREATE TABLE `eo_message` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `sender_id` BIGINT DEFAULT NULL COMMENT '发送者 ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者 ID',
    `type` TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '消息标题',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读（0 未读 1 已读）',
    `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
    `business_id` BIGINT DEFAULT NULL COMMENT '业务 ID',
    `conversation_id` BIGINT DEFAULT NULL COMMENT '会话 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_message_sender_time` (`sender_id`, `create_time` DESC),
    KEY `idx_eo_message_receiver_read_time` (`receiver_id`, `is_read`, `create_time` DESC),
    KEY `idx_eo_message_business_id` (`business_id`),
    KEY `idx_eo_message_conversation_time` (`conversation_id`, `create_time` DESC),
    CONSTRAINT `chk_eo_message_is_read` CHECK (`is_read` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';

CREATE TABLE `eo_message_archive` (
    `id` BIGINT NOT NULL COMMENT '消息 ID',
    `sender_id` BIGINT DEFAULT NULL COMMENT '发送者 ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者 ID',
    `type` TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '消息标题',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
    `business_id` BIGINT DEFAULT NULL COMMENT '业务 ID',
    `conversation_id` BIGINT DEFAULT NULL COMMENT '会话 ID',
    `create_time` DATETIME DEFAULT NULL COMMENT '原创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '原更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `archived_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`id`),
    KEY `idx_eo_message_archive_receiver` (`receiver_id`),
    KEY `idx_eo_message_archive_time` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息归档表';

CREATE TABLE `eo_message_subscription` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `message_type` VARCHAR(50) NOT NULL COMMENT '消息类型',
    `push_channel` VARCHAR(50) NOT NULL COMMENT '推送渠道',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_message_subscription_user_type_channel` (`user_id`, `message_type`, `push_channel`),
    KEY `idx_eo_message_subscription_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息订阅表';

CREATE TABLE `eo_message_template` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_type` VARCHAR(50) DEFAULT NULL COMMENT '模板类型',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '消息标题模板',
    `content` TEXT NOT NULL COMMENT '消息内容模板',
    `variables` TEXT DEFAULT NULL COMMENT '模板变量定义',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 启用）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_message_template_code` (`template_code`),
    KEY `idx_eo_message_template_type` (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表';

CREATE TABLE `eo_offline_message` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `message_id` BIGINT NOT NULL COMMENT '消息 ID',
    `push_channel` VARCHAR(50) NOT NULL COMMENT '推送渠道',
    `push_status` TINYINT NOT NULL DEFAULT 0 COMMENT '推送状态（0 待推送 1 已推送 2 推送失败）',
    `push_time` DATETIME DEFAULT NULL COMMENT '推送时间',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retry_count` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `last_retry_time` DATETIME DEFAULT NULL COMMENT '最后重试时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_offline_message_user_status` (`user_id`, `push_status`),
    KEY `idx_eo_offline_message_message_id` (`message_id`),
    KEY `idx_eo_offline_message_retry` (`push_status`, `retry_count`, `create_time` DESC),
    CONSTRAINT `chk_eo_offline_message_push_status` CHECK (`push_status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='离线消息表';

-- ===================================================================
-- 7. 文件模块
-- ===================================================================

CREATE TABLE `eo_upload_file` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `file_name` VARCHAR(200) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件访问 URL',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件扩展名',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    `md5` VARCHAR(32) DEFAULT NULL COMMENT '文件 MD5',
    `business_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
    `business_id` BIGINT DEFAULT NULL COMMENT '业务 ID',
    `uploader_id` BIGINT DEFAULT NULL COMMENT '上传者 ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 正常）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 2 删除）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_upload_file_md5` (`md5`),
    KEY `idx_eo_upload_file_business` (`business_type`, `business_id`),
    KEY `idx_eo_upload_file_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件上传记录表';

-- ===================================================================
-- 8. 日志模块
-- ===================================================================

CREATE TABLE `eo_oper_log` (
    `oper_id` BIGINT NOT NULL COMMENT '日志主键',
    `title` VARCHAR(50) DEFAULT NULL COMMENT '模块标题',
    `business_type` TINYINT NOT NULL DEFAULT 0 COMMENT '业务类型',
    `method` VARCHAR(100) DEFAULT NULL COMMENT '方法名称',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方式',
    `operator_type` TINYINT NOT NULL DEFAULT 0 COMMENT '操作类别',
    `oper_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人员',
    `oper_url` VARCHAR(255) DEFAULT NULL COMMENT '请求 URL',
    `oper_ip` VARCHAR(128) DEFAULT NULL COMMENT '主机地址',
    `oper_location` VARCHAR(255) DEFAULT NULL COMMENT '操作地点',
    `oper_param` TEXT DEFAULT NULL COMMENT '请求参数',
    `json_result` TEXT DEFAULT NULL COMMENT '返回参数',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '操作状态（0 正常 1 异常）',
    `error_msg` VARCHAR(2000) DEFAULT NULL COMMENT '错误消息',
    `oper_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `cost_time` INT NOT NULL DEFAULT 0 COMMENT '消耗时间（毫秒）',
    PRIMARY KEY (`oper_id`),
    KEY `idx_eo_oper_log_time` (`oper_time`),
    KEY `idx_eo_oper_log_name_time` (`oper_name`, `oper_time` DESC),
    KEY `idx_eo_oper_log_business_time` (`business_type`, `oper_time` DESC),
    KEY `idx_eo_oper_log_status_time` (`status`, `oper_time` DESC),
    CONSTRAINT `chk_eo_oper_log_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

CREATE TABLE `eo_oper_log_archive` (
    `oper_id` BIGINT NOT NULL COMMENT '归档日志主键',
    `title` VARCHAR(50) DEFAULT NULL COMMENT '模块标题',
    `business_type` TINYINT NOT NULL DEFAULT 0 COMMENT '业务类型',
    `method` VARCHAR(100) DEFAULT NULL COMMENT '方法名称',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方式',
    `operator_type` TINYINT NOT NULL DEFAULT 0 COMMENT '操作类别',
    `oper_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人员',
    `oper_url` VARCHAR(255) DEFAULT NULL COMMENT '请求 URL',
    `oper_ip` VARCHAR(128) DEFAULT NULL COMMENT '主机地址',
    `oper_location` VARCHAR(255) DEFAULT NULL COMMENT '操作地点',
    `oper_param` TEXT DEFAULT NULL COMMENT '请求参数',
    `json_result` TEXT DEFAULT NULL COMMENT '返回参数',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '操作状态（0 正常 1 异常）',
    `error_msg` VARCHAR(2000) DEFAULT NULL COMMENT '错误消息',
    `oper_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `cost_time` INT NOT NULL DEFAULT 0 COMMENT '消耗时间（毫秒）',
    `archived_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`oper_id`),
    KEY `idx_eo_oper_log_archive_time` (`oper_time`),
    KEY `idx_eo_oper_log_archive_name` (`oper_name`),
    KEY `idx_eo_oper_log_archive_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志归档表';

-- ===================================================================
-- 9. 领域事件表 (Outbox 模式)
-- ===================================================================

CREATE TABLE `eo_domain_event` (
    `id`              BIGINT          NOT NULL                 COMMENT '主键ID',
    `event_id`        CHAR(36)        NOT NULL                 COMMENT '事件唯一标识(UUID)',
    `aggregate_type`  VARCHAR(100)    NOT NULL                 COMMENT '聚合类型',
    `aggregate_id`    BIGINT          NOT NULL                 COMMENT '聚合ID',
    `event_type`      VARCHAR(100)    NOT NULL                 COMMENT '事件类型',
    `payload`         TEXT            DEFAULT NULL             COMMENT '事件载荷(JSON)',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/PUBLISHED/FAILED)',
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件创建时间',
    `published_at`    DATETIME(3)     DEFAULT NULL             COMMENT '事件发布时间',
    `del_flag`        TINYINT         NOT NULL DEFAULT 0       COMMENT '删除标志(0=正常 2=已删除)',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    `create_by`       BIGINT          DEFAULT NULL             COMMENT '创建人ID',
    `update_by`       BIGINT          DEFAULT NULL             COMMENT '更新人ID',
    `version`         INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_domain_event_event_id` (`event_id`),
    KEY `idx_eo_domain_event_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_eo_domain_event_status_created` (`status`, `created_at`),
    KEY `idx_eo_domain_event_event_type` (`event_type`),
    CONSTRAINT `chk_eo_domain_event_status` CHECK (`status` IN ('PENDING', 'PUBLISHED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领域事件表';

-- ===================================================================
-- 10. Saga 分布式事务状态表
-- ===================================================================

CREATE TABLE `eo_saga_status` (
    `saga_id`           CHAR(36)        NOT NULL                 COMMENT 'Saga 实例唯一标识(UUID)',
    `saga_type`         VARCHAR(100)    NOT NULL                 COMMENT 'Saga 类型',
    `state`             VARCHAR(20)     NOT NULL                 COMMENT 'Saga 状态',
    `current_step`      VARCHAR(50)     DEFAULT NULL             COMMENT '当前执行步骤',
    `payload`           TEXT            DEFAULT NULL             COMMENT 'Saga 载荷(JSON)',
    `error_message`     TEXT            DEFAULT NULL             COMMENT '错误信息',
    `compensation_log`  TEXT            DEFAULT NULL             COMMENT '补偿日志(JSON)',
    `retry_count`       INT             NOT NULL DEFAULT 0       COMMENT '重试次数',
    `created_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Saga 创建时间',
    `updated_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Saga 更新时间',
    PRIMARY KEY (`saga_id`),
    KEY `idx_eo_saga_status_type_state` (`saga_type`, `state`),
    KEY `idx_eo_saga_status_state_created` (`state`, `created_at`),
    KEY `idx_eo_saga_status_created_at` (`created_at`),
    CONSTRAINT `chk_eo_saga_status_state` CHECK (
        `state` IN ('STARTED', 'ORDER_CREATED', 'PAYMENT_CREATED', 'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'FAILED')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Saga 分布式事务状态表';

-- ===================================================================
-- 11. 幂等性键表
-- ===================================================================

CREATE TABLE `eo_idempotency_key` (
    `id`              BIGINT          NOT NULL COMMENT '主键ID',
    `idempotency_key` VARCHAR(255)    NOT NULL                 COMMENT '幂等性键',
    `user_id`         BIGINT          NOT NULL                 COMMENT '用户ID',
    `request_hash`    VARCHAR(64)     NOT NULL                 COMMENT '请求哈希',
    `response_data`   TEXT            DEFAULT NULL             COMMENT '响应数据(JSON)',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/COMPLETED/FAILED)',
    `expires_at`      DATETIME        NOT NULL                 COMMENT '过期时间',
    `del_flag`        TINYINT         NOT NULL DEFAULT 0       COMMENT '删除标志(0=正常 2=已删除)',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    `create_by`       BIGINT          DEFAULT NULL             COMMENT '创建人ID',
    `update_by`       BIGINT          DEFAULT NULL             COMMENT '更新人ID',
    `version`         INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_idempotency_key_key` (`idempotency_key`),
    KEY `idx_eo_idempotency_key_user_expires` (`user_id`, `expires_at`),
    CONSTRAINT `chk_eo_idempotency_key_status` CHECK (`status` IN ('PENDING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等性键表';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'EasyOrange database initialized successfully' AS result;
