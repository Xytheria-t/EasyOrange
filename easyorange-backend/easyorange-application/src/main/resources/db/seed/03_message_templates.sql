-- ===================================================================
-- EasyOrange - 消息模板种子数据
-- 说明：此文件包含系统启动所需的消息模板配置
-- 用途：通过 Spring Boot data.sql 或手动执行加载
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
ON DUPLICATE KEY UPDATE
    `template_name` = VALUES(`template_name`),
    `content` = VALUES(`content`),
    `variables` = VALUES(`variables`),
    `status` = VALUES(`status`),
    `update_time` = NOW();
