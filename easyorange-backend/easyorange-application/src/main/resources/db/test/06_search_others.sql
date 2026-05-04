-- ===================================================================
-- EasyOrange 校园二手交易平台 - 测试数据：搜索与其他模块
-- 说明：此文件包含开发环境测试搜索历史、热门关键词等数据
-- 用途：通过 TestDataLoader 或手动执行加载
-- 注意：依赖 01_users.sql
-- ===================================================================

-- ===================================================================
-- 1. 搜索历史数据
-- ===================================================================

INSERT INTO `eo_search_history` (
    `id`, `user_id`, `keyword`, `search_time`, `create_time`, `update_time`
) VALUES
(1,  1, 'iPhone',       NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY, NOW()),
(2,  1, 'MacBook',      NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 28 DAY, NOW()),
(3,  1, 'AirPods',      NOW() - INTERVAL 22 DAY, NOW() - INTERVAL 22 DAY, NOW()),
(4,  3, '华为手机',     NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY, NOW()),
(5,  3, '考研资料',     NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY, NOW()),
(6,  4, 'Nike球鞋',     NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, NOW()),
(7,  4, '耳机降噪',     NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY, NOW()),
(8,  5, '小米手机',     NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY, NOW()),
(9,  5, '自行车',       NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY, NOW()),
(10, 6, '瑜伽垫',       NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, NOW()),
(11, 6, '冲锋衣',       NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY, NOW()),
(12, 7, 'PS5',          NOW() - INTERVAL 22 DAY, NOW() - INTERVAL 22 DAY, NOW()),
(13, 7, 'ThinkPad',     NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, NOW()),
(14, 8, '考研政治',     NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY, NOW()),
(15, 8, '算法',         NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY, NOW()),
(16, 1, 'Switch',       NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY, NOW()),
(17, 3, '原神',         NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 8 DAY, NOW()),
(18, 5, '哑铃',         NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY, NOW()),
(19, 6, '加湿器',       NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY, NOW()),
(20, 7, '羽毛球拍',     NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `search_time` = new.`search_time`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 2. 热门关键词数据
-- ===================================================================

INSERT INTO `eo_hot_keyword` (
    `id`, `keyword`, `search_count`, `last_search_time`, `create_time`, `update_time`
) VALUES
(1,  'iPhone',     356, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 90 DAY, NOW()),
(2,  'MacBook',    289, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 90 DAY, NOW()),
(3,  'AirPods',    234, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 85 DAY, NOW()),
(4,  '考研资料',   198, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 60 DAY, NOW()),
(5,  'Nike',       167, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 80 DAY, NOW()),
(6,  'Switch',     156, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 70 DAY, NOW()),
(7,  '华为手机',   145, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 60 DAY, NOW()),
(8,  '耳机降噪',   134, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 55 DAY, NOW()),
(9,  '自行车',     123, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 50 DAY, NOW()),
(10, 'PS5',        112, NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 45 DAY, NOW()),
(11, '瑜伽垫',     98,  NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 40 DAY, NOW()),
(12, '教材',       89,  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 50 DAY, NOW()),
(13, '小米手机',   87,  NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 45 DAY, NOW()),
(14, '冲锋衣',     76,  NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 30 DAY, NOW()),
(15, '原神',       72,  NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 25 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `search_count` = new.`search_count`,
    `last_search_time` = new.`last_search_time`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 3. 支付渠道配置数据
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
    `update_time` = new.`update_time`;

-- ===================================================================
-- 4. 商品举报数据
-- ===================================================================

INSERT INTO `eo_product_report` (
    `id`, `product_id`, `reporter_id`, `reason`, `status`, `handle_result`,
    `create_time`, `update_time`
) VALUES
(1, 39, 4,  '虚拟物品交易风险，建议平台审核',   1, '已核实，商品信息真实，暂不处理', NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY),
(2, 70, 14, '游戏账号交易存在安全隐患',         1, '已提醒卖家完善交易保障说明',     NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
(3, 3,  17, '商品描述与实际不符，成色虚标',     0, NULL,                             NOW() - INTERVAL 1 DAY, NOW()),
(4, 25, 12, '价格明显高于市场价，疑似哄抬价格', 2, '经核实价格合理，已忽略',         NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 13 DAY),
(5, 42, 8,  '已下架商品仍在搜索结果中显示',     1, '已优化搜索索引，下架商品不再展示', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 18 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `status` = new.`status`,
    `handle_result` = new.`handle_result`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 5. 操作日志数据
-- ===================================================================

INSERT INTO `eo_oper_log` (
    `title`, `business_type`, `method`, `request_method`, `operator_type`,
    `oper_name`, `oper_url`, `oper_ip`, `oper_location`,
    `status`, `cost_time`, `oper_time`
) VALUES
('用户管理', 1, 'UserController.register()',  'POST', 1, 'testuser',  '/api/user/register', '192.168.1.100', '校园网', 0, 156, NOW() - INTERVAL 90 DAY),
('商品管理', 2, 'ProductController.create()',  'POST', 1, 'testuser',  '/api/product/create', '192.168.1.100', '校园网', 0, 89,  NOW() - INTERVAL 30 DAY),
('商品管理', 2, 'ProductController.create()',  'POST', 1, 'liming',    '/api/product/create', '10.0.0.55',    '图书馆', 0, 112, NOW() - INTERVAL 25 DAY),
('订单管理', 2, 'OrderController.create()',    'POST', 1, 'liming',    '/api/order/create',   '10.0.0.55',    '图书馆', 0, 234, NOW() - INTERVAL 60 DAY),
('支付管理', 2, 'PaymentController.pay()',     'POST', 1, 'liming',    '/api/payment/pay',    '10.0.0.55',    '图书馆', 0, 567, NOW() - INTERVAL 60 DAY),
('商品管理', 3, 'ProductController.update()',  'PUT',  1, 'testuser',  '/api/product/1',      '192.168.1.100', '校园网', 0, 67,  NOW() - INTERVAL 20 DAY),
('用户管理', 2, 'UserController.login()',      'POST', 1, 'wangfang',  '/api/user/login',     '172.16.0.23',  '宿舍区', 0, 45,  NOW() - INTERVAL 45 DAY),
('订单管理', 2, 'OrderController.create()',    'POST', 1, 'wangfang',  '/api/order/create',   '172.16.0.23',  '宿舍区', 0, 189, NOW() - INTERVAL 55 DAY),
('收藏管理', 2, 'FavoriteController.add()',    'POST', 1, 'zhaowei',   '/api/favorite/add',   '192.168.2.34', '教学楼', 0, 34,  NOW() - INTERVAL 8 DAY),
('商品管理', 1, 'ProductController.search()',  'GET',  1, 'sunli',     '/api/product/search', '192.168.2.34', '教学楼', 0, 78,  NOW() - INTERVAL 15 DAY),
('用户管理', 2, 'UserController.login()',      'POST', 1, 'huangjie',  '/api/user/login',     '10.0.1.100',   '宿舍区', 0, 38,  NOW() - INTERVAL 180 DAY),
('商品管理', 2, 'ProductController.create()',  'POST', 1, 'huangjie',  '/api/product/create', '10.0.1.100',   '宿舍区', 0, 95,  NOW() - INTERVAL 14 DAY),
('订单管理', 2, 'OrderController.cancel()',    'PUT',  1, 'wanghai',   '/api/order/20/cancel','10.0.0.88',    '图书馆', 0, 123, NOW() - INTERVAL 7 DAY),
('支付管理', 2, 'PaymentController.refund()',  'POST', 1, 'zhangmei',  '/api/payment/refund', '172.16.0.45',  '宿舍区', 0, 456, NOW() - INTERVAL 6 DAY),
('系统管理', 4, 'AdminController.exportLog()',  'GET',  2, 'admin',     '/api/admin/log/export','10.0.0.1',    '服务器', 0, 2345, NOW() - INTERVAL 3 DAY);
