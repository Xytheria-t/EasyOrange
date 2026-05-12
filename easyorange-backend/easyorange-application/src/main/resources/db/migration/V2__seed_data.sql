-- ===================================================================
-- EasyOrange 校园二手交易平台 - 种子数据
-- Version: V2
-- 职责: 插入系统启动所需的基础数据（纯 DML）
-- 注意: 使用 MySQL 8.0.20+ 推荐的别名语法替代已弃用的 VALUES() 函数
-- ===================================================================

-- ===================================================================
-- 1. 分类数据 - 一级分类
-- ===================================================================

INSERT INTO `eo_category` (
    `id`, `name`, `parent_id`, `level`, `sort_order`, `status`,
    `del_flag`, `create_time`, `update_time`
) VALUES
(1, '电子数码', 0, 1, 1, 1, 0, NOW(), NOW()),
(2, '书籍教材', 0, 1, 2, 1, 0, NOW(), NOW()),
(3, '服饰鞋包', 0, 1, 3, 1, 0, NOW(), NOW()),
(4, '生活用品', 0, 1, 4, 1, 0, NOW(), NOW()),
(5, '运动健身', 0, 1, 5, 1, 0, NOW(), NOW()),
(6, '虚拟物品', 0, 1, 6, 1, 0, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `name` = new.`name`,
    `sort_order` = new.`sort_order`,
    `status` = new.`status`,
    `update_time` = NOW();

-- ===================================================================
-- 2. 分类数据 - 二级分类
-- ===================================================================

INSERT INTO `eo_category` (
    `id`, `name`, `parent_id`, `level`, `sort_order`, `status`,
    `del_flag`, `create_time`, `update_time`
) VALUES
(10, '手机',     1, 2, 1, 1, 0, NOW(), NOW()),
(11, '电脑',     1, 2, 2, 1, 0, NOW(), NOW()),
(12, '耳机音箱', 1, 2, 3, 1, 0, NOW(), NOW()),
(13, '智能穿戴', 1, 2, 4, 1, 0, NOW(), NOW()),
(14, '游戏设备', 1, 2, 5, 1, 0, NOW(), NOW()),
(20, '教材',     2, 2, 1, 1, 0, NOW(), NOW()),
(21, '考研资料', 2, 2, 2, 1, 0, NOW(), NOW()),
(22, '课外读物', 2, 2, 3, 1, 0, NOW(), NOW()),
(30, '鞋靴',     3, 2, 1, 1, 0, NOW(), NOW()),
(31, '服装',     3, 2, 2, 1, 0, NOW(), NOW()),
(32, '箱包',     3, 2, 3, 1, 0, NOW(), NOW()),
(40, '宿舍好物', 4, 2, 1, 1, 0, NOW(), NOW()),
(41, '数码配件', 4, 2, 2, 1, 0, NOW(), NOW()),
(50, '健身器材', 5, 2, 1, 1, 0, NOW(), NOW()),
(51, '户外运动', 5, 2, 2, 1, 0, NOW(), NOW()),
(60, '游戏账号', 6, 2, 1, 1, 0, NOW(), NOW()),
(61, '会员卡券', 6, 2, 2, 1, 0, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `name` = new.`name`,
    `parent_id` = new.`parent_id`,
    `level` = new.`level`,
    `sort_order` = new.`sort_order`,
    `status` = new.`status`,
    `update_time` = NOW();

-- ===================================================================
-- 3. 支付配置数据
-- ===================================================================

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

-- ===================================================================
-- 4. 消息模板数据
-- ===================================================================

INSERT INTO `eo_message_template` (
    `id`, `template_code`, `template_name`, `template_type`, `title`, `content`, `variables`, `status`, `create_time`, `update_time`
) VALUES
(1, 'ORDER_CREATED',    '订单创建通知',   'order', '订单创建成功',   '你已成功下单「${productName}」，订单号：${orderNo}，请尽快完成支付。',      '["productName","orderNo"]',       1, NOW(), NOW()),
(2, 'ORDER_PAID',       '订单支付通知',   'order', '支付成功',       '订单 ${orderNo} 支付成功，卖家将尽快发货。',                                  '["orderNo"]',                     1, NOW(), NOW()),
(3, 'ORDER_SHIPPED',    '订单发货通知',   'order', '商品已发货',     '订单 ${orderNo} 已发货，请注意查收。',                                        '["orderNo"]',                     1, NOW(), NOW()),
(4, 'ORDER_COMPLETED',  '订单完成通知',   'order', '订单已完成',     '订单 ${orderNo} 已完成，快去评价吧！',                                        '["orderNo"]',                     1, NOW(), NOW()),
(5, 'ORDER_CANCELLED',  '订单取消通知',   'order', '订单已取消',     '订单 ${orderNo} 已取消，原因：${reason}。',                                   '["orderNo","reason"]',            1, NOW(), NOW()),
(6, 'ORDER_REFUNDED',   '订单退款通知',   'order', '退款成功',       '订单 ${orderNo} 退款 ${amount} 元已到账。',                                   '["orderNo","amount"]',           1, NOW(), NOW()),
(7, 'SELLER_NEW_ORDER', '卖家新订单通知', 'order', '收到新订单',     '你的商品「${productName}」有新订单，请尽快处理。订单号：${orderNo}',          '["productName","orderNo"]',      1, NOW(), NOW()),
(8, 'SELLER_PAID',     '卖家收款通知',    'order', '买家已付款',     '订单 ${orderNo} 买家已付款，请尽快发货。',                                    '["orderNo"]',                     1, NOW(), NOW()),
(9, 'PRODUCT_ONLINE',   '商品上架通知',   'system','商品上架成功',   '你发布的商品「${productName}」已成功上架，祝早日售出！',                      '["productName"]',                 1, NOW(), NOW()),
(10, 'PRODUCT_OFFLINE', '商品下架通知',   'system','商品已下架',     '你的商品「${productName}」已下架，原因：${reason}。',                         '["productName","reason"]',       1, NOW(), NOW()),
(11, 'USER_REGISTER',   '注册欢迎通知',   'system','欢迎加入',       '欢迎来到EasyOrange校园二手交易平台！在这里你可以轻松买卖二手商品~',             '[]',                              1, NOW(), NOW()),
(12, 'PRICE_DROP',      '降价提醒',        'system','收藏商品降价',   '你收藏的商品「${productName}」已降价至 ${price} 元，快去看看吧！',              '["productName","price"]',        1, NOW(), NOW()),
(13, 'AUDIT_SUCCESS',   '审核通过通知',     'audit', '商品审核通过 🎉',   '您发布的「${productName}」已通过审核，现已上架销售！',        '["productName"]',                 1, NOW(), NOW()),
(14, 'AUDIT_REJECTED', '审核驳回通知',      'audit', '商品审核未通过 ⚠️', '您发布的「${productName}」未通过审核。原因：${reason}。', '["productName","reason"]',           1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `template_name` = new.`template_name`,
    `content` = new.`content`,
    `variables` = new.`variables`,
    `status` = new.`status`,
    `update_time` = NOW();
