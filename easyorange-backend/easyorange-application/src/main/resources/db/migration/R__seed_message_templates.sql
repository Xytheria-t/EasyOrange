-- ===================================================================
-- EasyOrange AI 智能托管平台 - 消息模板种子数据
-- Description: Repeatable Migration - 系统消息模板
-- Type: DML（可重复执行）
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- 设计说明:
--   使用 ON DUPLICATE KEY UPDATE 保证幂等性
--   模板变量使用 ${variableName} 占位符格式
-- ===================================================================

START TRANSACTION;

INSERT INTO `eo_message_template` (
    `id`, `template_code`, `template_name`, `template_type`, `title`, `content`, `variables`, `status`, `create_time`, `update_time`
) VALUES
(1,  'ORDER_CREATED',    '订单创建通知',   'order',  '订单创建成功',   '你已成功下单「${productName}」，订单号：${orderNo}，请尽快完成支付。',      '["productName","orderNo"]',      1, NOW(), NOW()),
(2,  'ORDER_PAID',       '订单支付通知',   'order',  '支付成功',       '订单 ${orderNo} 支付成功，资产方将尽快交付。',                              '["orderNo"]',                    1, NOW(), NOW()),
(3,  'ORDER_SHIPPED',    '订单交付通知',   'order',  '资产已交付',     '订单 ${orderNo} 资产已交付，请确认查收。',                                        '["orderNo"]',                    1, NOW(), NOW()),
(4,  'ORDER_COMPLETED',  '订单完成通知',   'order',  '订单已完成',     '订单 ${orderNo} 已完成，快去评价吧！',                                        '["orderNo"]',                    1, NOW(), NOW()),
(5,  'ORDER_CANCELLED',  '订单取消通知',   'order',  '订单已取消',     '订单 ${orderNo} 已取消，原因：${reason}。',                                   '["orderNo","reason"]',           1, NOW(), NOW()),
(6,  'ORDER_REFUNDED',   '订单退款通知',   'order',  '退款成功',       '订单 ${orderNo} 退款 ${amount} 元已到账。',                                   '["orderNo","amount"]',          1, NOW(), NOW()),
(7,  'SELLER_NEW_ORDER', '资产方新订单通知', 'order',  '收到新订单',     '你的资产「${productName}」有新订单，请尽快处理。订单号：${orderNo}',          '["productName","orderNo"]',     1, NOW(), NOW()),
(8,  'SELLER_PAID',      '资产方收款通知',   'order',  '认领方已付款',     '订单 ${orderNo} 认领方已付款，请尽快交付。',                                  '["orderNo"]',                    1, NOW(), NOW()),
(9,  'PRODUCT_ONLINE',   '资产上架通知',   'system', '资产上架成功',   '你发布的资产「${productName}」已成功上架，祝早日完成流转！',                      '["productName"]',                1, NOW(), NOW()),
(10, 'PRODUCT_OFFLINE',  '资产下架通知',   'system', '资产已下架',     '你的资产「${productName}」已下架，原因：${reason}。',                         '["productName","reason"]',      1, NOW(), NOW()),
(11, 'USER_REGISTER',    '注册欢迎通知',   'system', '欢迎加入',       '欢迎来到 EasyOrange！在这里你可以发布资产，AI 工程化能力帮你估值、写描述，快去发布你的第一件资产吧~',             '[]',                             1, NOW(), NOW()),
(12, 'PRICE_DROP',       '降价提醒',       'system', '收藏资产降价',   '你收藏的资产「${productName}」已降价至 ${price} 元，快去看看吧！',              '["productName","price"]',       1, NOW(), NOW()),
(13, 'AUDIT_SUCCESS',    '审核通过通知',   'audit',  '资产审核通过 🎉',   '您发布的「${productName}」已通过审核，现已上架！',                     '["productName"]',                1, NOW(), NOW()),
(14, 'AUDIT_REJECTED',   '审核驳回通知',   'audit',  '资产审核未通过 ⚠️', '您发布的「${productName}」未通过审核。原因：${reason}。',               '["productName","reason"]',      1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `template_name` = new.`template_name`,
    `content` = new.`content`,
    `variables` = new.`variables`,
    `status` = new.`status`,
    `update_time` = NOW();

COMMIT;