-- ===================================================================
-- EasyOrange 校园二手交易平台 - 测试数据：商品模块
-- 说明：此文件包含开发环境测试商品数据
-- 用途：通过 TestDataLoader 或手动执行加载
-- 注意：依赖 01_users.sql 和 02_categories.sql
-- ===================================================================

-- ===================================================================
-- 1. 商品数据（45个商品：覆盖所有分类、多种状态、不同卖家）
-- ===================================================================

INSERT INTO `eo_product` (
    `id`, `user_id`, `category_id`, `name`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `contact_method`, `tags`, `search_text`, `del_flag`, `create_time`, `update_time`
) VALUES
-- 电子数码 - 手机
(1,  1, 10, 'iPhone 14 Pro Max 256G 暗紫色',       5999.00, 7999.00, 1, 1, 328, 9, '校园内',   '微信: test123',  '苹果,手机,旗舰',         'iPhone 14 Pro Max 256G 暗紫色 苹果 手机 旗舰', 0, NOW() - INTERVAL 30 DAY, NOW()),
(2,  3, 10, '华为 Mate 60 Pro 512G 雅丹黑',        4599.00, 6999.00, 1, 1, 256, 8, '教学楼',   '微信: liming_wx', '华为,手机,拍照',         '华为 Mate 60 Pro 512G 雅丹黑 华为 手机 拍照', 0, NOW() - INTERVAL 25 DAY, NOW()),
(3,  5, 10, '小米14 Ultra 16+512 白色',            3999.00, 5999.00, 1, 1, 189, 7, '宿舍区',   '微信: zhaowei_wx','小米,手机,徕卡',         '小米14 Ultra 16+512 白色 小米 手机 徕卡',     0, NOW() - INTERVAL 20 DAY, NOW()),
(4,  4, 10, 'OPPO Find X7 Ultra 天青蓝',           3299.00, 5499.00, 1, 2, 412, 8, '图书馆',   '微信: wangfang_w','OPPO,手机,影像',         'OPPO Find X7 Ultra 天青蓝 OPPO 手机 影像',   0, NOW() - INTERVAL 60 DAY, NOW()),
-- 电子数码 - 电脑
(5,  1, 11, 'MacBook Air M2 13寸轻薄笔记本',       6499.00, 8999.00, 1, 1, 186, 8, '图书馆',   '微信: test123',  '苹果,电脑,轻薄',         'MacBook Air M2 13寸轻薄笔记本 苹果 电脑 轻薄', 0, NOW() - INTERVAL 28 DAY, NOW()),
(6,  7, 11, 'ThinkPad X1 Carbon Gen11 14寸',       5299.00, 8499.00, 1, 1, 134, 7, '计算机学院','微信: zhouyang_w','ThinkPad,电脑,商务',    'ThinkPad X1 Carbon Gen11 14寸 ThinkPad 电脑 商务', 0, NOW() - INTERVAL 15 DAY, NOW()),
(7,  3, 11, '华为 MateBook X Pro 2024',            6999.00, 9999.00, 1, 0, 56,  9, '宿舍区',   '微信: liming_wx', '华为,电脑,轻薄',         '华为 MateBook X Pro 2024 华为 电脑 轻薄',     0, NOW() - INTERVAL 2 DAY, NOW()),
-- 电子数码 - 耳机音箱
(8,  1, 12, 'AirPods Pro 2 代 全新未拆封',         1299.00, 1899.00, 1, 1, 456, 9, '校园内',   '微信: test123',  '苹果,耳机,降噪',         'AirPods Pro 2 代 全新未拆封 苹果 耳机 降噪', 0, NOW() - INTERVAL 22 DAY, NOW()),
(9,  6, 12, 'Sony WH-1000XM5 头戴式降噪耳机',     1599.00, 2499.00, 1, 1, 198, 8, '宿舍区',   '微信: sunli_wx',  'Sony,耳机,降噪',         'Sony WH-1000XM5 头戴式降噪耳机 Sony 耳机 降噪', 0, NOW() - INTERVAL 18 DAY, NOW()),
(10, 8, 12, 'JBL Charge 5 蓝牙音箱 黑色',          599.00,  899.00,  1, 1, 87,  7, '操场旁',   '微信: chenxiao_w','JBL,音箱,蓝牙',          'JBL Charge 5 蓝牙音箱 黑色 JBL 音箱 蓝牙',   0, NOW() - INTERVAL 12 DAY, NOW()),
-- 电子数码 - 智能穿戴
(11, 1, 13, '小米手环8 NFC版 黑色',                299.00,  349.00,  1, 1, 178, 8, '体育场',   '电话: 13800138001','小米,手环,NFC',          '小米手环8 NFC版 黑色 小米 手环 NFC',          0, NOW() - INTERVAL 35 DAY, NOW()),
(12, 4, 13, 'Apple Watch SE 2代 40mm 星光色',      1499.00, 1999.00, 1, 1, 145, 8, '校园内',   '微信: wangfang_w','苹果,手表,智能',          'Apple Watch SE 2代 40mm 星光色 苹果 手表 智能', 0, NOW() - INTERVAL 10 DAY, NOW()),
-- 电子数码 - 游戏设备
(13, 5, 14, 'Switch OLED 白色 含底座',             1599.00, 2599.00, 1, 1, 267, 8, '宿舍区',   '微信: zhaowei_w','Switch,游戏机,任天堂',     'Switch OLED 白色 含底座 Switch 游戏机 任天堂', 0, NOW() - INTERVAL 40 DAY, NOW()),
(14, 7, 14, 'PS5 光驱版 国行主机',                 2899.00, 3899.00, 1, 1, 312, 7, '宿舍区',   '微信: zhouyang_w','PS5,游戏机,索尼',         'PS5 光驱版 国行主机 PS5 游戏机 索尼',         0, NOW() - INTERVAL 22 DAY, NOW()),
-- 书籍教材 - 教材
(15, 1, 20, '高等数学教材全套 上下册',              89.00,  128.00,  1, 1, 145, 7, '教学楼',   '电话: 13800138001','教材,数学,大一',          '高等数学教材全套 上下册 教材 数学 大一',      0, NOW() - INTERVAL 50 DAY, NOW()),
(16, 4, 20, '线性代数及其应用 第五版',              35.00,  59.00,   1, 1, 89,  6, '数学楼',   '微信: wangfang_w','教材,线代,数学',           '线性代数及其应用 第五版 教材 线代 数学',      0, NOW() - INTERVAL 45 DAY, NOW()),
(17, 6, 20, '大学物理 上下册 第四版',               55.00,  89.00,   1, 1, 67,  5, '物理楼',   '微信: sunli_wx',  '教材,物理,大学',          '大学物理 上下册 第四版 教材 物理 大学',       0, NOW() - INTERVAL 38 DAY, NOW()),
-- 书籍教材 - 考研资料
(18, 3, 21, '考研英语词汇红宝书 2025版',            35.00,  68.00,   1, 1, 234, 6, '考研自习室','微信: liming_wx', '考研,英语,词汇',         '考研英语词汇红宝书 2025版 考研 英语 词汇',   0, NOW() - INTERVAL 20 DAY, NOW()),
(19, 8, 21, '张宇考研数学基础30讲',                45.00,  79.00,   1, 1, 178, 7, '图书馆',   '微信: chenxiao_w','考研,数学,张宇',          '张宇考研数学基础30讲 考研 数学 张宇',        0, NOW() - INTERVAL 15 DAY, NOW()),
(20, 5, 21, '肖秀荣考研政治全套 2025',              89.00,  158.00,  1, 1, 345, 8, '考研自习室','微信: zhaowei_w','考研,政治,肖秀荣',       '肖秀荣考研政治全套 2025 考研 政治 肖秀荣',   0, NOW() - INTERVAL 12 DAY, NOW()),
-- 书籍教材 - 课外读物
(21, 6, 22, '人类简史 从动物到上帝',                28.00,  49.00,   1, 1, 56,  9, '宿舍区',   '微信: sunli_wx',  '课外,历史,畅销',          '人类简史 从动物到上帝 课外 历史 畅销',       0, NOW() - INTERVAL 8 DAY, NOW()),
(22, 8, 22, '算法导论 第三版 中文版',               65.00,  128.00,  1, 1, 123, 7, '计算机学院','微信: chenxiao_w','算法,计算机,经典',       '算法导论 第三版 中文版 算法 计算机 经典',    0, NOW() - INTERVAL 5 DAY, NOW()),
-- 服饰鞋包 - 鞋靴
(23, 3, 30, 'Nike Air Jordan 1 黑白 42码',         699.00,  1299.00, 1, 1, 289, 7, '操场旁',   '微信: liming_wx', 'Nike,球鞋,经典',         'Nike Air Jordan 1 黑白 42码 Nike 球鞋 经典', 0, NOW() - INTERVAL 18 DAY, NOW()),
(24, 4, 30, 'New Balance 990v6 灰色 38码',         899.00,  1499.00, 1, 1, 167, 8, '宿舍区',   '微信: wangfang_w','NB,跑鞋,复古',            'New Balance 990v6 灰色 38码 NB 跑鞋 复古',   0, NOW() - INTERVAL 14 DAY, NOW()),
(25, 7, 30, '阿迪达斯 UltraBoost 22 42码',         299.00,  899.00,  1, 1, 98,  6, '操场旁',   '微信: zhouyang_w','阿迪达斯,跑鞋,Boost',     '阿迪达斯 UltraBoost 22 42码 阿迪达斯 跑鞋 Boost', 0, NOW() - INTERVAL 25 DAY, NOW()),
-- 服饰鞋包 - 服装
(26, 6, 31, '北面冲锋衣 黑色 M码 防水',            399.00,  899.00,  1, 1, 156, 8, '宿舍区',   '微信: sunli_wx',  '北面,外套,户外',          '北面冲锋衣 黑色 M码 防水 北面 外套 户外',    0, NOW() - INTERVAL 16 DAY, NOW()),
(27, 8, 31, '优衣库羽绒服 黑色 L码',               199.00,  499.00,  1, 1, 78,  7, '宿舍区',   '微信: chenxiao_w','优衣库,羽绒服,冬季',      '优衣库羽绒服 黑色 L码 优衣库 羽绒服 冬季',   0, NOW() - INTERVAL 10 DAY, NOW()),
-- 服饰鞋包 - 箱包
(28, 5, 32, 'Nike 运动双肩背包 黑色',              159.00,  299.00,  1, 1, 112, 8, '宿舍区',   '微信: zhaowei_w','Nike,背包,运动',           'Nike 运动双肩背包 黑色 Nike 背包 运动',       0, NOW() - INTERVAL 20 DAY, NOW()),
-- 生活用品 - 宿舍好物
(29, 1, 40, '懒人加湿器 超声波静音款',              89.00,  159.00,  1, 1, 134, 9, '宿舍区',   '微信: test123',  '加湿器,静音,家用',        '懒人加湿器 超声波静音款 加湿器 静音 家用',    0, NOW() - INTERVAL 32 DAY, NOW()),
(30, 4, 40, '小米台灯Pro 护眼阅读灯',              89.00,  149.00,  1, 1, 167, 8, '图书馆',   '微信: wangfang_w','小米,台灯,护眼',           '小米台灯Pro 护眼阅读灯 小米 台灯 护眼',      0, NOW() - INTERVAL 8 DAY, NOW()),
(31, 6, 40, '宿舍收纳架 桌面多层置物架',            39.00,  79.00,   2, 1, 89,  8, '宿舍区',   '微信: sunli_wx',  '收纳,宿舍,置物架',        '宿舍收纳架 桌面多层置物架 收纳 宿舍 置物架',  0, NOW() - INTERVAL 5 DAY, NOW()),
(32, 3, 40, '电热水杯 便携旅行烧水杯 300ml',       69.00,  129.00,  1, 1, 56,  9, '宿舍区',   '微信: liming_wx', '水杯,便携,旅行',          '电热水杯 便携旅行烧水杯 300ml 水杯 便携 旅行', 0, NOW() - INTERVAL 3 DAY, NOW()),
-- 生活用品 - 数码配件
(33, 7, 41, 'Anker 65W 氮化镓充电器 三口',         129.00,  199.00,  1, 1, 78,  9, '宿舍区',   '微信: zhouyang_w','充电器,Anker,快充',       'Anker 65W 氮化镓充电器 三口 充电器 Anker 快充', 0, NOW() - INTERVAL 7 DAY, NOW()),
(34, 8, 41, '绿联 Type-C 扩展坞 7合1',             89.00,  159.00,  1, 1, 45,  8, '计算机学院','微信: chenxiao_w','扩展坞,绿联,Type-C',     '绿联 Type-C 扩展坞 7合1 扩展坞 绿联 Type-C', 0, NOW() - INTERVAL 4 DAY, NOW()),
-- 运动健身 - 健身器材
(35, 5, 50, '健身瑜伽垫加厚加宽防滑',              69.00,  99.00,   1, 1, 98,  8, '体育馆',   '微信: zhaowei_w','瑜伽,健身,防滑',           '健身瑜伽垫加厚加宽防滑 瑜伽 健身 防滑',      0, NOW() - INTERVAL 15 DAY, NOW()),
(36, 3, 50, '可调节哑铃 20kg单只',                  159.00,  299.00,  1, 1, 67,  7, '体育馆',   '微信: liming_wx', '哑铃,健身,力量',          '可调节哑铃 20kg单只 哑铃 健身 力量',         0, NOW() - INTERVAL 10 DAY, NOW()),
-- 运动健身 - 户外运动
(37, 1, 51, '迪卡侬山地自行车 27速',               899.00,  1599.00, 1, 1, 234, 7, '停车场',   '微信: test123',  '自行车,运动,出行',        '迪卡侬山地自行车 27速 自行车 运动 出行',     0, NOW() - INTERVAL 42 DAY, NOW()),
(38, 7, 51, '尤尼克斯羽毛球拍 ARC-7',              289.00,  450.00,  1, 1, 56,  8, '体育馆',   '微信: zhouyang_w','羽毛球,尤尼克斯,运动',    '尤尼克斯羽毛球拍 ARC-7 羽毛球 尤尼克斯 运动', 0, NOW() - INTERVAL 6 DAY, NOW()),
-- 虚拟物品 - 游戏账号
(39, 5, 60, '原神 60级全图鉴账号',                  599.00,  NULL,    1, 1, 345, 10,'线上交易', '微信: zhaowei_w','原神,游戏,账号',           '原神 60级全图鉴账号 原神 游戏 账号',         0, NOW() - INTERVAL 8 DAY, NOW()),
-- 虚拟物品 - 会员卡券
(40, 6, 61, '网易云音乐年卡VIP',                    88.00,   158.00,  1, 1, 67,  10,'线上交易', '微信: sunli_wx',  '网易,音乐,会员',          '网易云音乐年卡VIP 网易 音乐 会员',           0, NOW() - INTERVAL 3 DAY, NOW()),
-- 更多状态：草稿、下架、已售
(41, 1, 10, '一加12 16+512 岩息黑（草稿）',        3499.00, 4299.00, 1, 0, 0,   9, '宿舍区',   '微信: test123',  '一加,手机,旗舰',          '一加12 16+512 岩息黑 一加 手机 旗舰',        0, NOW() - INTERVAL 1 DAY, NOW()),
(42, 3, 11, '联想小新Pro16 2023（已下架）',        3999.00, 5499.00, 1, 3, 234, 7, '宿舍区',   '微信: liming_wx', '联想,电脑,大屏',          '联想小新Pro16 2023 联想 电脑 大屏',          0, NOW() - INTERVAL 90 DAY, NOW()),
(43, 4, 30, 'Vans 经典款帆布鞋 39码（已售出）',    129.00,  359.00,  0, 2, 567, 6, '宿舍区',   '微信: wangfang_w','Vans,帆布鞋,经典',        'Vans 经典款帆布鞋 39码 Vans 帆布鞋 经典',    0, NOW() - INTERVAL 120 DAY, NOW()),
(44, 6, 40, '飞利浦电动牙刷 HX6730（已售出）',     99.00,   199.00,  0, 2, 189, 8, '宿舍区',   '微信: sunli_wx',  '飞利浦,牙刷,电动',        '飞利浦电动牙刷 HX6730 飞利浦 牙刷 电动',     0, NOW() - INTERVAL 100 DAY, NOW()),
(45, 8, 21, '汤家凤考研数学1800题（已售出）',      25.00,   49.00,   0, 2, 234, 5, '图书馆',   '微信: chenxiao_w','考研,数学,汤家凤',        '汤家凤考研数学1800题 考研 数学 汤家凤',      0, NOW() - INTERVAL 80 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `name` = new.`name`,
    `price` = new.`price`,
    `original_price` = new.`original_price`,
    `stock` = new.`stock`,
    `status` = new.`status`,
    `view_count` = new.`view_count`,
    `condition_level` = new.`condition_level`,
    `location` = new.`location`,
    `contact_method` = new.`contact_method`,
    `tags` = new.`tags`,
    `search_text` = new.`search_text`,
    `del_flag` = new.`del_flag`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 2. 商品图片数据
-- ===================================================================

INSERT INTO `eo_product_image` (
    `id`, `product_id`, `image_url`, `sort_order`, `is_main`, `create_time`, `update_time`
) VALUES
-- iPhone 14 Pro Max
(1,  1, 'https://images.unsplash.com/photo-1678685888221-cda773a3acdb?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2,  1, 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 华为 Mate 60 Pro
(3,  2, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(4,  2, 'https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 小米14 Ultra
(5,  3, 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- OPPO Find X7 (已售)
(6,  4, 'https://images.unsplash.com/photo-1574944985070-8f3ebc6b79d2?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- MacBook Air M2
(7,  5, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(8,  5, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- ThinkPad X1
(9,  6, 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 华为 MateBook X Pro (草稿)
(10, 7, 'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- AirPods Pro 2
(11, 8, 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(12, 8, 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- Sony WH-1000XM5
(13, 9, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- JBL Charge 5
(14, 10, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 小米手环8
(15, 11, 'https://images.unsplash.com/photo-1575311373937-040b8e1fd5b6?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Apple Watch SE
(16, 12, 'https://images.unsplash.com/photo-1546868871-af0de0ae72be?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Switch OLED
(17, 13, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(18, 13, 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- PS5
(19, 14, 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 高等数学
(20, 15, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 线性代数
(21, 16, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 大学物理
(22, 17, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 考研英语红宝书
(23, 18, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 张宇考研数学
(24, 19, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 肖秀荣考研政治
(25, 20, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 人类简史
(26, 21, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 算法导论
(27, 22, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Nike AJ1
(28, 23, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(29, 23, 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- NB 990v6
(30, 24, 'https://images.unsplash.com/photo-1539185441755-769473a23570?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 阿迪达斯 UltraBoost
(31, 25, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 北面冲锋衣
(32, 26, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 优衣库羽绒服
(33, 27, 'https://images.unsplash.com/photo-1544923246-77307dd270b2?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Nike 背包
(34, 28, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 加湿器
(35, 29, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 小米台灯
(36, 30, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 收纳架
(37, 31, 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 电热水杯
(38, 32, 'https://images.unsplash.com/photo-1570197788417-0e82375c9ca7?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Anker 充电器
(39, 33, 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 绿联扩展坞
(40, 34, 'https://images.unsplash.com/photo-1625842268584-8f3296236761?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 瑜伽垫
(41, 35, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 哑铃
(42, 36, 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 自行车
(43, 37, 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(44, 37, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 羽毛球拍
(45, 38, 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 原神账号
(46, 39, 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 网易云年卡
(47, 40, 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url` = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main` = new.`is_main`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 3. 商品详情数据
-- ===================================================================

INSERT INTO `eo_product_detail` (
    `product_id`, `description`, `create_time`, `update_time`
) VALUES
(1,  'iPhone 14 Pro Max 256G 暗紫色 国行正品 在保<br><br>【配置】256G存储、暗紫色、灵动岛、全网通5G<br><br>【成色】9成新，轻微使用痕迹，屏幕无划痕，电池健康度92%<br><br>【配件】原装充电线、说明书<br><br>【购买渠道】官网购入，有购买凭证', NOW(), NOW()),
(2,  '华为 Mate 60 Pro 512G 雅丹黑 国行在保<br><br>【配置】512G存储、雅丹黑、昆仑玻璃、卫星通话<br><br>【成色】8成新，屏幕贴膜使用，无划痕<br><br>【配件】原装充电器、数据线、手机壳<br><br>【特色】支持卫星通话、XMAGE影像', NOW(), NOW()),
(3,  '小米14 Ultra 16+512 白色 徕卡影像旗舰<br><br>【配置】骁龙8Gen3、16G+512G、1英寸徕卡主摄<br><br>【成色】7成新，边框有轻微磕碰，屏幕完好<br><br>【配件】原装充电器、手机壳、说明书<br><br>【影像】徕卡Summilux镜头，摄影爱好者首选', NOW(), NOW()),
(4,  'OPPO Find X7 Ultra 天青蓝 哈苏影像<br><br>【配置】骁龙8Gen3、16G+256G、双潜望长焦<br><br>【成色】8成新，屏幕无划痕，功能正常<br><br>【配件】原装充电器、数据线<br><br>【已售出】此商品已售出，仅供展示', NOW(), NOW()),
(5,  'MacBook Air M2 13寸轻薄笔记本 深空灰 16+512<br><br>【配置】M2芯片、16G内存、512G固态硬盘<br><br>【成色】8成新，A面有轻微划痕，功能全部正常<br><br>【配件】原装充电器、包装盒<br><br>【电池循环】仅78次，性能依旧强劲', NOW(), NOW()),
(6,  'ThinkPad X1 Carbon Gen11 14寸商务旗舰<br><br>【配置】i7-1365U、16G内存、512G固态<br><br>【成色】7成新，键盘使用痕迹，屏幕无亮点<br><br>【配件】原装充电器、小红帽<br><br>【适合】商务办公、编程开发', NOW(), NOW()),
(7,  '华为 MateBook X Pro 2024 全新未激活<br><br>【配置】Ultra 9处理器、32G+1T、14.2寸OLED触控屏<br><br>【成色】全新未拆封，还在保修期内<br><br>【配件】原装充电器、包装盒<br><br>【注意】此商品还在草稿状态，暂未上架', NOW(), NOW()),
(8,  'AirPods Pro 2 代 全新未拆封 正品保障<br><br>【型号】AirPods Pro (第二代) 带MagSafe充电盒<br><br>【成色】全新未拆封，原厂塑封完整<br><br>【配件】原装耳机、充电盒、充电线、说明书<br><br>【保修】未激活，在保', NOW(), NOW()),
(9,  'Sony WH-1000XM5 头戴式降噪耳机 银色<br><br>【型号】WH-1000XM5 行货正品<br><br>【成色】8成新，耳罩无破损，降噪功能正常<br><br>【配件】原装收纳盒、充电线、飞机转接头<br><br>【续航】约30小时，支持快充', NOW(), NOW()),
(10, 'JBL Charge 5 蓝牙音箱 黑色 防水便携<br><br>【型号】JBL Charge 5 IP67防水<br><br>【成色】7成新，外观有轻微磨损，音质正常<br><br>【配件】原装充电线<br><br>【续航】约20小时，支持PartyBoost串联', NOW(), NOW()),
(11, '小米手环8 NFC版 黑色 原装配件齐全<br><br>【功能】NFC门禁、NFC支付、心率监测、睡眠监测、血氧饱和度<br><br>【成色】8成新，屏幕无划痕，腕带无破损<br><br>【配件】原装充电器、说明书<br><br>【电池续航】约7天', NOW(), NOW()),
(12, 'Apple Watch SE 2代 40mm 星光色 GPS版<br><br>【配置】S8芯片、GPS版、星光色铝金属表壳<br><br>【成色】8成新，屏幕无划痕，表带有使用痕迹<br><br>【配件】原装磁力充电线、运动表带<br><br>【功能】心率监测、运动追踪、消息通知', NOW(), NOW()),
(13, 'Switch OLED 白色 含底座手柄 64G<br><br>【配置】7寸OLED屏、64G存储、白色Joy-Con手柄<br><br>【成色】8成新，屏幕无划痕，底座完好<br><br>【配件】主机、底座、手柄、充电线、腕带<br><br>【备注】掌机/主机双模式，适合宿舍娱乐', NOW(), NOW()),
(14, 'PS5 光驱版 国行主机 含手柄<br><br>【配置】光驱版、825G SSD、DualSense手柄<br><br>【成色】7成新，主机有轻微灰尘，运行正常<br><br>【配件】主机、手柄、HDMI线、电源线、底座<br><br>【备注】国行版本，可备份港服账号', NOW(), NOW()),
(15, '高等数学教材全套 上下册第七版 同济大学<br><br>【版本】第七版，同济大学数学系编<br><br>【成色】7成新，笔记较多但不影响阅读<br><br>【内容】上册：函数与极限、导数与微分等；下册：积分、空间解析几何等<br><br>【适合】大一新生考研复习', NOW(), NOW()),
(16, '线性代数及其应用 第五版 戴维·C·莱<br><br>【版本】第五版，机械工业出版社<br><br>【成色】6成新，有少量笔记和划线<br><br>【内容】线性方程组、矩阵、行列式、特征值等<br><br>【适合】工科学生、考研数学', NOW(), NOW()),
(17, '大学物理 上下册 第四版 张三慧<br><br>【版本】第四版，清华大学出版社<br><br>【成色】5成新，有较多笔记，部分页面折角<br><br>【内容】力学、热学、电磁学、光学、量子物理<br><br>【适合】理工科大学物理课程', NOW(), NOW()),
(18, '考研英语词汇红宝书 2025版 考研必备<br><br>【版本】2025最新版，包含5500+核心词汇<br><br>【成色】6成新，有部分笔记划线<br><br>【内容】词汇分类、真题例句、记忆方法<br><br>【适合】考研英语一、英语二备考', NOW(), NOW()),
(19, '张宇考研数学基础30讲 2025版<br><br>【作者】张宇，北京理工大学出版社<br><br>【成色】7成新，有少量笔记<br><br>【内容】高数+线代+概率论基础知识点全覆盖<br><br>【适合】考研数学一/二/三基础阶段', NOW(), NOW()),
(20, '肖秀荣考研政治全套 2025 含精讲精练+1000题+真题<br><br>【版本】2025最新版全套四本<br><br>【成色】8成新，精讲精练有笔记，1000题未做<br><br>【内容】精讲精练、1000题、讲真题、形势与政策<br><br>【适合】考研政治全程复习', NOW(), NOW()),
(21, '人类简史 从动物到上帝 尤瓦尔·赫拉利<br><br>【版本】中信出版社 精装版<br><br>【成色】9成新，几乎全新，无折痕<br><br>【内容】从认知革命到科学革命，重新审视人类历史<br><br>【推荐】豆瓣9.1分，全球畅销书', NOW(), NOW()),
(22, '算法导论 第三版 中文版 MIT经典教材<br><br>【版本】第三版，机械工业出版社<br><br>【成色】7成新，书脊有折痕，内容完整<br><br>【内容】排序、图算法、动态规划、NP完全性等<br><br>【适合】计算机专业学生、算法竞赛、面试准备', NOW(), NOW()),
(23, 'Nike Air Jordan 1 High OG 黑白熊猫 42码<br><br>【型号】AJ1 High OG 黑白配色<br><br>【尺码】42码（US 8.5）<br><br>【成色】7成新，鞋底轻微磨损，鞋面干净<br><br>【来源】得物购入，正品保障', NOW(), NOW()),
(24, 'New Balance 990v6 灰色 38码 美产<br><br>【型号】990v6 元祖灰 美国制造<br><br>【尺码】38码（US 6.5）<br><br>【成色】8成新，鞋底磨损正常，鞋面干净<br><br>【特点】猪巴革+网面鞋面，ENCAP中底', NOW(), NOW()),
(25, '阿迪达斯 UltraBoost 22 42码 黑色<br><br>【型号】UltraBoost 22 跑鞋<br><br>【尺码】42码，适合脚长26cm左右<br><br>【成色】6成新，鞋底磨损明显，仍可穿着<br><br>【技术】Boost中底，Continental橡胶外底', NOW(), NOW()),
(26, '北面冲锋衣 黑色 M码 防水透气<br><br>【型号】The North Face DryVent冲锋衣<br><br>【尺码】M码，适合身高170-175cm<br><br>【成色】8成新，无破损，拉链顺畅<br><br>【功能】防水透气、可调节帽、多口袋设计', NOW(), NOW()),
(27, '优衣库羽绒服 黑色 L码 轻薄保暖<br><br>【型号】优衣库Ultra Light Down 短款<br><br>【尺码】L码，适合身高170-175cm<br><br>【成色】7成新，有轻微使用痕迹，保暖性良好<br><br>【特点】可收纳便携、90%白鸭绒填充', NOW(), NOW()),
(28, 'Nike 运动双肩背包 黑色 30L大容量<br><br>【容量】30L，可放置15.6寸笔记本<br><br>【成色】8成新，整体清洁，无明显破损<br><br>【功能】多口袋设计、透气背垫、电脑隔层<br><br>【适用】上学、旅行、健身', NOW(), NOW()),
(29, '懒人加湿器 超声波静音款 4.5L大容量<br><br>【容量】4.5L，持续加湿12小时<br><br>【特点】超声波静音技术，运行时噪音低于35dB<br><br>【成色】9成新，使用时间不超过1个月<br><br>【功能】智能恒湿、定时关机、过夜保护', NOW(), NOW()),
(30, '小米台灯Pro 护眼阅读灯 国AA级<br><br>【功能】国AA级照度、无频闪、蓝光防护<br><br>【成色】8成新，使用约3个月<br><br>【特点】智能调光、定时关灯、米家APP控制<br><br>【适用】学生学习、办公阅读', NOW(), NOW()),
(31, '宿舍收纳架 桌面多层置物架 白色 2个装<br><br>【尺寸】每层30*20*15cm，共3层<br><br>【材质】加厚碳钢+环保喷塑<br><br>【成色】8成新，无锈迹，承重良好<br><br>【适合】宿舍桌面收纳、化妆品/书籍/杂物', NOW(), NOW()),
(32, '电热水杯 便携旅行烧水杯 300ml 白色<br><br>【容量】300ml，304不锈钢内胆<br><br>【功率】300W，宿舍可用不跳闸<br><br>【成色】9成新，使用不到1个月<br><br>【功能】多段温控、防干烧、自动断电', NOW(), NOW()),
(33, 'Anker 65W 氮化镓充电器 三口 黑色<br><br>【规格】2C1A三口输出，最大65W<br><br>【技术】GaN II氮化镓，体积小巧<br><br>【成色】9成新，几乎全新<br><br>【兼容】MacBook/iPad/手机/Switch全兼容', NOW(), NOW()),
(34, '绿联 Type-C 扩展坞 7合1 银色<br><br>【接口】HDMI 4K+3*USB3.0+SD/TF+PD100W<br><br>【成色】8成新，接口完好，无松动<br><br>【兼容】MacBook/笔记本/平板通用<br><br>【适合】外接显示器、U盘读取、充电扩展', NOW(), NOW()),
(35, '健身瑜伽垫加厚加宽防滑 TPE材质 送收纳绑带<br><br>【尺寸】183cm*80cm*10mm加宽加厚款<br><br>【材质】TPE环保材质，无异味<br><br>【成色】8成新，表面防滑性能良好<br><br>【适用】瑜伽、普拉提、健身训练', NOW(), NOW()),
(36, '可调节哑铃 20kg单只 快调式 黑色<br><br>【重量】2.5-20kg可调节，15档快调<br><br>【成色】7成新，调节机构顺畅<br><br>【特点】一秒切换重量，节省空间<br><br>【适合】宿舍健身、家庭训练', NOW(), NOW()),
(37, '迪卡侬山地自行车 Rockrider ST520 27速<br><br>【型号】Rockrider ST520 铝合金车架<br><br>【变速】27速禧玛诺变速系统<br><br>【成色】7成新，轮胎磨损正常<br><br>【配置】前后碟刹、避震前叉、水壶架', NOW(), NOW()),
(38, '尤尼克斯羽毛球拍 ARC-7 进攻型<br><br>【型号】Arcsaber 7 全碳素<br><br>【规格】4UG5，适合进攻打法<br><br>【成色】8成新，线已断需重新穿线<br><br>【适合】中高级球友，进攻型打法', NOW(), NOW()),
(39, '原神 60级全图鉴账号 官服<br><br>【等级】冒险等阶60级<br><br>【角色】全图鉴含限定角色，满命座多角色<br><br>【武器】5星武器20+，含限定武器<br><br>【备注】官服可改绑，安全交易', NOW(), NOW()),
(40, '网易云音乐年卡VIP 官方直充<br><br>【类型】黑胶VIP年卡<br><br>【功能】无损音质、免广告、专属皮肤<br><br>【有效期】充值后12个月<br><br>【备注】官方直充，秒到账', NOW(), NOW()),
(41, '一加12 16+512 岩息黑 全新未拆封（草稿）<br><br>【配置】骁龙8Gen3、16G+512G、2K东方屏<br><br>【成色】全新未拆封<br><br>【配件】原装全套<br><br>【注意】此商品还在编辑中，暂未上架', NOW(), NOW()),
(42, '联想小新Pro16 2023 锐龙版（已下架）<br><br>【配置】R7-7840HS、16G+1T、16寸2.5K屏<br><br>【成色】7成新，键盘有使用痕迹<br><br>【下架原因】已换新电脑，暂时下架<br><br>【备注】如需购买可私信重新上架', NOW(), NOW()),
(43, 'Vans 经典款帆布鞋 39码 黑白（已售出）<br><br>【型号】Old Skool 经典黑白<br><br>【尺码】39码<br><br>【成色】6成新，已售出<br><br>【备注】此商品已售出', NOW(), NOW()),
(44, '飞利浦电动牙刷 HX6730 粉色（已售出）<br><br>【型号】Sonicare HX6730 声波震动<br><br>【成色】8成新，已售出<br><br>【功能】3种清洁模式、2分钟智能计时<br><br>【备注】此商品已售出', NOW(), NOW()),
(45, '汤家凤考研数学1800题 2024版（已售出）<br><br>【作者】汤家凤<br><br>【成色】5成新，大量笔记，已售出<br><br>【内容】基础篇+提高篇全覆盖<br><br>【备注】此商品已售出', NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `description` = new.`description`,
    `update_time` = new.`update_time`;
