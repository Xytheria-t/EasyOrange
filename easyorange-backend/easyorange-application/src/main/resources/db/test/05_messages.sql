-- ===================================================================
-- EasyOrange 校园二手交易平台 - 测试数据：消息模块
-- 说明：此文件包含开发环境测试消息数据
-- 用途：通过 TestDataLoader 或手动执行加载
-- 注意：依赖 01_users.sql
-- ===================================================================

-- ===================================================================
-- 1. 消息数据（系统消息、私聊、订单消息）
-- ===================================================================

INSERT INTO `eo_message` (
    `id`, `sender_id`, `receiver_id`, `type`, `title`, `content`,
    `is_read`, `read_time`, `business_id`, `conversation_id`,
    `create_time`, `update_time`
) VALUES
-- 系统消息
(1,  NULL, 1, 0, '欢迎加入EasyOrange', '欢迎来到EasyOrange校园二手交易平台！在这里你可以轻松买卖二手商品，快去发布你的第一件商品吧~', 1, NOW() - INTERVAL 89 DAY, NULL, NULL, NOW() - INTERVAL 90 DAY, NOW()),
(2,  NULL, 3, 0, '账号注册成功', '你的账号已成功注册，快去完善个人资料吧！', 1, NOW() - INTERVAL 59 DAY, NULL, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(3,  NULL, 4, 0, '账号注册成功', '你的账号已成功注册，快去完善个人资料吧！', 1, NOW() - INTERVAL 44 DAY, NULL, NULL, NOW() - INTERVAL 45 DAY, NOW()),
(4,  NULL, 1, 0, '商品上架提醒', '你发布的商品「iPhone 14 Pro Max 256G 暗紫色」已成功上架，祝早日售出！', 1, NOW() - INTERVAL 29 DAY, 1, NULL, NOW() - INTERVAL 30 DAY, NOW()),
(5,  NULL, 5, 0, '商品上架提醒', '你发布的商品「小米14 Ultra 16+512 白色」已成功上架，祝早日售出！', 0, NULL, 3, NULL, NOW() - INTERVAL 20 DAY, NOW()),
-- 订单消息
(6,  NULL, 3, 2, '订单创建成功', '你已成功下单「iPhone 14 Pro Max 256G 暗紫色」，请尽快完成支付。订单号：ORD20260101001', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(7,  NULL, 1, 2, '收到新订单', '你的商品「iPhone 14 Pro Max 256G 暗紫色」有新订单，请尽快处理。订单号：ORD20260101001', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(8,  NULL, 3, 2, '支付成功', '订单 ORD20260101001 支付成功，卖家将尽快发货。', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(9,  NULL, 1, 2, '买家已付款', '订单 ORD20260101001 买家已付款，请尽快发货。', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(10, NULL, 3, 2, '订单已完成', '订单 ORD20260101001 已完成，快去评价吧！', 1, NOW() - INTERVAL 58 DAY, 1, NULL, NOW() - INTERVAL 58 DAY, NOW()),
(11, NULL, 7, 2, '订单创建成功', '你已成功下单「小米14 Ultra 16+512 白色」，请尽快完成支付。订单号：ORD20260202001', 1, NOW() - INTERVAL 2 DAY, 5, NULL, NOW() - INTERVAL 2 DAY, NOW()),
(12, NULL, 5, 2, '收到新订单', '你的商品「小米14 Ultra 16+512 白色」有新订单，请尽快处理。', 0, NULL, 5, NULL, NOW() - INTERVAL 2 DAY, NOW()),
-- 私聊消息
(13, 3, 1, 1, NULL, '你好，iPhone还在吗？可以小刀吗？', 1, NOW() - INTERVAL 62 DAY, NULL, 100, NOW() - INTERVAL 62 DAY, NOW()),
(14, 1, 3, 1, NULL, '在的，可以少200，5799出', 1, NOW() - INTERVAL 62 DAY, NULL, 100, NOW() - INTERVAL 62 DAY, NOW()),
(15, 3, 1, 1, NULL, '5700可以吗？我马上付款', 1, NOW() - INTERVAL 61 DAY, NULL, 100, NOW() - INTERVAL 61 DAY, NOW()),
(16, 1, 3, 1, NULL, '行吧，5700成交', 1, NOW() - INTERVAL 61 DAY, NULL, 100, NOW() - INTERVAL 61 DAY, NOW()),
(17, 4, 5, 1, NULL, '小米14 Ultra的屏幕有划痕吗？', 1, NOW() - INTERVAL 22 DAY, NULL, 101, NOW() - INTERVAL 22 DAY, NOW()),
(18, 5, 4, 1, NULL, '没有划痕，贴了膜一直用的', 1, NOW() - INTERVAL 22 DAY, NULL, 101, NOW() - INTERVAL 22 DAY, NOW()),
(19, 6, 1, 1, NULL, 'MacBook Air还在吗？', 0, NULL, NULL, 102, NOW() - INTERVAL 1 DAY, NOW()),
(20, 7, 5, 1, NULL, 'Switch OLED可以面交吗？', 0, NULL, NULL, 103, NOW() - INTERVAL 1 DAY, NOW()),
(21, 8, 6, 1, NULL, 'Sony耳机续航怎么样？', 1, NOW() - INTERVAL 18 DAY, NULL, 104, NOW() - INTERVAL 18 DAY, NOW()),
(22, 6, 8, 1, NULL, '续航很棒，充满能用30小时左右', 1, NOW() - INTERVAL 18 DAY, NULL, 104, NOW() - INTERVAL 18 DAY, NOW()),
(23, 3, 5, 1, NULL, '华为Mate60支持无线充电吗？', 1, NOW() - INTERVAL 25 DAY, NULL, 105, NOW() - INTERVAL 25 DAY, NOW()),
(24, 5, 3, 1, NULL, '支持的，50W无线快充', 1, NOW() - INTERVAL 25 DAY, NULL, 105, NOW() - INTERVAL 25 DAY, NOW()),
(25, 4, 8, 1, NULL, '考研英语红宝书还有吗？', 0, NULL, NULL, 106, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `is_read` = new.`is_read`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 2. 消息模板数据
-- ===================================================================

INSERT INTO `eo_message_template` (
    `id`, `template_code`, `template_name`, `template_type`, `title`, `content`, `variables`, `status`, `create_time`, `update_time`
) VALUES
(1, 'ORDER_CREATED',      '订单创建通知',     'order',   '订单创建成功',   '你已成功下单「${productName}」，订单号：${orderNo}，请尽快完成支付。',       '["productName","orderNo"]',       1, NOW(), NOW()),
(2, 'ORDER_PAID',         '订单支付通知',     'order',   '支付成功',       '订单 ${orderNo} 支付成功，卖家将尽快发货。',                                   '["orderNo"]',                     1, NOW(), NOW()),
(3, 'ORDER_SHIPPED',      '订单发货通知',     'order',   '商品已发货',     '订单 ${orderNo} 已发货，请注意查收。',                                         '["orderNo"]',                     1, NOW(), NOW()),
(4, 'ORDER_COMPLETED',    '订单完成通知',     'order',   '订单已完成',     '订单 ${orderNo} 已完成，快去评价吧！',                                         '["orderNo"]',                     1, NOW(), NOW()),
(5, 'ORDER_CANCELLED',    '订单取消通知',     'order',   '订单已取消',     '订单 ${orderNo} 已取消，原因：${reason}。',                                    '["orderNo","reason"]',            1, NOW(), NOW()),
(6, 'ORDER_REFUNDED',     '订单退款通知',     'order',   '退款成功',       '订单 ${orderNo} 退款 ${amount} 元已到账。',                                    '["orderNo","amount"]',            1, NOW(), NOW()),
(7, 'SELLER_NEW_ORDER',   '卖家新订单通知',   'order',   '收到新订单',     '你的商品「${productName}」有新订单，请尽快处理。订单号：${orderNo}',           '["productName","orderNo"]',       1, NOW(), NOW()),
(8, 'SELLER_PAID',        '卖家收款通知',     'order',   '买家已付款',     '订单 ${orderNo} 买家已付款，请尽快发货。',                                     '["orderNo"]',                     1, NOW(), NOW()),
(9, 'PRODUCT_ONLINE',     '商品上架通知',     'system',  '商品上架成功',   '你发布的商品「${productName}」已成功上架，祝早日售出！',                       '["productName"]',                 1, NOW(), NOW()),
(10, 'PRODUCT_OFFLINE',   '商品下架通知',     'system',  '商品已下架',     '你的商品「${productName}」已下架，原因：${reason}。',                          '["productName","reason"]',        1, NOW(), NOW()),
(11, 'USER_REGISTER',     '注册欢迎通知',     'system',  '欢迎加入',       '欢迎来到EasyOrange校园二手交易平台！在这里你可以轻松买卖二手商品~',            '[]',                              1, NOW(), NOW()),
(12, 'PRICE_DROP',        '降价提醒',         'system',  '收藏商品降价',   '你收藏的商品「${productName}」已降价至 ${price} 元，快去看看吧！',             '["productName","price"]',         1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `template_name` = new.`template_name`,
    `content` = new.`content`,
    `variables` = new.`variables`,
    `status` = new.`status`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 3. 消息订阅数据
-- ===================================================================

INSERT INTO `eo_message_subscription` (
    `id`, `user_id`, `message_type`, `push_channel`, `enabled`, `create_time`, `update_time`
) VALUES
(1,  1, 'order',   'websocket', 1, NOW(), NOW()),
(2,  1, 'system',  'websocket', 1, NOW(), NOW()),
(3,  1, 'chat',    'websocket', 1, NOW(), NOW()),
(4,  3, 'order',   'websocket', 1, NOW(), NOW()),
(5,  3, 'system',  'websocket', 1, NOW(), NOW()),
(6,  3, 'chat',    'websocket', 1, NOW(), NOW()),
(7,  4, 'order',   'websocket', 1, NOW(), NOW()),
(8,  4, 'system',  'websocket', 1, NOW(), NOW()),
(9,  4, 'chat',    'websocket', 1, NOW(), NOW()),
(10, 5, 'order',   'websocket', 1, NOW(), NOW()),
(11, 5, 'system',  'websocket', 1, NOW(), NOW()),
(12, 5, 'chat',    'websocket', 1, NOW(), NOW()),
(13, 6, 'order',   'websocket', 1, NOW(), NOW()),
(14, 6, 'system',  'websocket', 1, NOW(), NOW()),
(15, 6, 'chat',    'websocket', 0, NOW(), NOW()),
(16, 7, 'order',   'websocket', 1, NOW(), NOW()),
(17, 7, 'system',  'websocket', 1, NOW(), NOW()),
(18, 8, 'order',   'websocket', 1, NOW(), NOW()),
(19, 8, 'system',  'websocket', 1, NOW(), NOW()),
(20, 8, 'chat',    'websocket', 1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `enabled` = new.`enabled`,
    `update_time` = new.`update_time`;
