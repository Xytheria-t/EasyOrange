-- ===================================================================
-- EasyOrange - 数据库初始化（V1 合并版）
-- 职责: 创建所有初始表结构、索引、约束（当前完整 DDL）
-- 说明: 合并原 V1~V6 为单文件（项目未发版，开发阶段标准化）
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- ===================================================================

-- ===================================================================
-- 1. 用户模块
-- ===================================================================

CREATE TABLE `eo_user` (
    `user_id`     VARCHAR(36)  NOT NULL COMMENT '用户 ID',
    `username`    VARCHAR(30)  NOT NULL COMMENT '用户账号',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `user_type`   VARCHAR(2)   NOT NULL DEFAULT '01' COMMENT '用户类型（01 普通用户 02 管理员）',
    `email`       VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
    `phonenumber` VARCHAR(20)  DEFAULT NULL COMMENT '手机号码',
    `student_id`  VARCHAR(20)  DEFAULT NULL COMMENT '学号',
    `real_name`   VARCHAR(30)  DEFAULT NULL COMMENT '真实姓名',
    `nick_name`   VARCHAR(30)  DEFAULT NULL COMMENT '用户昵称',
    `avatar`      VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
    `sex`         TINYINT      NOT NULL DEFAULT 0 COMMENT '用户性别（0 未知 1 男 2 女）',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '帐号状态（0 正常 1 禁用 2 锁定）',
    `login_ip`    VARCHAR(128) DEFAULT NULL COMMENT '最后登录 IP',
    `login_date`  DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `pwd_update_date` DATETIME DEFAULT NULL COMMENT '密码最后更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(36)  DEFAULT NULL COMMENT '创建者',
    `update_by`   VARCHAR(36)  DEFAULT NULL COMMENT '更新者',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_eo_user_username` (`username`),
    UNIQUE KEY `uk_eo_user_email` (`email`),
    UNIQUE KEY `uk_eo_user_phone` (`phonenumber`),
    UNIQUE KEY `uk_eo_user_student_id` (`student_id`),
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
    `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id`  VARCHAR(36) NOT NULL DEFAULT '0' COMMENT '父分类 ID',
    `level`      TINYINT     NOT NULL DEFAULT 1 COMMENT '分类层级',
    `icon`       VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `sort_order` INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 启用）',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`   TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`    INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_category_parent_id` (`parent_id`),
    KEY `idx_eo_category_status_sort` (`status`, `del_flag`, `sort_order`),
    CONSTRAINT `chk_eo_category_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

CREATE TABLE `eo_product` (
    `id`          VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `user_id`     VARCHAR(36)   NOT NULL COMMENT '发布者 ID',
    `category_id` VARCHAR(36)   DEFAULT NULL COMMENT '分类 ID',
    `name`        VARCHAR(100)  NOT NULL COMMENT '商品名称',
    `price`       DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `stock`       INT           NOT NULL DEFAULT 1 COMMENT '库存数量',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '商品状态（0 草稿 4 待审核 5 已驳回 1 上架 2 已售出 3 下架）',
    `view_count`  INT           NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `condition_level` TINYINT   DEFAULT NULL COMMENT '新旧程度（1-10）',
    `location`    VARCHAR(100)  DEFAULT NULL COMMENT '交易地点',
    `contact_method` VARCHAR(200) DEFAULT NULL COMMENT '联系方式',
    `tags`        VARCHAR(500)  DEFAULT NULL COMMENT '标签',
    `search_text` TEXT          DEFAULT NULL COMMENT '搜索文本冗余字段',
    `price_update_time` DATETIME DEFAULT NULL COMMENT '价格最后更新时间',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(36)   DEFAULT NULL COMMENT '创建者',
    `update_by`   VARCHAR(36)   DEFAULT NULL COMMENT '更新者',
    `del_flag`    TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`     INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_category_status_time` (`category_id`, `status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_product_search` (`status`, `del_flag`, `category_id`, `create_time` DESC),
    KEY `idx_eo_product_status_del_price` (`status`, `del_flag`, `price`),
    KEY `idx_eo_product_user_status_del` (`user_id`, `status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_product_status_del_create_time` (`status`, `del_flag`, `create_time` DESC) COMMENT '商品状态+删除标志+创建时间',
    KEY `idx_eo_product_status_del_view` (`status`, `del_flag`, `view_count` DESC) COMMENT '热门商品查询',
    FULLTEXT KEY `ft_eo_product_name` (`name`) WITH PARSER ngram,
    FULLTEXT KEY `ft_eo_product_search_text` (`search_text`) WITH PARSER ngram,
    CONSTRAINT `chk_eo_product_price` CHECK (`price` >= 0),
    CONSTRAINT `chk_eo_product_original_price` CHECK (`original_price` IS NULL OR `original_price` >= 0),
    CONSTRAINT `chk_eo_product_stock` CHECK (`stock` >= 0),
    CONSTRAINT `chk_eo_product_status` CHECK (`status` IN (0, 4, 5, 1, 2, 3)),
    CONSTRAINT `chk_eo_product_condition` CHECK (`condition_level` IS NULL OR (`condition_level` >= 1 AND `condition_level` <= 10)),
    CONSTRAINT `chk_eo_product_view_count` CHECK (`view_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息表';

CREATE TABLE `eo_product_audit_log` (
    `id`              VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `product_id`      VARCHAR(36) NOT NULL COMMENT '商品 ID',
    `operator_id`     VARCHAR(36) NOT NULL COMMENT '操作人 ID',
    `operator_name`   VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    `action`          TINYINT     NOT NULL COMMENT '审核动作（1 通过 2 拒绝 3 重新提交）',
    `reason`          VARCHAR(500) DEFAULT NULL COMMENT '审核原因',
    `audit_dimensions` VARCHAR(500) DEFAULT NULL COMMENT '审核维度JSON',
    `before_status`   TINYINT     NOT NULL COMMENT '操作前状态',
    `after_status`    TINYINT     NOT NULL COMMENT '操作后状态',
    `remark`          VARCHAR(500) DEFAULT NULL COMMENT '管理员备注',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_product` (`product_id`, `create_time` DESC),
    KEY `idx_audit_operator` (`operator_id`, `create_time` DESC),
    KEY `idx_audit_action_time` (`action`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品审核记录表';

CREATE TABLE `eo_product_detail` (
    `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    `description` TEXT       DEFAULT NULL COMMENT '商品详情描述',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`   VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`    TINYINT    NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`     INT        NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品详情表';

CREATE TABLE `eo_product_image` (
    `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    `image_url`  VARCHAR(500) NOT NULL COMMENT '图片 URL',
    `sort_order` INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `is_main`    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否主图（0 否 1 是）',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`   TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`    INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_image_product_sort` (`product_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品图片表';

CREATE TABLE `eo_product_review` (
    `id`           VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `product_id`   VARCHAR(36) NOT NULL COMMENT '商品 ID',
    `user_id`      VARCHAR(36) NOT NULL COMMENT '评价用户 ID',
    `order_id`     VARCHAR(36) NOT NULL COMMENT '关联订单 ID',
    `rating`       TINYINT     NOT NULL DEFAULT 5 COMMENT '评分（1-5）',
    `content`      TEXT        NOT NULL COMMENT '评价内容',
    `reply_content` TEXT       DEFAULT NULL COMMENT '资产方回复内容',
    `reply_time`   DATETIME    DEFAULT NULL COMMENT '资产方回复时间',
    `likes`        INT         NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态（0 隐藏 1 显示 2 待审核）',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`    VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`     TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`      INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_product_review_user_order` (`user_id`, `order_id`),
    KEY `idx_eo_product_review_product_status_del_time` (`product_id`, `status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_product_review_order_id` (`order_id`),
    CONSTRAINT `chk_eo_product_review_rating` CHECK (`rating` >= 1 AND `rating` <= 5),
    CONSTRAINT `chk_eo_product_review_status` CHECK (`status` IN (0, 1, 2)),
    CONSTRAINT `chk_eo_product_review_likes` CHECK (`likes` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';

CREATE TABLE `eo_product_report` (
    `id`          VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `product_id`  VARCHAR(36) NOT NULL COMMENT '被举报商品 ID',
    `reporter_id` VARCHAR(36) NOT NULL COMMENT '举报人 ID',
    `reason`      VARCHAR(500) NOT NULL COMMENT '举报原因',
    `reason_type` TINYINT     DEFAULT NULL COMMENT '举报类型（1 虚假信息 2 侵权投诉 3 违规内容 4 其他）',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '处理状态（0 待处理 1 处理中 2 已解决 3 已驳回）',
    `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`   VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`    TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_report_product_id` (`product_id`),
    KEY `idx_eo_product_report_reporter_id` (`reporter_id`),
    KEY `idx_eo_product_report_status_time` (`status`, `create_time` DESC),
    CONSTRAINT `chk_eo_product_report_status` CHECK (`status` IN (0, 1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品举报表';

CREATE TABLE `eo_report_handle_history` (
    `id`          VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `report_id`   VARCHAR(36) NOT NULL COMMENT '举报ID',
    `operator_id` VARCHAR(36) NOT NULL COMMENT '操作人ID',
    `action`      VARCHAR(30) NOT NULL COMMENT '动作类型（IGNORE/PRODUCT_OFFLINE/WARN_SENDER/BAN_PRODUCT）',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`   VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`    TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_report_handle_history_report_id` (`report_id`),
    KEY `idx_eo_report_handle_history_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报处理历史表';

CREATE TABLE `eo_favorite` (
    `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `user_id`    VARCHAR(36) NOT NULL COMMENT '用户 ID',
    `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`   TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`    INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_favorite_user_product_del` (`user_id`, `product_id`, `del_flag`),
    KEY `idx_eo_favorite_user_time` (`user_id`, `create_time` DESC),
    KEY `idx_eo_favorite_product_count` (`product_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';

-- ===================================================================
-- 3. 搜索模块
-- ===================================================================

CREATE TABLE `eo_search_history` (
    `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `user_id`    VARCHAR(36) NOT NULL COMMENT '用户 ID',
    `keyword`    VARCHAR(100) NOT NULL COMMENT '搜索关键词',
    `search_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`   TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`    INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_search_history_user_keyword` (`user_id`, `keyword`),
    KEY `idx_eo_search_history_user_time` (`user_id`, `search_time` DESC),
    KEY `idx_eo_search_history_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索历史表';

CREATE TABLE `eo_hot_keyword` (
    `id`               VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `keyword`          VARCHAR(100) NOT NULL COMMENT '关键词',
    `search_count`     INT         NOT NULL DEFAULT 0 COMMENT '搜索次数',
    `last_search_time` DATETIME    DEFAULT NULL COMMENT '最后搜索时间',
    `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`        VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`         TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`          INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
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
    `id`             VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `order_no`       VARCHAR(64)   NOT NULL COMMENT '订单号',
    `buyer_id`       VARCHAR(36)   NOT NULL COMMENT '认领方 ID',
    `seller_id`      VARCHAR(36)   NOT NULL COMMENT '资产方 ID',
    `total_amount`   DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '订单状态（0 待付款 1 待发货 2 待收货 3 已完成 4 已取消 5 已退款）',
    `payment_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '支付状态（0 未支付 1 已支付 2 已退款）',
    `address`        VARCHAR(500)  DEFAULT NULL COMMENT '收货地址',
    `phone`          VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
    `remark`         VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `cancel_reason`  VARCHAR(500)  DEFAULT NULL COMMENT '取消原因',
    `cancel_time`    DATETIME      DEFAULT NULL COMMENT '取消时间',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      VARCHAR(36)   DEFAULT NULL COMMENT '创建者',
    `update_by`      VARCHAR(36)   DEFAULT NULL COMMENT '更新者',
    `del_flag`       TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_order_order_no` (`order_no`),
    KEY `idx_eo_order_buyer_status_time` (`buyer_id`, `status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_order_seller_status_time` (`seller_id`, `status`, `del_flag`, `create_time` DESC),
    KEY `idx_eo_order_status_payment` (`status`, `payment_status`, `create_time` DESC),
    CONSTRAINT `chk_eo_order_total_amount` CHECK (`total_amount` >= 0),
    CONSTRAINT `chk_eo_order_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5)),
    CONSTRAINT `chk_eo_order_payment_status` CHECK (`payment_status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE `eo_order_item` (
    `id`              VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `order_id`        VARCHAR(36)   NOT NULL COMMENT '订单 ID',
    `product_id`      VARCHAR(36)   NOT NULL COMMENT '商品 ID',
    `product_snapshot` JSON         NOT NULL COMMENT '下单时商品信息快照',
    `unit_price`      DECIMAL(10,2) NOT NULL COMMENT '单价',
    `quantity`        INT           NOT NULL DEFAULT 1 COMMENT '数量',
    `subtotal`        DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       VARCHAR(36)   DEFAULT NULL COMMENT '创建者',
    `update_by`       VARCHAR(36)   DEFAULT NULL COMMENT '更新者',
    `del_flag`        TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志',
    `version`         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY `idx_eo_order_item_order_id` (`order_id`, `del_flag`) COMMENT '订单 ID 索引',
    KEY `idx_eo_order_item_product_id` (`product_id`, `del_flag`) COMMENT '商品 ID 索引',
    CONSTRAINT `chk_eo_order_item_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `chk_eo_order_item_subtotal` CHECK (`subtotal` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单行项表';

-- ===================================================================
-- 5. 支付模块
-- ===================================================================

CREATE TABLE `eo_payment` (
    `id`              VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `payment_no`      VARCHAR(64)   NOT NULL COMMENT '支付流水号',
    `order_id`        VARCHAR(36)   NOT NULL COMMENT '订单 ID',
    `user_id`         VARCHAR(36)   NOT NULL COMMENT '用户 ID',
    `amount`          DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `refunded_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已退款金额',
    `payment_method`  TINYINT       DEFAULT NULL COMMENT '支付方式（1 微信 2 支付宝 3 余额）',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '支付状态（0 待支付 1 已支付 2 已退款 3 部分退款 4 支付失败 5 已关闭 6 支付中 7 退款中）',
    `transaction_id`  VARCHAR(64)   DEFAULT NULL COMMENT '第三方支付流水号',
    `refund_reason`   VARCHAR(500)  DEFAULT NULL COMMENT '退款原因',
    `refund_time`     DATETIME      DEFAULT NULL COMMENT '退款时间',
    `attach`          TEXT          DEFAULT NULL COMMENT '附加数据',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       VARCHAR(36)   DEFAULT NULL COMMENT '创建者',
    `update_by`       VARCHAR(36)   DEFAULT NULL COMMENT '更新者',
    `del_flag`        TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_payment_payment_no` (`payment_no`),
    UNIQUE KEY `uk_eo_payment_transaction_id` (`transaction_id`),
    KEY `idx_eo_payment_order_id` (`order_id`),
    KEY `idx_eo_payment_status_method` (`status`, `payment_method`, `create_time` DESC),
    KEY `idx_eo_payment_user_status` (`user_id`, `status`, `create_time` DESC),
    CONSTRAINT `chk_eo_payment_amount` CHECK (`amount` >= 0),
    CONSTRAINT `chk_eo_payment_refunded_amount` CHECK (`refunded_amount` >= 0),
    CONSTRAINT `chk_eo_payment_status` CHECK (`status` IN (0, 1, 2, 3, 4, 5, 6, 7)),
    CONSTRAINT `chk_eo_payment_method` CHECK (`payment_method` IS NULL OR `payment_method` IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

CREATE TABLE `eo_payment_config` (
    `id`           VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
    `channel_name` VARCHAR(100) NOT NULL COMMENT '渠道名称',
    `app_id`       VARCHAR(100) DEFAULT NULL COMMENT '应用 ID',
    `private_key`  TEXT         DEFAULT NULL COMMENT '商户私钥',
    `public_key`   TEXT         DEFAULT NULL COMMENT '商户公钥',
    `sandbox`      TINYINT     NOT NULL DEFAULT 0 COMMENT '是否沙箱环境',
    `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 启用）',
    `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`    VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`     TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`      INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_payment_config_channel` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付渠道配置表';

-- ===================================================================
-- 6. 消息模块
-- ===================================================================

CREATE TABLE `eo_message` (
    `id`              VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `sender_id`       VARCHAR(36) DEFAULT NULL COMMENT '发送者 ID',
    `receiver_id`     VARCHAR(36) NOT NULL COMMENT '接收者 ID',
    `type`            TINYINT     NOT NULL DEFAULT 0 COMMENT '消息类型',
    `title`           VARCHAR(200) DEFAULT NULL COMMENT '消息标题',
    `content`         TEXT        NOT NULL COMMENT '消息内容',
    `is_read`         TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已读（0 未读 1 已读）',
    `msg_status`      VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT '消息状态（SENT/DELIVERED/READ/RECALLED）',
    `recalled_at`     DATETIME    DEFAULT NULL COMMENT '撤回时间',
    `read_time`       DATETIME    DEFAULT NULL COMMENT '已读时间',
    `business_id`     VARCHAR(36) DEFAULT NULL COMMENT '业务 ID',
    `conversation_id` VARCHAR(36) DEFAULT NULL COMMENT '会话 ID',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`       VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`        TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`         INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_message_sender_time` (`sender_id`, `create_time` DESC),
    KEY `idx_eo_message_receiver_read_type_del_time` (`receiver_id`, `is_read`, `del_flag`, `type`, `create_time` DESC),
    KEY `idx_eo_message_conversation_time` (`conversation_id`, `create_time` DESC),
    KEY `idx_eo_message_business_id` (`business_id`),
    CONSTRAINT `chk_eo_message_is_read` CHECK (`is_read` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';

CREATE TABLE `eo_message_archive` (
    `id`              VARCHAR(36) NOT NULL COMMENT '消息 ID',
    `sender_id`       VARCHAR(36) DEFAULT NULL COMMENT '发送者 ID',
    `receiver_id`     VARCHAR(36) NOT NULL COMMENT '接收者 ID',
    `type`            TINYINT     NOT NULL DEFAULT 0 COMMENT '消息类型',
    `title`           VARCHAR(200) DEFAULT NULL COMMENT '消息标题',
    `content`         TEXT        NOT NULL COMMENT '消息内容',
    `is_read`         TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已读',
    `read_time`       DATETIME    DEFAULT NULL COMMENT '已读时间',
    `business_id`     VARCHAR(36) DEFAULT NULL COMMENT '业务 ID',
    `conversation_id` VARCHAR(36) DEFAULT NULL COMMENT '会话 ID',
    `create_time`     DATETIME    DEFAULT NULL COMMENT '原创建时间',
    `update_time`     DATETIME    DEFAULT NULL COMMENT '原更新时间',
    `create_by`       VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`       VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `archived_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (`id`),
    KEY `idx_eo_message_archive_receiver` (`receiver_id`),
    KEY `idx_eo_message_archive_time` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息归档表';

CREATE TABLE `eo_message_subscription` (
    `id`           VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `user_id`      VARCHAR(36) NOT NULL COMMENT '用户 ID',
    `message_type` VARCHAR(50) NOT NULL COMMENT '消息类型',
    `push_channel` VARCHAR(50) NOT NULL COMMENT '推送渠道',
    `enabled`      TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`    VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`     TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`      INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_message_subscription_user_type_channel` (`user_id`, `message_type`, `push_channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息订阅表';

CREATE TABLE `eo_message_template` (
    `id`            VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_type` VARCHAR(50) DEFAULT NULL COMMENT '模板类型',
    `title`         VARCHAR(200) DEFAULT NULL COMMENT '消息标题模板',
    `content`       TEXT        NOT NULL COMMENT '消息内容模板',
    `variables`     TEXT        DEFAULT NULL COMMENT '模板变量定义',
    `status`        TINYINT     NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 启用）',
    `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`     VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`      TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`       INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_message_template_code` (`template_code`),
    KEY `idx_eo_message_template_type` (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表';

CREATE TABLE `eo_offline_message` (
    `id`             VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `user_id`        VARCHAR(36) NOT NULL COMMENT '用户 ID',
    `message_id`     VARCHAR(36) NOT NULL COMMENT '消息 ID',
    `push_channel`   VARCHAR(50) NOT NULL COMMENT '推送渠道',
    `push_status`    TINYINT     NOT NULL DEFAULT 0 COMMENT '推送状态（0 待推送 1 已推送 2 推送失败）',
    `push_time`      DATETIME    DEFAULT NULL COMMENT '推送时间',
    `retry_count`    INT         NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retry_count` INT        NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `last_retry_time` DATETIME   DEFAULT NULL COMMENT '最后重试时间',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`      VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`       TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`        INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
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
    `id`            VARCHAR(36)  NOT NULL COMMENT '主键 ID',
    `file_name`     VARCHAR(200) NOT NULL COMMENT '文件名',
    `file_path`     VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_url`      VARCHAR(500) DEFAULT NULL COMMENT '文件访问 URL',
    `file_size`     BIGINT       DEFAULT NULL COMMENT '文件大小',
    `file_type`     VARCHAR(50)  DEFAULT NULL COMMENT '文件扩展名',
    `mime_type`     VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    `md5`           VARCHAR(32)  DEFAULT NULL COMMENT '文件 MD5',
    `storage_type`  VARCHAR(32)  NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型（LOCAL/S3/OSS）',
    `storage_key`   VARCHAR(500) DEFAULT NULL COMMENT '存储后端标识键',
    `business_type` VARCHAR(50)  DEFAULT NULL COMMENT '业务类型',
    `business_id`   VARCHAR(36)  DEFAULT NULL COMMENT '业务 ID',
    `uploader_id`   VARCHAR(36)  DEFAULT NULL COMMENT '上传者 ID',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0 禁用 1 正常）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     VARCHAR(36)  DEFAULT NULL COMMENT '创建者',
    `update_by`     VARCHAR(36)  DEFAULT NULL COMMENT '更新者',
    `del_flag`      TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_upload_file_md5` (`md5`),
    KEY `idx_eo_upload_file_business` (`business_type`, `business_id`),
    KEY `idx_eo_upload_file_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件上传记录表';

-- ===================================================================
-- 8. 审计日志模块
-- ===================================================================

CREATE TABLE `eo_audit_log` (
    `id`             VARCHAR(36) NOT NULL COMMENT '日志主键',
    `title`          VARCHAR(50) DEFAULT NULL COMMENT '模块标题',
    `business_type`  TINYINT     NOT NULL DEFAULT 0 COMMENT '业务类型',
    `method`         VARCHAR(100) DEFAULT NULL COMMENT '方法名称',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方式',
    `operator_type`  TINYINT     NOT NULL DEFAULT 0 COMMENT '操作类别',
    `username`       VARCHAR(50) DEFAULT NULL COMMENT '操作人员',
    `request_url`    VARCHAR(255) DEFAULT NULL COMMENT '请求 URL',
    `client_ip`      VARCHAR(128) DEFAULT NULL COMMENT '客户端 IP',
    `oper_location`  VARCHAR(255) DEFAULT NULL COMMENT '操作地点',
    `request_params` TEXT        DEFAULT NULL COMMENT '请求参数',
    `response_data`  TEXT        DEFAULT NULL COMMENT '响应数据',
    `status`         TINYINT     NOT NULL DEFAULT 0 COMMENT '操作状态（0 正常 1 异常）',
    `error_msg`      VARCHAR(2000) DEFAULT NULL COMMENT '错误消息',
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `duration`       INT         NOT NULL DEFAULT 0 COMMENT '执行耗时(毫秒)',
    PRIMARY KEY (`id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_username_created_at` (`username`, `created_at` DESC),
    KEY `idx_business_type_created_at` (`business_type`, `created_at` DESC),
    KEY `idx_status_created_at` (`status`, `created_at` DESC),
    CONSTRAINT `chk_eo_audit_log_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志表';

-- ===================================================================
-- 9. AI 功能模块
-- ===================================================================

CREATE TABLE `eo_product_question` (
    `id`         VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `product_id` VARCHAR(36) NOT NULL COMMENT '商品 ID',
    `user_id`    VARCHAR(36) NOT NULL COMMENT '用户 ID',
    `question`   TEXT        NOT NULL COMMENT '问题内容',
    `answer`     TEXT        DEFAULT NULL COMMENT 'AI 回答内容',
    `status`     TINYINT     NOT NULL DEFAULT 0 COMMENT '状态（0 待回答 1 已回答 2 已驳回）',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`  VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by`  VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag`   TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`    INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_product_question_product_id` (`product_id`),
    KEY `idx_eo_product_question_user_id` (`user_id`),
    KEY `idx_eo_product_question_status_time` (`status`, `create_time` DESC),
    CONSTRAINT `chk_eo_product_question_status` CHECK (`status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品问答表';

CREATE TABLE `eo_audit_suggestion` (
    `id`                VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `product_id`        VARCHAR(36)   NOT NULL COMMENT '商品 ID',
    `suggestion_type`   VARCHAR(50)   NOT NULL COMMENT '建议类型（PRICE_AUDIT/DESCRIPTION_AUDIT/CATEGORY_AUDIT/IMAGE_AUDIT）',
    `suggestion_content` JSON          DEFAULT NULL COMMENT '建议内容（JSON）',
    `confidence`        DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '置信度（0.00-1.00）',
    `status`            TINYINT       NOT NULL DEFAULT 0 COMMENT '状态（0 待处理 1 已采纳 2 已忽略）',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
    `create_by`         VARCHAR(36)   DEFAULT NULL COMMENT '创建者',
    `update_by`         VARCHAR(36)   DEFAULT NULL COMMENT '处理者',
    `del_flag`          TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_eo_audit_suggestion_product_id` (`product_id`),
    KEY `idx_eo_audit_suggestion_type_status` (`suggestion_type`, `status`, `create_time` DESC),
    CONSTRAINT `chk_eo_audit_suggestion_status` CHECK (`status` IN (0, 1, 2)),
    CONSTRAINT `chk_eo_audit_suggestion_confidence` CHECK (`confidence` >= 0 AND `confidence` <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 审核建议表';

-- ===================================================================
-- 10. 信用评分模块
-- ===================================================================

CREATE TABLE `eo_user_credit` (
    `id`                VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `user_id`           VARCHAR(36)   NOT NULL COMMENT '用户 ID',
    `credit_score`      INT           NOT NULL DEFAULT 100 COMMENT '信用评分（0-200）',
    `level`             VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT '信用等级（EXCELLENT/GOOD/NORMAL/LOW/BLACKLIST）',
    `total_trades`      INT           NOT NULL DEFAULT 0 COMMENT '总交易数',
    `completed_trades`  INT           NOT NULL DEFAULT 0 COMMENT '已完成交易数',
    `cancelled_trades`  INT           NOT NULL DEFAULT 0 COMMENT '已取消交易数',
    `total_reports`     INT           NOT NULL DEFAULT 0 COMMENT '总举报数',
    `confirmed_reports` INT           NOT NULL DEFAULT 0 COMMENT '已确认举报数',
    `review_avg_rating` DECIMAL(3,2)  DEFAULT NULL COMMENT '评价平均分',
    `last_updated`      DATETIME      DEFAULT NULL COMMENT '最后评分更新时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         VARCHAR(36)   DEFAULT NULL COMMENT '创建者',
    `update_by`         VARCHAR(36)   DEFAULT NULL COMMENT '更新者',
    `del_flag`          TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_user_credit_user_id` (`user_id`),
    KEY `idx_eo_user_credit_score` (`credit_score`),
    KEY `idx_eo_user_credit_level` (`level`),
    KEY `idx_eo_user_credit_last_updated` (`last_updated`),
    CONSTRAINT `chk_eo_user_credit_score` CHECK (`credit_score` >= 0 AND `credit_score` <= 200),
    CONSTRAINT `chk_eo_user_credit_total_trades` CHECK (`total_trades` >= 0),
    CONSTRAINT `chk_eo_user_credit_completed_trades` CHECK (`completed_trades` >= 0),
    CONSTRAINT `chk_eo_user_credit_cancelled_trades` CHECK (`cancelled_trades` >= 0),
    CONSTRAINT `chk_eo_user_credit_total_reports` CHECK (`total_reports` >= 0),
    CONSTRAINT `chk_eo_user_credit_confirmed_reports` CHECK (`confirmed_reports` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信用评分表';

CREATE TABLE `eo_credit_change_log` (
    `id`            VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `user_id`       VARCHAR(36) NOT NULL COMMENT '用户 ID',
    `change_amount` INT         NOT NULL COMMENT '变更分值',
    `before_score`  INT         NOT NULL COMMENT '变更前评分',
    `after_score`   INT         NOT NULL COMMENT '变更后评分',
    `change_type`   VARCHAR(30) NOT NULL COMMENT '变更类型（TRADE_COMPLETE/TRADE_CANCEL/REPORT_CONFIRMED/REVIEW_RATING/RECALCULATE/ADMIN_ADJUST）',
    `reason`        VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    `reference_id`  VARCHAR(36) DEFAULT NULL COMMENT '关联业务 ID（订单ID/举报ID等）',
    `create_by`     VARCHAR(36) DEFAULT NULL COMMENT '操作人/系统 ID',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_eo_credit_change_log_user_id` (`user_id`),
    KEY `idx_eo_credit_change_log_type_time` (`change_type`, `create_time` DESC),
    KEY `idx_eo_credit_change_log_create_time` (`create_time` DESC),
    CONSTRAINT `chk_eo_credit_change_log_change_amount` CHECK (`change_amount` <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信用变更日志表';

-- ===================================================================
-- 11. 领域事件表 (Outbox 模式)
-- ===================================================================

CREATE TABLE `eo_domain_event` (
    `id`             VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `event_id`       CHAR(36)    NOT NULL COMMENT '事件唯一标识（UUID）',
    `aggregate_type` VARCHAR(100) NOT NULL COMMENT '聚合类型',
    `aggregate_id`   VARCHAR(36) NOT NULL COMMENT '聚合 ID',
    `event_type`     VARCHAR(100) NOT NULL COMMENT '事件类型',
    `payload`        TEXT        DEFAULT NULL COMMENT '事件载荷（JSON）',
    `status`         VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/PUBLISHED/FAILED）',
    `error_message`  VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `created_at`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件创建时间',
    `published_at`   DATETIME(3) DEFAULT NULL COMMENT '事件发布时间',
    `del_flag`       TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    `create_by`      VARCHAR(36) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`      VARCHAR(36) DEFAULT NULL COMMENT '更新人 ID',
    `version`        INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_domain_event_event_id` (`event_id`),
    KEY `idx_eo_domain_event_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_eo_domain_event_status_created` (`status`, `created_at`),
    KEY `idx_eo_domain_event_event_type` (`event_type`),
    CONSTRAINT `chk_eo_domain_event_status` CHECK (`status` IN ('PENDING', 'PUBLISHED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领域事件表';

-- ===================================================================
-- 12. Saga 分布式事务状态表
-- ===================================================================

CREATE TABLE `eo_saga_status` (
    `saga_id`          CHAR(36)    NOT NULL COMMENT 'Saga 实例唯一标识（UUID）',
    `saga_type`        VARCHAR(100) NOT NULL COMMENT 'Saga 类型',
    `state`            VARCHAR(20) NOT NULL COMMENT 'Saga 状态',
    `current_step`     VARCHAR(50) DEFAULT NULL COMMENT '当前执行步骤',
    `payload`          TEXT        DEFAULT NULL COMMENT 'Saga 载荷（JSON）',
    `error_message`    TEXT        DEFAULT NULL COMMENT '错误信息',
    `compensation_log` TEXT        DEFAULT NULL COMMENT '补偿日志（JSON）',
    `retry_count`      INT         NOT NULL DEFAULT 0 COMMENT '重试次数',
    `created_at`       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Saga 创建时间',
    `updated_at`       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Saga 更新时间',
    PRIMARY KEY (`saga_id`),
    KEY `idx_eo_saga_status_type_state` (`saga_type`, `state`),
    KEY `idx_eo_saga_status_state_created` (`state`, `created_at`),
    KEY `idx_eo_saga_status_created_at` (`created_at`),
    CONSTRAINT `chk_eo_saga_status_state` CHECK (
        `state` IN ('PENDING', 'ORDER_CREATED', 'PAYMENT_CREATED', 'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'FAILED')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Saga 分布式事务状态表';

-- ===================================================================
-- 13. 幂等性键表
-- ===================================================================

CREATE TABLE `eo_idempotency_key` (
    `id`              VARCHAR(36)  NOT NULL COMMENT '主键 ID',
    `idempotency_key` VARCHAR(255) NOT NULL COMMENT '幂等性键',
    `user_id`         VARCHAR(36)  NOT NULL COMMENT '用户 ID',
    `request_hash`    VARCHAR(64)  NOT NULL COMMENT '请求哈希',
    `response_data`   TEXT         DEFAULT NULL COMMENT '响应数据（JSON）',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/COMPLETED/FAILED）',
    `expires_at`      DATETIME     NOT NULL COMMENT '过期时间',
    `del_flag`        TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    `create_by`       VARCHAR(36)  DEFAULT NULL COMMENT '创建人 ID',
    `update_by`       VARCHAR(36)  DEFAULT NULL COMMENT '更新人 ID',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_idempotency_key_key` (`idempotency_key`),
    KEY `idx_eo_idempotency_key_user_expires` (`user_id`, `expires_at`),
    CONSTRAINT `chk_eo_idempotency_key_status` CHECK (`status` IN ('PENDING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等性键表';
