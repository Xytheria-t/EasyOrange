-- ===================================================================
-- EasyOrange 校园二手交易平台 - 数据库初始化
-- Version: V1
-- 职责: 仅创建表结构，不包含索引、约束（按 Flyway 最佳实践分层）
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- ===================================================================

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
    PRIMARY KEY (`user_id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`id`)
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
    PRIMARY KEY (`oper_id`)
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
    PRIMARY KEY (`oper_id`)
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
    PRIMARY KEY (`id`)
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
    `error_message`    TEXT            DEFAULT NULL             COMMENT '错误信息',
    `compensation_log`  TEXT            DEFAULT NULL             COMMENT '补偿日志(JSON)',
    `retry_count`       INT             NOT NULL DEFAULT 0       COMMENT '重试次数',
    `created_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Saga 创建时间',
    `updated_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Saga 更新时间',
    PRIMARY KEY (`saga_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Saga 分布式事务状态表';

-- ===================================================================
-- 11. 幂等性键表
-- ===================================================================

CREATE TABLE `eo_idempotency_key` (
    `id`              BIGINT          NOT NULL                 COMMENT '主键ID',
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
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等性键表';
