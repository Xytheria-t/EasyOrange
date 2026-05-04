-- ===================================================================
-- EasyOrange 校园二手交易平台 - 测试数据：订单与支付模块
-- 说明：此文件包含开发环境测试订单和支付数据
-- 用途：通过 TestDataLoader 或手动执行加载
-- 注意：依赖 01_users.sql 和 03_products.sql
-- ===================================================================

-- ===================================================================
-- 1. 收藏数据
-- ===================================================================

INSERT INTO `eo_favorite` (
    `id`, `user_id`, `product_id`, `create_time`, `update_time`
) VALUES
(1,  3, 1,  NOW() - INTERVAL 20 DAY, NOW()),
(2,  3, 5,  NOW() - INTERVAL 18 DAY, NOW()),
(3,  4, 8,  NOW() - INTERVAL 15 DAY, NOW()),
(4,  4, 13, NOW() - INTERVAL 12 DAY, NOW()),
(5,  4, 23, NOW() - INTERVAL 10 DAY, NOW()),
(6,  5, 2,  NOW() - INTERVAL 8 DAY, NOW()),
(7,  5, 9,  NOW() - INTERVAL 7 DAY, NOW()),
(8,  5, 37, NOW() - INTERVAL 5 DAY, NOW()),
(9,  6, 1,  NOW() - INTERVAL 6 DAY, NOW()),
(10, 6, 26, NOW() - INTERVAL 4 DAY, NOW()),
(11, 6, 35, NOW() - INTERVAL 3 DAY, NOW()),
(12, 7, 14, NOW() - INTERVAL 5 DAY, NOW()),
(13, 7, 22, NOW() - INTERVAL 3 DAY, NOW()),
(14, 8, 18, NOW() - INTERVAL 4 DAY, NOW()),
(15, 8, 20, NOW() - INTERVAL 2 DAY, NOW()),
(16, 8, 24, NOW() - INTERVAL 1 DAY, NOW()),
(17, 1, 2,  NOW() - INTERVAL 10 DAY, NOW()),
(18, 1, 13, NOW() - INTERVAL 5 DAY, NOW()),
(19, 3, 14, NOW() - INTERVAL 3 DAY, NOW()),
(20, 5, 39, NOW() - INTERVAL 1 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `update_time` = new.`update_time`;

-- ===================================================================
-- 2. 订单数据（多种状态覆盖）
-- ===================================================================

INSERT INTO `eo_order` (
    `id`, `order_no`, `buyer_id`, `seller_id`, `product_id`, `amount`,
    `status`, `payment_status`, `address`, `phone`, `remark`,
    `create_time`, `update_time`
) VALUES
-- 已完成订单
(1, 'ORD20260101001', 3, 1, 1,  5999.00, 3, 1, '东校区3号楼302室', '13800138003', '请中午送达', NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 58 DAY),
(2, 'ORD20260102001', 4, 1, 8,  1299.00, 3, 1, '南校区7号楼518室', '13800138004', '',             NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 53 DAY),
(3, 'ORD20260105001', 5, 3, 2,  4599.00, 3, 1, '西校区1号楼205室', '13800138005', '周末自取',     NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 48 DAY),
-- 待付款订单
(4, 'ORD20260201001', 6, 1, 5,  6499.00, 0, 0, '北校区2号楼410室', '13800138006', '',             NOW() - INTERVAL 1 DAY, NOW()),
-- 待发货订单
(5, 'ORD20260202001', 7, 5, 3,  3999.00, 1, 1, '东校区5号楼601室', '13800138007', '尽快发货',     NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
-- 待收货订单
(6, 'ORD20260203001', 8, 6, 9,  1599.00, 2, 1, '南校区9号楼303室', '13800138008', '',             NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
-- 已取消订单
(7, 'ORD20260110001', 3, 7, 6,  5299.00, 4, 0, '西校区3号楼108室', '13800138003', '',             NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 39 DAY),
-- 已退款订单
(8, 'ORD20260115001', 4, 3, 18, 35.00,   5, 2, '东校区3号楼302室', '13800138004', '书本有缺页',   NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 33 DAY),
-- 更多已完成订单
(9, 'ORD20260120001', 5, 1, 11, 299.00,  3, 1, '北校区2号楼410室', '13800138005', '',             NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY),
(10,'ORD20260125001', 6, 4, 24, 899.00,  3, 1, '南校区7号楼518室', '13800138006', '试穿后确认',   NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 23 DAY),
(11,'ORD20260204001', 3, 5, 13, 1599.00, 3, 1, '东校区5号楼601室', '13800138003', '',             NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY),
(12,'ORD20260205001', 8, 7, 38, 289.00,  3, 1, '南校区9号楼303室', '13800138008', '',             NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 5 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `status` = new.`status`,
    `payment_status` = new.`payment_status`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 3. 支付记录数据
-- ===================================================================

INSERT INTO `eo_payment` (
    `id`, `payment_no`, `order_id`, `user_id`, `amount`, `refunded_amount`,
    `payment_method`, `status`, `create_time`, `update_time`
) VALUES
(1,  'PAY20260101001', 1,  3, 5999.00, 0,     1, 1, NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 58 DAY),
(2,  'PAY20260102001', 2,  4, 1299.00, 0,     2, 1, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 53 DAY),
(3,  'PAY20260105001', 3,  5, 4599.00, 0,     1, 1, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 48 DAY),
(4,  'PAY20260202001', 5,  7, 3999.00, 0,     1, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
(5,  'PAY20260203001', 6,  8, 1599.00, 0,     2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
(6,  'PAY20260115001', 8,  4, 35.00,   35.00, 1, 2, NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 33 DAY),
(7,  'PAY20260120001', 9,  5, 299.00,  0,     1, 1, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY),
(8,  'PAY20260125001', 10, 6, 899.00,  0,     2, 1, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 23 DAY),
(9,  'PAY20260204001', 11, 3, 1599.00, 0,     1, 1, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY),
(10, 'PAY20260205001', 12, 8, 289.00,  0,     1, 1, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 5 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `status` = new.`status`,
    `refunded_amount` = new.`refunded_amount`,
    `update_time` = new.`update_time`;
