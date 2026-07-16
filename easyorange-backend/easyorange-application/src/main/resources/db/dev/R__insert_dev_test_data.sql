-- ===================================================================
-- EasyOrange AI 智能托管平台 - 开发环境测试数据
-- 说明：仅在 dev profile 中通过 classpath:db/dev 加载
-- ===================================================================

-- ===================================================================
-- 1. 用户数据（10个用户：不同类型、性别、状态）
-- ===================================================================

INSERT INTO `eo_user` (
    `user_id`, `username`, `password`, `user_type`, `nick_name`,
    `sex`, `status`, `del_flag`, `email`, `phone`, `student_id`,
    `real_name`, `avatar`, `create_time`, `update_time`
) VALUES
(1,  'testuser',  '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '测试用户',   0, 0, 0, 'testuser@example.com',    '13800138001', '2023001', '张三',   'https://picsum.photos/seed/avatar1/100/100',  NOW() - INTERVAL 90 DAY, NOW()),
(2,  'admin',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '02', '管理员',     1, 0, 0, 'admin@example.com',       '13800138002', NULL,      '李管理', 'https://picsum.photos/seed/avatar2/100/100',  NOW() - INTERVAL 120 DAY, NOW()),
(3,  'liming',    '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '黎明',       1, 0, 0, 'liming@example.com',      '13800138003', '2023002', '黎明',   'https://picsum.photos/seed/avatar3/100/100',  NOW() - INTERVAL 60 DAY, NOW()),
(4,  'wangfang',  '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '小芳同学',   2, 0, 0, 'wangfang@example.com',    '13800138004', '2023003', '王芳',   'https://picsum.photos/seed/avatar4/100/100',  NOW() - INTERVAL 45 DAY, NOW()),
(5,  'zhaowei',   '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '赵伟',       1, 0, 0, 'zhaowei@example.com',     '13800138005', '2023004', '赵伟',   'https://picsum.photos/seed/avatar5/100/100',  NOW() - INTERVAL 30 DAY, NOW()),
(6,  'sunli',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '孙丽',       2, 0, 0, 'sunli@example.com',       '13800138006', '2023005', '孙丽',   'https://picsum.photos/seed/avatar6/100/100',  NOW() - INTERVAL 20 DAY, NOW()),
(7,  'zhouyang',  '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '周洋',       1, 0, 0, 'zhouyang@example.com',    '13800138007', '2023006', '周洋',   'https://picsum.photos/seed/avatar7/100/100',  NOW() - INTERVAL 15 DAY, NOW()),
(8,  'chenxiao',  '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '陈晓',       2, 0, 0, 'chenxiao@example.com',    '13800138008', '2023007', '陈晓',   'https://picsum.photos/seed/avatar8/100/100',  NOW() - INTERVAL 10 DAY, NOW()),
(9,  'lockeduser','$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '被锁定用户', 0, 2, 0, 'locked@example.com',      '13800138009', '2023008', '刘锁',   NULL,                                           NOW() - INTERVAL 5 DAY, NOW()),
(10, 'disableduser','$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK','01', '已禁用用户', 0, 1, 0, 'disabled@example.com',    '13800138010', '2023009', '吴禁',   NULL,                                           NOW() - INTERVAL 3 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `nick_name` = new.`nick_name`,
    `status` = new.`status`,
    `del_flag` = new.`del_flag`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 3. 商品数据（30+商品：覆盖所有分类、多种状态、不同资产方）
-- ===================================================================

INSERT INTO `eo_product` (
    `id`, `user_id`, `category_id`, `name`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `contact_method`, `tags`, `search_text`, `del_flag`, `create_time`, `update_time`
) VALUES
-- 电子数码 - 手机
(1,  1, 10, 'iPhone 14 Pro Max 256G 暗紫色',       5999.00, 7999.00, 1, 1, 328, 9, '同城',   '微信: test123',  '苹果,手机,旗舰',         'iPhone 14 Pro Max 256G 暗紫色 苹果 手机 旗舰', 0, NOW() - INTERVAL 30 DAY, NOW()),
(2,  3, 10, '华为 Mate 60 Pro 512G 雅丹黑',        4599.00, 6999.00, 1, 1, 256, 8, '教学楼',   '微信: liming_wx', '华为,手机,拍照',         '华为 Mate 60 Pro 512G 雅丹黑 华为 手机 拍照', 0, NOW() - INTERVAL 25 DAY, NOW()),
(3,  5, 10, '小米14 Ultra 16+512 白色',            3999.00, 5999.00, 1, 1, 189, 7, '同城',   '微信: zhaowei_wx','小米,手机,徕卡',         '小米14 Ultra 16+512 白色 小米 手机 徕卡',     0, NOW() - INTERVAL 20 DAY, NOW()),
(4,  4, 10, 'OPPO Find X7 Ultra 天青蓝',           3299.00, 5499.00, 1, 2, 412, 8, '图书馆',   '微信: wangfang_w','OPPO,手机,影像',         'OPPO Find X7 Ultra 天青蓝 OPPO 手机 影像',   0, NOW() - INTERVAL 60 DAY, NOW()),
-- 电子数码 - 电脑
(5,  1, 11, 'MacBook Air M2 13寸轻薄笔记本',       6499.00, 8999.00, 1, 1, 186, 8, '图书馆',   '微信: test123',  '苹果,电脑,轻薄',         'MacBook Air M2 13寸轻薄笔记本 苹果 电脑 轻薄', 0, NOW() - INTERVAL 28 DAY, NOW()),
(6,  7, 11, 'ThinkPad X1 Carbon Gen11 14寸',       5299.00, 8499.00, 1, 1, 134, 7, '计算机学院','微信: zhouyang_w','ThinkPad,电脑,商务',    'ThinkPad X1 Carbon Gen11 14寸 ThinkPad 电脑 商务', 0, NOW() - INTERVAL 15 DAY, NOW()),
(7,  3, 11, '华为 MateBook X Pro 2024',            6999.00, 9999.00, 1, 0, 56,  9, '同城',   '微信: liming_wx', '华为,电脑,轻薄',         '华为 MateBook X Pro 2024 华为 电脑 轻薄',     0, NOW() - INTERVAL 2 DAY, NOW()),
-- 电子数码 - 耳机音箱
(8,  1, 12, 'AirPods Pro 2 代 全新未拆封',         1299.00, 1899.00, 1, 1, 456, 9, '同城',   '微信: test123',  '苹果,耳机,降噪',         'AirPods Pro 2 代 全新未拆封 苹果 耳机 降噪', 0, NOW() - INTERVAL 22 DAY, NOW()),
(9,  6, 12, 'Sony WH-1000XM5 头戴式降噪耳机',     1599.00, 2499.00, 1, 1, 198, 8, '同城',   '微信: sunli_wx',  'Sony,耳机,降噪',         'Sony WH-1000XM5 头戴式降噪耳机 Sony 耳机 降噪', 0, NOW() - INTERVAL 18 DAY, NOW()),
(10, 8, 12, 'JBL Charge 5 蓝牙音箱 黑色',          599.00,  899.00,  1, 1, 87,  7, '操场旁',   '微信: chenxiao_w','JBL,音箱,蓝牙',          'JBL Charge 5 蓝牙音箱 黑色 JBL 音箱 蓝牙',   0, NOW() - INTERVAL 12 DAY, NOW()),
-- 电子数码 - 智能穿戴
(11, 1, 13, '小米手环8 NFC版 黑色',                299.00,  349.00,  1, 1, 178, 8, '体育场',   '电话: 13800138001','小米,手环,NFC',          '小米手环8 NFC版 黑色 小米 手环 NFC',          0, NOW() - INTERVAL 35 DAY, NOW()),
(12, 4, 13, 'Apple Watch SE 2代 40mm 星光色',      1499.00, 1999.00, 1, 1, 145, 8, '同城',   '微信: wangfang_w','苹果,手表,智能',          'Apple Watch SE 2代 40mm 星光色 苹果 手表 智能', 0, NOW() - INTERVAL 10 DAY, NOW()),
-- 电子数码 - 游戏设备
(13, 5, 14, 'Switch OLED 白色 含底座',             1599.00, 2599.00, 1, 1, 267, 8, '同城',   '微信: zhaowei_w','Switch,游戏机,任天堂',     'Switch OLED 白色 含底座 Switch 游戏机 任天堂', 0, NOW() - INTERVAL 40 DAY, NOW()),
(14, 7, 14, 'PS5 光驱版 国行主机',                 2899.00, 3899.00, 1, 1, 312, 7, '同城',   '微信: zhouyang_w','PS5,游戏机,索尼',         'PS5 光驱版 国行主机 PS5 游戏机 索尼',         0, NOW() - INTERVAL 22 DAY, NOW()),
-- 书籍教材 - 教材
(15, 1, 20, '高等数学教材全套 上下册',              89.00,  128.00,  1, 1, 145, 7, '教学楼',   '电话: 13800138001','教材,数学,大一',          '高等数学教材全套 上下册 教材 数学 大一',      0, NOW() - INTERVAL 50 DAY, NOW()),
(16, 4, 20, '线性代数及其应用 第五版',              35.00,  59.00,   1, 1, 89,  6, '数学楼',   '微信: wangfang_w','教材,线代,数学',           '线性代数及其应用 第五版 教材 线代 数学',      0, NOW() - INTERVAL 45 DAY, NOW()),
(17, 6, 20, '大学物理 上下册 第四版',               55.00,  89.00,   1, 1, 67,  5, '物理楼',   '微信: sunli_wx',  '教材,物理,大学',          '大学物理 上下册 第四版 教材 物理 大学',       0, NOW() - INTERVAL 38 DAY, NOW()),
-- 书籍教材 - 考研资料
(18, 3, 21, '考研英语词汇红宝书 2025版',            35.00,  68.00,   1, 1, 234, 6, '线下交易点','微信: liming_wx', '考研,英语,词汇',         '考研英语词汇红宝书 2025版 考研 英语 词汇',   0, NOW() - INTERVAL 20 DAY, NOW()),
(19, 8, 21, '张宇考研数学基础30讲',                45.00,  79.00,   1, 1, 178, 7, '图书馆',   '微信: chenxiao_w','考研,数学,张宇',          '张宇考研数学基础30讲 考研 数学 张宇',        0, NOW() - INTERVAL 15 DAY, NOW()),
(20, 5, 21, '肖秀荣考研政治全套 2025',              89.00,  158.00,  1, 1, 345, 8, '线下交易点','微信: zhaowei_w','考研,政治,肖秀荣',       '肖秀荣考研政治全套 2025 考研 政治 肖秀荣',   0, NOW() - INTERVAL 12 DAY, NOW()),
-- 书籍教材 - 课外读物
(21, 6, 22, '人类简史 从动物到上帝',                28.00,  49.00,   1, 1, 56,  9, '同城',   '微信: sunli_wx',  '课外,历史,畅销',          '人类简史 从动物到上帝 课外 历史 畅销',       0, NOW() - INTERVAL 8 DAY, NOW()),
(22, 8, 22, '算法导论 第三版 中文版',               65.00,  128.00,  1, 1, 123, 7, '计算机学院','微信: chenxiao_w','算法,计算机,经典',       '算法导论 第三版 中文版 算法 计算机 经典',    0, NOW() - INTERVAL 5 DAY, NOW()),
-- 服饰鞋包 - 鞋靴
(23, 3, 30, 'Nike Air Jordan 1 黑白 42码',         699.00,  1299.00, 1, 1, 289, 7, '操场旁',   '微信: liming_wx', 'Nike,球鞋,经典',         'Nike Air Jordan 1 黑白 42码 Nike 球鞋 经典', 0, NOW() - INTERVAL 18 DAY, NOW()),
(24, 4, 30, 'New Balance 990v6 灰色 38码',         899.00,  1499.00, 1, 1, 167, 8, '同城',   '微信: wangfang_w','NB,跑鞋,复古',            'New Balance 990v6 灰色 38码 NB 跑鞋 复古',   0, NOW() - INTERVAL 14 DAY, NOW()),
(25, 7, 30, '阿迪达斯 UltraBoost 22 42码',         299.00,  899.00,  1, 1, 98,  6, '操场旁',   '微信: zhouyang_w','阿迪达斯,跑鞋,Boost',     '阿迪达斯 UltraBoost 22 42码 阿迪达斯 跑鞋 Boost', 0, NOW() - INTERVAL 25 DAY, NOW()),
-- 服饰鞋包 - 服装
(26, 6, 31, '北面冲锋衣 黑色 M码 防水',            399.00,  899.00,  1, 1, 156, 8, '同城',   '微信: sunli_wx',  '北面,外套,户外',          '北面冲锋衣 黑色 M码 防水 北面 外套 户外',    0, NOW() - INTERVAL 16 DAY, NOW()),
(27, 8, 31, '优衣库羽绒服 黑色 L码',               199.00,  499.00,  1, 1, 78,  7, '同城',   '微信: chenxiao_w','优衣库,羽绒服,冬季',      '优衣库羽绒服 黑色 L码 优衣库 羽绒服 冬季',   0, NOW() - INTERVAL 10 DAY, NOW()),
-- 服饰鞋包 - 箱包
(28, 5, 32, 'Nike 运动双肩背包 黑色',              159.00,  299.00,  1, 1, 112, 8, '同城',   '微信: zhaowei_w','Nike,背包,运动',           'Nike 运动双肩背包 黑色 Nike 背包 运动',       0, NOW() - INTERVAL 20 DAY, NOW()),
-- 生活用品 - 生活用品
(29, 1, 40, '懒人加湿器 超声波静音款',              89.00,  159.00,  1, 1, 134, 9, '同城',   '微信: test123',  '加湿器,静音,家用',        '懒人加湿器 超声波静音款 加湿器 静音 家用',    0, NOW() - INTERVAL 32 DAY, NOW()),
(30, 4, 40, '小米台灯Pro 护眼阅读灯',              89.00,  149.00,  1, 1, 167, 8, '图书馆',   '微信: wangfang_w','小米,台灯,护眼',           '小米台灯Pro 护眼阅读灯 小米 台灯 护眼',      0, NOW() - INTERVAL 8 DAY, NOW()),
(31, 6, 40, '生活收纳架 桌面多层置物架',            39.00,  79.00,   2, 1, 89,  8, '同城',   '微信: sunli_wx',  '收纳,生活,置物架',        '生活收纳架 桌面多层置物架 收纳 生活 置物架',  0, NOW() - INTERVAL 5 DAY, NOW()),
(32, 3, 40, '电热水杯 便携旅行烧水杯 300ml',       69.00,  129.00,  1, 1, 56,  9, '同城',   '微信: liming_wx', '水杯,便携,旅行',          '电热水杯 便携旅行烧水杯 300ml 水杯 便携 旅行', 0, NOW() - INTERVAL 3 DAY, NOW()),
-- 生活用品 - 数码配件
(33, 7, 41, 'Anker 65W 氮化镓充电器 三口',         129.00,  199.00,  1, 1, 78,  9, '同城',   '微信: zhouyang_w','充电器,Anker,快充',       'Anker 65W 氮化镓充电器 三口 充电器 Anker 快充', 0, NOW() - INTERVAL 7 DAY, NOW()),
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
(40, 6, 61, '网易云音乐年卡VIP',                    88.00,   158.00,  1, 1, 67, 10,'线上交易', '微信: sunli_wx',  '网易,音乐,会员',          '网易云音乐年卡VIP 网易 音乐 会员',           0, NOW() - INTERVAL 3 DAY, NOW()),
(46, 3, 61, '哔哩哔哩大会员 剩余10个月',            45.00,   98.00,   1, 1, 128, 10,'线上交易', '微信: liming_wx', 'B站,会员,视频',           '哔哩哔哩大会员 剩余10个月 B站 会员 视频',    0, NOW() - INTERVAL 2 DAY, NOW()),
-- 虚拟物品 - 数字素材
(47, 8, 62, 'Figma设计系统UI组件包 500+矢量',       129.00,  299.00,  1, 1, 89,  10,'线上交易', '微信: chenxiao_w','设计,素材,Figma',         'Figma设计系统UI组件包 500+矢量 设计 素材 Figma', 0, NOW() - INTERVAL 1 DAY, NOW()),
(48, 5, 62, 'Notion个人版会员 剩余8个月',           199.00,  388.00,  1, 1, 256, 10,'线上交易', '微信: zhaowei_w','Notion,会员,效率',        'Notion个人版会员 剩余8个月 Notion 会员 效率', 0, NOW() - INTERVAL 1 DAY, NOW()),
-- 更多状态：草稿、下架、已售
(41, 1, 10, '一加12 16+512 岩息黑（草稿）',        3499.00, 4299.00, 1, 0, 0,   9, '同城',   '微信: test123',  '一加,手机,旗舰',          '一加12 16+512 岩息黑 一加 手机 旗舰',        0, NOW() - INTERVAL 1 DAY, NOW()),
(42, 3, 11, '联想小新Pro16 2023（已下架）',        3999.00, 5499.00, 1, 3, 234, 7, '同城',   '微信: liming_wx', '联想,电脑,大屏',          '联想小新Pro16 2023 联想 电脑 大屏',          0, NOW() - INTERVAL 90 DAY, NOW()),
(43, 4, 30, 'Vans 经典款帆布鞋 39码（已售出）',    129.00,  359.00,  0, 2, 567, 6, '同城',   '微信: wangfang_w','Vans,帆布鞋,经典',        'Vans 经典款帆布鞋 39码 Vans 帆布鞋 经典',    0, NOW() - INTERVAL 120 DAY, NOW()),
(44, 6, 40, '飞利浦电动牙刷 HX6730（已售出）',     99.00,   199.00,  0, 2, 189, 8, '同城',   '微信: sunli_wx',  '飞利浦,牙刷,电动',        '飞利浦电动牙刷 HX6730 飞利浦 牙刷 电动',     0, NOW() - INTERVAL 100 DAY, NOW()),
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
-- 4. 商品图片数据
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
(47, 40, 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 哔哩哔哩大会员
(48, 46, 'https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Figma设计素材包
(49, 47, 'https://images.unsplash.com/photo-1561070791-2526d30994b5?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Notion会员
(50, 48, 'https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url` = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main` = new.`is_main`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 5. 商品详情数据
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
(13, 'Switch OLED 白色 含底座手柄 64G<br><br>【配置】7寸OLED屏、64G存储、白色Joy-Con手柄<br><br>【成色】8成新，屏幕无划痕，底座完好<br><br>【配件】主机、底座、手柄、充电线、腕带<br><br>【备注】掌机/主机双模式，适合生活娱乐', NOW(), NOW()),
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
(31, '生活收纳架 桌面多层置物架 白色 2个装<br><br>【尺寸】每层30*20*15cm，共3层<br><br>【材质】加厚碳钢+环保喷塑<br><br>【成色】8成新，无锈迹，承重良好<br><br>【适合】生活桌面收纳、化妆品/书籍/杂物', NOW(), NOW()),
(32, '电热水杯 便携旅行烧水杯 300ml 白色<br><br>【容量】300ml，304不锈钢内胆<br><br>【功率】300W，生活可用不跳闸<br><br>【成色】9成新，使用不到1个月<br><br>【功能】多段温控、防干烧、自动断电', NOW(), NOW()),
(33, 'Anker 65W 氮化镓充电器 三口 黑色<br><br>【规格】2C1A三口输出，最大65W<br><br>【技术】GaN II氮化镓，体积小巧<br><br>【成色】9成新，几乎全新<br><br>【兼容】MacBook/iPad/手机/Switch全兼容', NOW(), NOW()),
(34, '绿联 Type-C 扩展坞 7合1 银色<br><br>【接口】HDMI 4K+3*USB3.0+SD/TF+PD100W<br><br>【成色】8成新，接口完好，无松动<br><br>【兼容】MacBook/笔记本/平板通用<br><br>【适合】外接显示器、U盘读取、充电扩展', NOW(), NOW()),
(35, '健身瑜伽垫加厚加宽防滑 TPE材质 送收纳绑带<br><br>【尺寸】183cm*80cm*10mm加宽加厚款<br><br>【材质】TPE环保材质，无异味<br><br>【成色】8成新，表面防滑性能良好<br><br>【适用】瑜伽、普拉提、健身训练', NOW(), NOW()),
(36, '可调节哑铃 20kg单只 快调式 黑色<br><br>【重量】2.5-20kg可调节，15档快调<br><br>【成色】7成新，调节机构顺畅<br><br>【特点】一秒切换重量，节省空间<br><br>【适合】生活健身、家庭训练', NOW(), NOW()),
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

-- ===================================================================
-- 6. 收藏数据
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
-- 7. 订单数据（多种状态覆盖）
-- ===================================================================

INSERT INTO `eo_order` (
    `id`, `order_no`, `buyer_id`, `seller_id`, `total_amount`,
    `status`, `payment_status`, `address`, `phone`, `remark`,
    `create_time`, `update_time`
) VALUES
-- 已完成订单
(1, 'ORD20260101001', 3, 1, 5999.00, 3, 1, '东校区3号楼302室', '13800138003', '请中午送达', NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 58 DAY),
(2, 'ORD20260102001', 4, 1, 1299.00, 3, 1, '南校区7号楼518室', '13800138004', '',             NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 53 DAY),
(3, 'ORD20260105001', 5, 3, 4599.00, 3, 1, '西校区1号楼205室', '13800138005', '周末自取',     NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 48 DAY),
-- 待付款订单
(4, 'ORD20260201001', 6, 1, 6499.00, 0, 0, '北校区2号楼410室', '13800138006', '',             NOW() - INTERVAL 1 DAY, NOW()),
-- 待发货订单
(5, 'ORD20260202001', 7, 5, 3999.00, 1, 1, '东校区5号楼601室', '13800138007', '尽快发货',     NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
-- 待收货订单
(6, 'ORD20260203001', 8, 6, 1599.00, 2, 1, '南校区9号楼303室', '13800138008', '',             NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
-- 已取消订单
(7, 'ORD20260110001', 3, 7, 5299.00, 4, 0, '西校区3号楼108室', '13800138003', '',             NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 39 DAY),
-- 已退款订单
(8, 'ORD20260115001', 4, 3, 35.00,   5, 2, '东校区3号楼302室', '13800138004', '书本有缺页',   NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 33 DAY),
-- 更多已完成订单
(9, 'ORD20260120001', 5, 1, 299.00,  3, 1, '北校区2号楼410室', '13800138005', '',             NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY),
(10,'ORD20260125001', 6, 4, 899.00,  3, 1, '南校区7号楼518室', '13800138006', '试穿后确认',   NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 23 DAY),
(11,'ORD20260204001', 3, 5, 1599.00, 3, 1, '东校区5号楼601室', '13800138003', '',             NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY),
(12,'ORD20260205001', 8, 7, 289.00,  3, 1, '南校区9号楼303室', '13800138008', '',             NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 5 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `status` = new.`status`,
    `payment_status` = new.`payment_status`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 7.5 订单行项数据（与 eo_order 一一对应，关联商品和价格）
-- ===================================================================
INSERT INTO `eo_order_item` (
    `id`, `order_id`, `product_id`, `product_snapshot`, `unit_price`, `quantity`, `subtotal`,
    `create_time`, `update_time`, `del_flag`, `version`
) VALUES
(100, 1,  1,  '{}', 5999.00, 1, 5999.00, NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 58 DAY, 0, 1),
(200, 2,  8,  '{}', 1299.00, 1, 1299.00, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 53 DAY, 0, 1),
(300, 3,  2,  '{}', 4599.00, 1, 4599.00, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 48 DAY, 0, 1),
(400, 4,  5,  '{}', 6499.00, 1, 6499.00, NOW() - INTERVAL 1 DAY,  NOW(),                    0, 1),
(500, 5,  3,  '{}', 3999.00, 1, 3999.00, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 1 DAY,  0, 1),
(600, 6,  9,  '{}', 1599.00, 1, 1599.00, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 3 DAY,  0, 1),
(700, 7,  6,  '{}', 5299.00, 1, 5299.00, NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 39 DAY, 0, 1),
(800, 8,  18, '{}', 35.00,   1, 35.00,   NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 33 DAY, 0, 1),
(900, 9,  11, '{}', 299.00,  1, 299.00,  NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY, 0, 1),
(1000,10, 24, '{}', 899.00,  1, 899.00,  NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 23 DAY, 0, 1),
(1100,11, 13, '{}', 1599.00, 1, 1599.00, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY,  0, 1),
(1200,12, 38, '{}', 289.00,  1, 289.00,  NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 5 DAY,  0, 1)
AS new
ON DUPLICATE KEY UPDATE
    `product_snapshot` = new.`product_snapshot`,
    `unit_price` = new.`unit_price`,
    `subtotal` = new.`subtotal`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 8. 支付记录数据
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

-- ===================================================================
-- 9. 消息数据（系统消息、私聊、订单消息）
-- ===================================================================

INSERT INTO `eo_message` (
    `id`, `sender_id`, `receiver_id`, `type`, `title`, `content`,
    `is_read`, `read_time`, `business_id`, `conversation_id`,
    `create_time`, `update_time`
) VALUES
-- 系统消息
(1,  NULL, 1, 0, '欢迎加入EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布资产，AI 工程化能力帮你估值、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 89 DAY, NULL, NULL, NOW() - INTERVAL 90 DAY, NOW()),
(2,  NULL, 3, 0, '账号注册成功', '你的账号已成功注册，快去完善个人资料吧！', 1, NOW() - INTERVAL 59 DAY, NULL, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(3,  NULL, 4, 0, '账号注册成功', '你的账号已成功注册，快去完善个人资料吧！', 1, NOW() - INTERVAL 44 DAY, NULL, NULL, NOW() - INTERVAL 45 DAY, NOW()),
(4,  NULL, 1, 0, '商品上架提醒', '你发布的商品「iPhone 14 Pro Max 256G 暗紫色」已成功上架，祝早日售出！', 1, NOW() - INTERVAL 29 DAY, 1, NULL, NOW() - INTERVAL 30 DAY, NOW()),
(5,  NULL, 5, 0, '商品上架提醒', '你发布的商品「小米14 Ultra 16+512 白色」已成功上架，祝早日售出！', 0, NULL, 3, NULL, NOW() - INTERVAL 20 DAY, NOW()),
-- 订单消息
(6,  NULL, 3, 2, '订单创建成功', '你已成功下单「iPhone 14 Pro Max 256G 暗紫色」，请尽快完成支付。订单号：ORD20260101001', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(7,  NULL, 1, 2, '收到新订单', '你的商品「iPhone 14 Pro Max 256G 暗紫色」有新订单，请尽快处理。订单号：ORD20260101001', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(8,  NULL, 3, 2, '支付成功', '订单 ORD20260101001 支付成功，资产方将尽快发货。', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
(9,  NULL, 1, 2, '认领方已付款', '订单 ORD20260101001 认领方已付款，请尽快发货。', 1, NOW() - INTERVAL 60 DAY, 1, NULL, NOW() - INTERVAL 60 DAY, NOW()),
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
-- 10. 搜索历史数据
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
-- 11. 热门关键词数据
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
-- 12. 消息模板数据
-- ===================================================================

INSERT INTO `eo_message_template` (
    `id`, `template_code`, `template_name`, `template_type`, `title`, `content`, `variables`, `status`, `create_time`, `update_time`
) VALUES
(1, 'ORDER_CREATED',      '订单创建通知',     'order',   '订单创建成功',   '你已成功下单「${productName}」，订单号：${orderNo}，请尽快完成支付。',       '["productName","orderNo"]',       1, NOW(), NOW()),
(2, 'ORDER_PAID',         '订单支付通知',     'order',   '支付成功',       '订单 ${orderNo} 支付成功，资产方将尽快发货。',                                   '["orderNo"]',                     1, NOW(), NOW()),
(3, 'ORDER_SHIPPED',      '订单发货通知',     'order',   '商品已发货',     '订单 ${orderNo} 已发货，请注意查收。',                                         '["orderNo"]',                     1, NOW(), NOW()),
(4, 'ORDER_COMPLETED',    '订单完成通知',     'order',   '订单已完成',     '订单 ${orderNo} 已完成，快去评价吧！',                                         '["orderNo"]',                     1, NOW(), NOW()),
(5, 'ORDER_CANCELLED',    '订单取消通知',     'order',   '订单已取消',     '订单 ${orderNo} 已取消，原因：${reason}。',                                    '["orderNo","reason"]',            1, NOW(), NOW()),
(6, 'ORDER_REFUNDED',     '订单退款通知',     'order',   '退款成功',       '订单 ${orderNo} 退款 ${amount} 元已到账。',                                    '["orderNo","amount"]',            1, NOW(), NOW()),
(7, 'SELLER_NEW_ORDER',   '资产方新订单通知',   'order',   '收到新订单',     '你的资产「${productName}」有新订单，请尽快处理。订单号：${orderNo}',           '["productName","orderNo"]',       1, NOW(), NOW()),
(8, 'SELLER_PAID',        '资产方收款通知',     'order',   '认领方已付款',     '订单 ${orderNo} 认领方已付款，请尽快发货。',                                     '["orderNo"]',                     1, NOW(), NOW()),
(9, 'PRODUCT_ONLINE',     '商品上架通知',     'system',  '商品上架成功',   '你发布的商品「${productName}」已成功上架，祝早日售出！',                       '["productName"]',                 1, NOW(), NOW()),
(10, 'PRODUCT_OFFLINE',   '商品下架通知',     'system',  '商品已下架',     '你的商品「${productName}」已下架，原因：${reason}。',                          '["productName","reason"]',        1, NOW(), NOW()),
(11, 'USER_REGISTER',     '注册欢迎通知',     'system',  '欢迎加入',       '欢迎来到 EasyOrange！在这里你可以发布资产，AI 工程化能力帮你估值、写描述，快去发布你的第一件资产吧~',            '[]',                              1, NOW(), NOW()),
(12, 'PRICE_DROP',        '降价提醒',         'system',  '收藏商品降价',   '你收藏的商品「${productName}」已降价至 ${price} 元，快去看看吧！',             '["productName","price"]',         1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `template_name` = new.`template_name`,
    `content` = new.`content`,
    `variables` = new.`variables`,
    `status` = new.`status`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 13. 消息订阅数据
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

-- ===================================================================
-- 14. 支付渠道配置数据
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
-- 15. 补充用户数据（8个新用户：不同专业、年级、活跃度）
-- ===================================================================

INSERT INTO `eo_user` (
    `user_id`, `username`, `password`, `user_type`, `nick_name`,
    `sex`, `status`, `del_flag`, `email`, `phone`, `student_id`,
    `real_name`, `avatar`, `create_time`, `update_time`
) VALUES
(11, 'huangjie',  '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '黄杰学长',   1, 0, 0, 'huangjie@example.com',    '13800138011', '2021001', '黄杰', 'https://picsum.photos/seed/avatar11/100/100', NOW() - INTERVAL 180 DAY, NOW()),
(12, 'liuyan',    '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '刘燕',       2, 0, 0, 'liuyan@example.com',      '13800138012', '2022001', '刘燕', 'https://picsum.photos/seed/avatar12/100/100', NOW() - INTERVAL 120 DAY, NOW()),
(13, 'wanghai',   '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '王海',       1, 0, 0, 'wanghai@example.com',     '13800138013', '2022002', '王海', 'https://picsum.photos/seed/avatar13/100/100', NOW() - INTERVAL 90 DAY, NOW()),
(14, 'zhangmei',  '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '张梅',       2, 0, 0, 'zhangmei@example.com',    '13800138014', '2023003', '张梅', 'https://picsum.photos/seed/avatar14/100/100', NOW() - INTERVAL 60 DAY, NOW()),
(15, 'liguang',   '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '李光',       1, 0, 0, 'liguang@example.com',     '13800138015', '2023004', '李光', 'https://picsum.photos/seed/avatar15/100/100', NOW() - INTERVAL 45 DAY, NOW()),
(16, 'xujia',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '许佳',       2, 0, 0, 'xujia@example.com',       '13800138016', '2023005', '许佳', 'https://picsum.photos/seed/avatar16/100/100', NOW() - INTERVAL 30 DAY, NOW()),
(17, 'qianlei',   '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '钱磊',       1, 0, 0, 'qianlei@example.com',     '13800138017', '2024001', '钱磊', 'https://picsum.photos/seed/avatar17/100/100', NOW() - INTERVAL 14 DAY, NOW()),
(18, 'hanxue',    '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '韩雪',       2, 0, 0, 'hanxue@example.com',      '13800138018', '2024002', '韩雪', 'https://picsum.photos/seed/avatar18/100/100', NOW() - INTERVAL 7 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `nick_name` = new.`nick_name`,
    `status` = new.`status`,
    `del_flag` = new.`del_flag`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 16. 补充商品数据（25个新商品：更多品类、价格区间、资产方）
-- ===================================================================

INSERT INTO `eo_product` (
    `id`, `user_id`, `category_id`, `name`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `contact_method`, `tags`, `search_text`, `del_flag`, `create_time`, `update_time`
) VALUES
-- 电子数码 - 手机（更多品牌）
(46, 11, 10, '三星 Galaxy S24 Ultra 12+256 钛灰',      5499.00, 7999.00, 1, 1, 198, 8, '同城',   '微信: huangjie_wx','三星,手机,旗舰',          '三星 Galaxy S24 Ultra 12+256 钛灰 三星 手机 旗舰', 0, NOW() - INTERVAL 14 DAY, NOW()),
(47, 12, 10, 'vivo X100 Pro 16+512 落日橙',             3499.00, 4999.00, 1, 1, 156, 9, '教学楼',   '微信: liuyan_wx',  'vivo,手机,影像',          'vivo X100 Pro 16+512 落日橙 vivo 手机 影像', 0, NOW() - INTERVAL 7 DAY, NOW()),
-- 电子数码 - 电脑（更多型号）
(48, 13, 11, '联想拯救者Y9000P 2024 i9款',              7999.00, 9999.00, 1, 1, 289, 7, '同城',   '微信: wanghai_wx', '联想,游戏本,高性能',      '联想拯救者Y9000P 2024 i9款 联想 游戏本 高性能', 0, NOW() - INTERVAL 18 DAY, NOW()),
(49, 14, 11, '华硕天选5 Pro 锐龙版 16寸',               5999.00, 7499.00, 1, 1, 167, 8, '计算机学院','微信: zhangmei_wx','华硕,游戏本,锐龙',       '华硕天选5 Pro 锐龙版 16寸 华硕 游戏本 锐龙', 0, NOW() - INTERVAL 10 DAY, NOW()),
-- 电子数码 - 耳机音箱（更多选择）
(50, 15, 12, 'Bose QC45 头戴式消噪耳机 黑色',           1299.00, 2299.00, 1, 1, 134, 7, '图书馆',   '微信: liguang_wx', 'Bose,耳机,消噪',         'Bose QC45 头戴式消噪耳机 黑色 Bose 耳机 消噪', 0, NOW() - INTERVAL 22 DAY, NOW()),
(51, 16, 12, '漫步者W820NB 双金标头戴耳机 白色',        299.00,  499.00,  1, 1, 89,  8, '同城',   '微信: xujia_wx',   '漫步者,耳机,降噪',       '漫步者W820NB 双金标头戴耳机 白色 漫步者 耳机 降噪', 0, NOW() - INTERVAL 5 DAY, NOW()),
-- 电子数码 - 智能穿戴
(52, 17, 13, '华为 Watch GT4 46mm 棕色皮带',            899.00,  1488.00, 1, 1, 112, 9, '同城',   '微信: qianlei_wx', '华为,手表,运动',         '华为 Watch GT4 46mm 棕色皮带 华为 手表 运动', 0, NOW() - INTERVAL 12 DAY, NOW()),
-- 电子数码 - 游戏设备
(53, 11, 14, 'Xbox Series X 国行主机 1TB',              2999.00, 3799.00, 1, 1, 178, 8, '同城',   '微信: huangjie_wx','Xbox,游戏机,微软',       'Xbox Series X 国行主机 1TB Xbox 游戏机 微软', 0, NOW() - INTERVAL 25 DAY, NOW()),
(54, 12, 14, 'Steam Deck OLED 512G 掌机',               3299.00, 4099.00, 1, 1, 234, 9, '同城',   '微信: liuyan_wx',  'Steam,掌机,游戏',        'Steam Deck OLED 512G 掌机 Steam 掌机 游戏', 0, NOW() - INTERVAL 8 DAY, NOW()),
-- 书籍教材 - 教材（更多科目）
(55, 13, 20, 'C语言程序设计 第五版 谭浩强',              25.00,   49.00,   1, 1, 201, 6, '计算机学院','微信: wanghai_wx', '教材,C语言,编程',       'C语言程序设计 第五版 谭浩强 教材 C语言 编程', 0, NOW() - INTERVAL 35 DAY, NOW()),
(56, 14, 20, '数据结构 C语言版 严蔚敏',                  30.00,   45.00,   1, 1, 178, 7, '计算机学院','微信: zhangmei_wx','教材,数据结构,考研',     '数据结构 C语言版 严蔚敏 教材 数据结构 考研', 0, NOW() - INTERVAL 28 DAY, NOW()),
(57, 15, 20, '概率论与数理统计 浙大第五版',              28.00,   42.00,   2, 1, 145, 5, '数学楼',   '微信: liguang_wx', '教材,概率论,数学',       '概率论与数理统计 浙大第五版 教材 概率论 数学', 0, NOW() - INTERVAL 20 DAY, NOW()),
-- 书籍教材 - 课外读物
(58, 16, 22, '三体 全三册 刘慈欣 精装版',                68.00,   128.00,  1, 1, 312, 9, '同城',   '微信: xujia_wx',   '课外,科幻,经典',         '三体 全三册 刘慈欣 精装版 课外 科幻 经典', 0, NOW() - INTERVAL 15 DAY, NOW()),
(59, 17, 22, '深入理解计算机系统 CSAPP 第三版',          89.00,   139.00,  1, 1, 267, 8, '计算机学院','微信: qianlei_wx', '计算机,经典,底层',       '深入理解计算机系统 CSAPP 第三版 计算机 经典 底层', 0, NOW() - INTERVAL 9 DAY, NOW()),
-- 服饰鞋包 - 鞋靴
(60, 11, 30, 'Converse 1970s 黑色高帮 43码',             259.00,  459.00,  1, 1, 145, 8, '同城',   '微信: huangjie_wx','匡威,帆布鞋,经典',       'Converse 1970s 黑色高帮 43码 匡威 帆布鞋 经典', 0, NOW() - INTERVAL 16 DAY, NOW()),
-- 服饰鞋包 - 服装
(61, 12, 31, 'Patagonia 抓绒衣 蓝色 M码',                349.00,  699.00,  1, 1, 98,  8, '同城',   '微信: liuyan_wx',  'Patagonia,抓绒,户外',    'Patagonia 抓绒衣 蓝色 M码 Patagonia 抓绒 户外', 0, NOW() - INTERVAL 11 DAY, NOW()),
(62, 13, 31, 'Levi\'s 501 经典直筒牛仔裤 32码',          199.00,  599.00,  1, 1, 78,  7, '同城',   '微信: wanghai_wx', 'Levi\'s,牛仔裤,经典',    'Levi\'s 501 经典直筒牛仔裤 32码 牛仔裤 经典', 0, NOW() - INTERVAL 8 DAY, NOW()),
-- 服饰鞋包 - 箱包
(63, 14, 32, 'Fjallraven Kanken 经典双肩包 森林绿',      599.00,  899.00,  1, 1, 167, 9, '同城',   '微信: zhangmei_wx','北极狐,背包,经典',       'Fjallraven Kanken 经典双肩包 森林绿 北极狐 背包 经典', 0, NOW() - INTERVAL 6 DAY, NOW()),
-- 生活用品 - 生活用品
(64, 15, 40, '小米空气净化器4 Lite 卧室款',              399.00,  699.00,  1, 1, 123, 9, '同城',   '微信: liguang_wx', '小米,净化器,生活',       '小米空气净化器4 Lite 卧室款 小米 净化器 生活', 0, NOW() - INTERVAL 13 DAY, NOW()),
(65, 16, 40, '得力碎纸机 家用办公4级保密',               199.00,  399.00,  1, 1, 56,  7, '教学楼',   '微信: xujia_wx',   '碎纸机,办公,得力',       '得力碎纸机 家用办公4级保密 碎纸机 办公 得力', 0, NOW() - INTERVAL 4 DAY, NOW()),
-- 生活用品 - 数码配件
(66, 17, 41, '罗技MX Master 3S 无线鼠标 深灰',          499.00,  749.00,  1, 1, 189, 9, '图书馆',   '微信: qianlei_wx', '罗技,鼠标,办公',         '罗技MX Master 3S 无线鼠标 深灰 罗技 鼠标 办公', 0, NOW() - INTERVAL 11 DAY, NOW()),
(67, 18, 41, 'Apple Magic Keyboard 妙控键盘 带触控ID',   999.00,  1499.00, 1, 1, 134, 10,'图书馆',   '微信: hanxue_wx',  '苹果,键盘,妙控',         'Apple Magic Keyboard 妙控键盘 带触控ID 苹果 键盘 妙控', 0, NOW() - INTERVAL 3 DAY, NOW()),
-- 运动健身 - 健身器材
(68, 11, 50, 'Keep 瑜伽球 65cm 抗爆 配打气筒',          49.00,   89.00,   3, 1, 67,  8, '体育馆',   '微信: huangjie_wx','瑜伽球,健身,Keep',       'Keep 瑜伽球 65cm 抗爆 配打气筒 瑜伽球 健身 Keep', 0, NOW() - INTERVAL 7 DAY, NOW()),
-- 运动健身 - 户外运动
(69, 12, 51, '捷安特ATX860 山地自行车 27.5寸',           1299.00, 2198.00, 1, 1, 198, 7, '停车场',   '微信: liuyan_wx',  '自行车,山地,捷安特',     '捷安特ATX860 山地自行车 27.5寸 自行车 山地 捷安特', 0, NOW() - INTERVAL 30 DAY, NOW()),
-- 虚拟物品 - 游戏账号
(70, 13, 60, '王者荣耀 V10 贵族号 100+皮肤',             299.00,  NULL,    1, 1, 456, 10,'线上交易', '微信: wanghai_wx', '王者荣耀,游戏,账号',     '王者荣耀 V10 贵族号 100+皮肤 王者荣耀 游戏 账号', 0, NOW() - INTERVAL 5 DAY, NOW())
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
-- 17. 补充商品图片数据
-- ===================================================================

INSERT INTO `eo_product_image` (
    `id`, `product_id`, `image_url`, `sort_order`, `is_main`, `create_time`, `update_time`
) VALUES
-- 三星 Galaxy S24 Ultra
(48, 46, 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- vivo X100 Pro
(49, 47, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 联想拯救者
(50, 48, 'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(51, 48, 'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 华硕天选
(52, 49, 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Bose QC45
(53, 50, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 漫步者W820NB
(54, 51, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 华为 Watch GT4
(55, 52, 'https://images.unsplash.com/photo-1546868871-af0de0ae72be?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Xbox Series X
(56, 53, 'https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Steam Deck
(57, 54, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- C语言程序设计
(58, 55, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 数据结构
(59, 56, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 概率论
(60, 57, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 三体
(61, 58, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- CSAPP
(62, 59, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Converse 1970s
(63, 60, 'https://images.unsplash.com/photo-1607522370275-f14206abe5d3?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Patagonia 抓绒
(64, 61, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Levi's 501
(65, 62, 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 北极狐 Kanken
(66, 63, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 小米空气净化器
(67, 64, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 得力碎纸机
(68, 65, 'https://images.unsplash.com/photo-1586953208448-b95a79798f07?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 罗技MX Master 3S
(69, 66, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Apple 妙控键盘
(70, 67, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Keep 瑜伽球
(71, 68, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 捷安特自行车
(72, 69, 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(73, 69, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 王者荣耀账号
(74, 70, 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url` = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main` = new.`is_main`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 18. 补充商品详情数据
-- ===================================================================

INSERT INTO `eo_product_detail` (
    `product_id`, `description`, `create_time`, `update_time`
) VALUES
(46, '三星 Galaxy S24 Ultra 12+256 钛灰 国行在保<br><br>【配置】骁龙8Gen3、12G+256G、S Pen手写笔<br><br>【成色】8成新，屏幕无划痕，钛金属边框轻微磨损<br><br>【特色】2亿像素主摄、Galaxy AI、S Pen<br><br>【配件】原装充电器、S Pen、手机壳', NOW(), NOW()),
(47, 'vivo X100 Pro 16+512 落日橙 蔡司影像<br><br>【配置】天玑9300、16G+512G、1英寸蔡司主摄<br><br>【成色】9成新，几乎全新，贴膜使用<br><br>【配件】原装充电器、数据线、手机壳<br><br>【影像】蔡司APO长焦、T*镀膜', NOW(), NOW()),
(48, '联想拯救者Y9000P 2024 i9款 游戏旗舰<br><br>【配置】i9-14900HX、RTX4060、16G+1T、2.5K 240Hz<br><br>【成色】7成新，键盘有使用痕迹，屏幕无坏点<br><br>【配件】原装充电器、包装盒<br><br>【适合】3A大作、视频剪辑、深度学习', NOW(), NOW()),
(49, '华硕天选5 Pro 锐龙版 16寸 性价比之选<br><br>【配置】R9-7940HX、RTX4070、16G+1T、2.5K 165Hz<br><br>【成色】8成新，外观整洁，散热正常<br><br>【配件】原装充电器<br><br>【适合】游戏、设计、编程', NOW(), NOW()),
(50, 'Bose QC45 头戴式消噪耳机 黑色<br><br>【型号】QuietComfort 45 行货正品<br><br>【成色】7成新，耳罩皮质有轻微磨损，降噪功能正常<br><br>【配件】原装收纳盒、充电线、飞机转接头<br><br>【续航】约24小时，支持快充', NOW(), NOW()),
(51, '漫步者W820NB 双金标头戴耳机 白色 性价比<br><br>【型号】W820NB Hi-Res双金标认证<br><br>【成色】8成新，外观干净，功能正常<br><br>【配件】原装充电线、3.5mm音频线<br><br>【续航】约49小时超长续航', NOW(), NOW()),
(52, '华为 Watch GT4 46mm 棕色皮带 运动健康<br><br>【型号】Watch GT4 46mm 棕色皮表带版<br><br>【成色】9成新，表带轻微使用痕迹，屏幕完好<br><br>【功能】心率、血氧、睡眠监测、100+运动模式<br><br>【续航】约14天超长续航', NOW(), NOW()),
(53, 'Xbox Series X 国行主机 1TB 黑色<br><br>【配置】1TB SSD、4K 120fps、光线追踪<br><br>【成色】8成新，主机运行正常，无故障<br><br>【配件】主机、手柄、HDMI线、电源线<br><br>【备注】国行版本，支持XGPU订阅', NOW(), NOW()),
(54, 'Steam Deck OLED 512G 掌机 便携PC<br><br>【配置】OLED屏幕、512G SSD、AMD APU<br><br>【成色】9成新，屏幕无划痕，摇杆手感正常<br><br>【配件】原装充电器、收纳盒<br><br>【特点】7.4寸OLED屏、SteamOS系统、可装Windows', NOW(), NOW()),
(55, 'C语言程序设计 第五版 谭浩强 清华大学<br><br>【版本】第五版，清华大学出版社<br><br>【成色】6成新，有大量笔记和划线<br><br>【内容】C语言基础、指针、结构体、文件操作<br><br>【适合】编程入门、计算机专业必修', NOW(), NOW()),
(56, '数据结构 C语言版 严蔚敏 经典教材<br><br>【版本】C语言版，清华大学出版社<br><br>【成色】7成新，有少量笔记<br><br>【内容】线性表、树、图、排序、查找<br><br>【适合】计算机专业、考研408', NOW(), NOW()),
(57, '概率论与数理统计 浙大第五版 2本装<br><br>【版本】第五版，高等教育出版社<br><br>【成色】5成新，笔记较多，不影响阅读<br><br>【库存】2本，可单买<br><br>【适合】理工科必修、考研数学', NOW(), NOW()),
(58, '三体 全三册 刘慈欣 精装版 科幻巨著<br><br>【版本】重庆出版社 精装典藏版<br><br>【成色】9成新，几乎全新，无折痕<br><br>【内容】地球往事、黑暗森林、死神永生<br><br>【推荐】雨果奖作品，中国科幻巅峰', NOW(), NOW()),
(59, '深入理解计算机系统 CSAPP 第三版 程序员圣经<br><br>【版本】第三版，机械工业出版社<br><br>【成色】8成新，有少量笔记<br><br>【内容】数据表示、汇编、存储器层次、链接、并发<br><br>【适合】计算机专业进阶、系统编程', NOW(), NOW()),
(60, 'Converse 1970s 黑色高帮 43码 经典百搭<br><br>【型号】Chuck 70 Hi 黑色<br><br>【尺码】43码（US 9.5）<br><br>【成色】8成新，鞋底磨损正常，鞋面干净<br><br>【特点】复古鞋型、加厚鞋垫、经典百搭', NOW(), NOW()),
(61, 'Patagonia 抓绒衣 蓝色 M码 户外保暖<br><br>【型号】Better Sweater 蓝色<br><br>【尺码】M码，适合身高170-175cm<br><br>【成色】8成新，无起球，保暖性良好<br><br>【特点】再生聚酯纤维、全拉链设计', NOW(), NOW()),
(62, 'Levi\'s 501 经典直筒牛仔裤 32码 原色<br><br>【型号】501 Original Fit 原色牛<br><br>【尺码】32码（W32 L32）<br><br>【成色】7成新，有自然落色效果<br><br>【特点】经典直筒、铜扣设计、原色丹宁', NOW(), NOW()),
(63, 'Fjallraven Kanken 经典双肩包 森林绿 瑞典<br><br>【型号】Kanken Classic 16L 森林绿<br><br>【成色】9成新，使用不到3个月<br><br>【材质】Vinylon F防水面料<br><br>【特点】轻量、防水、人体工学背负', NOW(), NOW()),
(64, '小米空气净化器4 Lite 卧室款 静音<br><br>【型号】米家空气净化器4 Lite<br><br>【适用面积】20-40㎡ 生活/卧室<br><br>【成色】9成新，滤芯使用约2个月<br><br>【功能】HEPA滤芯、PM2.5实时显示、米家APP控制', NOW(), NOW()),
(65, '得力碎纸机 家用办公4级保密 白色<br><br>【型号】得力9922 4级保密碎纸机<br><br>【成色】7成新，运行正常，刀片锋利<br><br>【功能】4级保密、可碎信用卡、连续碎纸5分钟<br><br>【适合】生活办公、隐私文件销毁', NOW(), NOW()),
(66, '罗技MX Master 3S 无线鼠标 深灰 办公神器<br><br>【型号】MX Master 3S 深灰色<br><br>【成色】9成新，手感极佳，滚轮丝滑<br><br>【连接】蓝牙+2.4G双模、支持3设备切换<br><br>【续航】约70天，Type-C快充', NOW(), NOW()),
(67, 'Apple Magic Keyboard 妙控键盘 带触控ID<br><br>【型号】带触控ID和数字小键盘版<br><br>【成色】10成新，全新未使用<br><br>【连接】蓝牙无线、USB-C充电<br><br>【兼容】Mac/iPad 通用', NOW(), NOW()),
(68, 'Keep 瑜伽球 65cm 抗爆 配打气筒 紫色<br><br>【尺寸】65cm，适合身高160-175cm<br><br>【材质】PVC抗爆材质，承重300kg<br><br>【成色】8成新，无漏气<br><br>【配件】打气筒、气塞、使用说明', NOW(), NOW()),
(69, '捷安特ATX860 山地自行车 27.5寸 蓝色<br><br>【型号】ATX860 铝合金车架 蓝白配色<br><br>【变速】Shimano 3x8速 24速变速<br><br>【成色】7成新，轮胎磨损正常<br><br>【配置】液压碟刹、避震前叉、铝合金轮组', NOW(), NOW()),
(70, '王者荣耀 V10 贵族号 100+皮肤 全英雄<br><br>【等级】V10贵族、全英雄解锁<br><br>【皮肤】100+皮肤含限定、传说、史诗<br><br>【段位】历史最强王者<br><br>【备注】可改绑手机号，安全交易', NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `description` = new.`description`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 19. 补充收藏数据
-- ===================================================================

INSERT INTO `eo_favorite` (
    `id`, `user_id`, `product_id`, `create_time`, `update_time`
) VALUES
(21, 11, 5,  NOW() - INTERVAL 12 DAY, NOW()),
(22, 11, 14, NOW() - INTERVAL 8 DAY, NOW()),
(23, 12, 48, NOW() - INTERVAL 5 DAY, NOW()),
(24, 12, 8,  NOW() - INTERVAL 3 DAY, NOW()),
(25, 13, 46, NOW() - INTERVAL 10 DAY, NOW()),
(26, 13, 58, NOW() - INTERVAL 7 DAY, NOW()),
(27, 14, 54, NOW() - INTERVAL 4 DAY, NOW()),
(28, 14, 66, NOW() - INTERVAL 2 DAY, NOW()),
(29, 15, 50, NOW() - INTERVAL 6 DAY, NOW()),
(30, 15, 37, NOW() - INTERVAL 3 DAY, NOW()),
(31, 16, 59, NOW() - INTERVAL 8 DAY, NOW()),
(32, 16, 67, NOW() - INTERVAL 1 DAY, NOW()),
(33, 17, 53, NOW() - INTERVAL 5 DAY, NOW()),
(34, 17, 69, NOW() - INTERVAL 2 DAY, NOW()),
(35, 18, 47, NOW() - INTERVAL 3 DAY, NOW()),
(36, 18, 63, NOW() - INTERVAL 1 DAY, NOW()),
(37, 1,  48, NOW() - INTERVAL 4 DAY, NOW()),
(38, 3,  54, NOW() - INTERVAL 2 DAY, NOW()),
(39, 5,  66, NOW() - INTERVAL 1 DAY, NOW()),
(40, 7,  58, NOW() - INTERVAL 3 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `update_time` = new.`update_time`;

-- ===================================================================
-- 20. 补充订单数据（更多场景覆盖）
-- ===================================================================

INSERT INTO `eo_order` (
    `id`, `order_no`, `buyer_id`, `seller_id`, `total_amount`,
    `status`, `payment_status`, `address`, `phone`, `remark`,
    `create_time`, `update_time`
) VALUES
(13, 'ORD20260206001', 14, 11, 5499.00, 3, 1, '西校区5号楼201室', '13800138014', '请周末面交',   NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 10 DAY),
(14, 'ORD20260207001', 15, 12, 3499.00, 3, 1, '东校区2号楼415室', '13800138015', '',              NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 7 DAY),
(15, 'ORD20260208001', 16, 13, 7999.00, 1, 1, '北校区8号楼303室', '13800138016', '尽快发货',     NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),
(16, 'ORD20260209001', 17, 14, 5999.00, 2, 1, '南校区6号楼507室', '13800138017', '',              NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
(17, 'ORD20260210001', 18, 11, 2999.00, 0, 0, '东校区1号楼102室', '13800138018', '可以面交吗',   NOW() - INTERVAL 1 DAY, NOW()),
(18, 'ORD20260211001', 11, 15, 1299.00, 3, 1, '西校区5号楼201室', '13800138011', '',              NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 18 DAY),
(19, 'ORD20260212001', 12, 16, 299.00,  3, 1, '南校区3号楼608室', '13800138012', '',              NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 13 DAY),
(20, 'ORD20260213001', 13, 17, 499.00,  4, 0, '东校区4号楼210室', '13800138013', '',              NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 7 DAY),
(21, 'ORD20260214001', 14, 12, 3299.00, 5, 2, '西校区5号楼201室', '13800138014', '掌机屏幕有亮点', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 4 DAY),
(22, 'ORD20260215001', 15, 11, 259.00,  3, 1, '北校区2号楼410室', '13800138015', '',              NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `status` = new.`status`,
    `payment_status` = new.`payment_status`,
    `update_time` = new.`update_time`;
INSERT INTO `eo_order_item` (
    `id`, `order_id`, `product_id`, `product_snapshot`, `unit_price`, `quantity`, `subtotal`,
    `create_time`, `update_time`, `del_flag`, `version`
) VALUES
(1300,13, 46, '{}', 5499.00, 1, 5499.00, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 10 DAY, 0, 1),
(1400,14, 47, '{}', 3499.00, 1, 3499.00, NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 7 DAY,  0, 1),
(1500,15, 48, '{}', 7999.00, 1, 7999.00, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 2 DAY,  0, 1),
(1600,16, 49, '{}', 5999.00, 1, 5999.00, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 3 DAY,  0, 1),
(1700,17, 53, '{}', 2999.00, 1, 2999.00, NOW() - INTERVAL 1 DAY,  NOW(),                    0, 1),
(1800,18, 50, '{}', 1299.00, 1, 1299.00, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 18 DAY,  0, 1),
(1900,19, 51, '{}', 299.00,  1, 299.00,  NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 13 DAY,  0, 1),
(2000,20, 66, '{}', 499.00,  1, 499.00,  NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 7 DAY,   0, 1),
(2100,21, 54, '{}', 3299.00, 1, 3299.00, NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 4 DAY,   0, 1),
(2200,22, 60, '{}', 259.00,  1, 259.00,  NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 2 DAY,   0, 1)
AS new
ON DUPLICATE KEY UPDATE
    `product_snapshot` = new.`product_snapshot`,
    `unit_price` = new.`unit_price`,
    `subtotal` = new.`subtotal`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 21. 补充支付记录数据
-- ===================================================================

INSERT INTO `eo_payment` (
    `id`, `payment_no`, `order_id`, `user_id`, `amount`, `refunded_amount`,
    `payment_method`, `status`, `create_time`, `update_time`
) VALUES
(11, 'PAY20260206001', 13, 14, 5499.00, 0,     2, 1, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 10 DAY),
(12, 'PAY20260207001', 14, 15, 3499.00, 0,     1, 1, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 7 DAY),
(13, 'PAY20260208001', 15, 16, 7999.00, 0,     1, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),
(14, 'PAY20260209001', 16, 17, 5999.00, 0,     2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
(15, 'PAY20260211001', 18, 11, 1299.00, 0,     1, 1, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 18 DAY),
(16, 'PAY20260212001', 19, 12, 299.00,  0,     2, 1, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 13 DAY),
(17, 'PAY20260214001', 21, 14, 3299.00, 3299.00, 1, 2, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 4 DAY),
(18, 'PAY20260215001', 22, 15, 259.00,  0,     1, 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `status` = new.`status`,
    `refunded_amount` = new.`refunded_amount`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 22. 补充消息数据（更多私聊对话和系统消息）
-- ===================================================================

INSERT INTO `eo_message` (
    `id`, `sender_id`, `receiver_id`, `type`, `title`, `content`,
    `is_read`, `read_time`, `business_id`, `conversation_id`,
    `create_time`, `update_time`
) VALUES
-- 系统消息（新用户注册）
(26, NULL, 11, 0, '欢迎加入EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布资产，AI 工程化能力帮你估值、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 179 DAY, NULL, NULL, NOW() - INTERVAL 180 DAY, NOW()),
(27, NULL, 12, 0, '欢迎加入EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布资产，AI 工程化能力帮你估值、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 119 DAY, NULL, NULL, NOW() - INTERVAL 120 DAY, NOW()),
(28, NULL, 17, 0, '欢迎加入EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布资产，AI 工程化能力帮你估值、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 13 DAY, NULL, NULL, NOW() - INTERVAL 14 DAY, NOW()),
-- 订单消息（新订单）
(29, NULL, 14, 2, '订单创建成功', '你已成功下单「三星 Galaxy S24 Ultra」，订单号：ORD20260206001，请尽快完成支付。', 1, NOW() - INTERVAL 12 DAY, 13, NULL, NOW() - INTERVAL 12 DAY, NOW()),
(30, NULL, 11, 2, '收到新订单', '你的商品「三星 Galaxy S24 Ultra」有新订单，请尽快处理。订单号：ORD20260206001', 1, NOW() - INTERVAL 12 DAY, 13, NULL, NOW() - INTERVAL 12 DAY, NOW()),
(31, NULL, 16, 2, '订单创建成功', '你已成功下单「联想拯救者Y9000P」，订单号：ORD20260208001，请尽快完成支付。', 1, NOW() - INTERVAL 3 DAY, 15, NULL, NOW() - INTERVAL 3 DAY, NOW()),
(32, NULL, 13, 2, '收到新订单', '你的商品「联想拯救者Y9000P」有新订单，请尽快处理。', 0, NULL, 15, NULL, NOW() - INTERVAL 3 DAY, NOW()),
-- 私聊对话 - 黄杰和刘燕聊三星手机
(33, 12, 11, 1, NULL, '三星S24 Ultra还在吗？可以便宜点吗？', 1, NOW() - INTERVAL 15 DAY, NULL, 107, NOW() - INTERVAL 15 DAY, NOW()),
(34, 11, 12, 1, NULL, '在的，5299最低了', 1, NOW() - INTERVAL 15 DAY, NULL, 107, NOW() - INTERVAL 15 DAY, NOW()),
(35, 12, 11, 1, NULL, '5499包邮可以吗？', 1, NOW() - INTERVAL 14 DAY, NULL, 107, NOW() - INTERVAL 14 DAY, NOW()),
(36, 11, 12, 1, NULL, '可以，你下单吧', 1, NOW() - INTERVAL 14 DAY, NULL, 107, NOW() - INTERVAL 14 DAY, NOW()),
-- 私聊对话 - 王海和刘燕聊vivo
(37, 13, 12, 1, NULL, 'vivo X100 Pro拍照效果怎么样？', 1, NOW() - INTERVAL 8 DAY, NULL, 108, NOW() - INTERVAL 8 DAY, NOW()),
(38, 12, 13, 1, NULL, '蔡司镜头很棒，夜景尤其好', 1, NOW() - INTERVAL 8 DAY, NULL, 108, NOW() - INTERVAL 8 DAY, NOW()),
-- 私聊对话 - 张梅问华硕电脑
(39, 14, 13, 1, NULL, '华硕天选散热怎么样？打游戏会降频吗？', 1, NOW() - INTERVAL 11 DAY, NULL, 109, NOW() - INTERVAL 11 DAY, NOW()),
(40, 13, 14, 1, NULL, '不会降频，双风扇散热很给力，我打黑神话都没问题', 1, NOW() - INTERVAL 11 DAY, NULL, 109, NOW() - INTERVAL 11 DAY, NOW()),
-- 私聊对话 - 钱磊问罗技鼠标
(41, 17, 15, 1, NULL, '罗技MX Master 3S手感怎么样？办公用', 0, NULL, NULL, 110, NOW() - INTERVAL 2 DAY, NOW()),
(42, 15, 17, 1, NULL, '手感非常好，滚轮丝滑，推荐入手', 0, NULL, NULL, 110, NOW() - INTERVAL 2 DAY, NOW()),
-- 私聊对话 - 韩雪问妙控键盘
(43, 18, 16, 1, NULL, '妙控键盘是全新的吗？为什么出？', 0, NULL, NULL, 111, NOW() - INTERVAL 1 DAY, NOW()),
(44, 16, 18, 1, NULL, '买来没拆封，买错型号了', 0, NULL, NULL, 111, NOW() - INTERVAL 1 DAY, NOW()),
-- 私聊对话 - 李光问Bose耳机
(45, 15, 11, 1, NULL, 'Bose QC45降噪效果和Sony XM5比怎么样？', 1, NOW() - INTERVAL 23 DAY, NULL, 112, NOW() - INTERVAL 23 DAY, NOW()),
(46, 11, 15, 1, NULL, '降噪略逊XM5，但佩戴更舒适，长时间戴不夹头', 1, NOW() - INTERVAL 23 DAY, NULL, 112, NOW() - INTERVAL 23 DAY, NOW()),
-- 私聊对话 - 许佳和钱磊聊三体
(47, 17, 16, 1, NULL, '三体精装版有书盒吗？', 1, NOW() - INTERVAL 16 DAY, NULL, 113, NOW() - INTERVAL 16 DAY, NOW()),
(48, 16, 17, 1, NULL, '有的，精装礼盒装，很漂亮', 1, NOW() - INTERVAL 16 DAY, NULL, 113, NOW() - INTERVAL 16 DAY, NOW()),
-- 私聊对话 - 黄杰问自行车
(49, 11, 12, 1, NULL, '捷安特自行车可以试骑吗？', 0, NULL, NULL, 114, NOW(), NOW()),
-- 私聊对话 - 王海聊王者荣耀
(50, 13, 11, 1, NULL, '王者荣耀账号可以走平台担保吗？', 0, NULL, NULL, 115, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `is_read` = new.`is_read`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 23. 补充搜索历史数据
-- ===================================================================

INSERT INTO `eo_search_history` (
    `id`, `user_id`, `keyword`, `search_time`, `create_time`, `update_time`
) VALUES
(21, 11, '游戏本',       NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY, NOW()),
(22, 11, 'Xbox',         NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY, NOW()),
(23, 12, 'vivo手机',     NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 7 DAY, NOW()),
(24, 12, 'Steam Deck',   NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 8 DAY, NOW()),
(25, 13, '数据结构',     NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 28 DAY, NOW()),
(26, 13, '游戏账号',     NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY, NOW()),
(27, 14, '华硕电脑',     NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY, NOW()),
(28, 14, '北极狐背包',   NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY, NOW()),
(29, 15, 'Bose耳机',     NOW() - INTERVAL 22 DAY, NOW() - INTERVAL 22 DAY, NOW()),
(30, 15, '空气净化器',   NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY, NOW()),
(31, 16, '三体',         NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, NOW()),
(32, 16, '妙控键盘',     NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY, NOW()),
(33, 17, '罗技鼠标',     NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY, NOW()),
(34, 17, '自行车',       NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY, NOW()),
(35, 18, 'vivo',         NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `search_time` = new.`search_time`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 24. 补充热门关键词数据
-- ===================================================================

INSERT INTO `eo_hot_keyword` (
    `id`, `keyword`, `search_count`, `last_search_time`, `create_time`, `update_time`
) VALUES
(16, '游戏本',     198, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 60 DAY, NOW()),
(17, 'Steam Deck', 89,  NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 30 DAY, NOW()),
(18, '罗技鼠标',   67,  NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 25 DAY, NOW()),
(19, '三体',       56,  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 20 DAY, NOW()),
(20, '妙控键盘',   45,  NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 15 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `search_count` = new.`search_count`,
    `last_search_time` = new.`last_search_time`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 25. 商品举报数据（新数据类型）
-- ===================================================================

INSERT INTO `eo_product_report` (
    `id`, `product_id`, `reporter_id`, `reason`, `status`, `handle_result`,
    `create_time`, `update_time`
) VALUES
(1, 39, 4,  '虚拟物品交易风险，建议平台审核',   1, '已核实，商品信息真实，暂不处理', NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY),
(2, 70, 14, '游戏账号交易存在安全隐患',         1, '已提醒资产方完善交易保障说明',     NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
(3, 3,  17, '商品描述与实际不符，成色虚标',     0, NULL,                             NOW() - INTERVAL 1 DAY, NOW()),
(4, 25, 12, '价格明显高于市场价，疑似哄抬价格', 2, '经核实价格合理，已忽略',         NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 13 DAY),
(5, 42, 8,  '已下架商品仍在搜索结果中显示',     1, '已优化搜索索引，下架商品不再展示', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 18 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `status` = new.`status`,
    `handle_result` = new.`handle_result`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 26. 操作日志数据（新数据类型）
-- ===================================================================

INSERT INTO `eo_oper_log` (
    `oper_id`, `title`, `business_type`, `method`, `request_method`, `operator_type`,
    `oper_name`, `oper_url`, `oper_ip`, `oper_location`,
    `status`, `cost_time`, `oper_time`
) VALUES
(1, '用户管理', 1, 'UserController.register()',  'POST', 1, 'testuser',  '/api/user/register', '192.168.1.100', '宽带', 0, 156, NOW() - INTERVAL 90 DAY),
(2, '商品管理', 2, 'ProductController.create()',  'POST', 1, 'testuser',  '/api/product/create', '192.168.1.100', '宽带', 0, 89,  NOW() - INTERVAL 30 DAY),
(3, '商品管理', 2, 'ProductController.create()',  'POST', 1, 'liming',    '/api/product/create', '10.0.0.55',    '图书馆', 0, 112, NOW() - INTERVAL 25 DAY),
(4, '订单管理', 2, 'OrderController.create()',    'POST', 1, 'liming',    '/api/order/create',   '10.0.0.55',    '图书馆', 0, 234, NOW() - INTERVAL 60 DAY),
(5, '支付管理', 2, 'PaymentController.pay()',     'POST', 1, 'liming',    '/api/payment/pay',    '10.0.0.55',    '图书馆', 0, 567, NOW() - INTERVAL 60 DAY),
(6, '商品管理', 3, 'ProductController.update()',  'PUT',  1, 'testuser',  '/api/product/1',      '192.168.1.100', '宽带', 0, 67,  NOW() - INTERVAL 20 DAY),
(7, '用户管理', 2, 'UserController.login()',      'POST', 1, 'wangfang',  '/api/user/login',     '172.16.0.23',  '同城', 0, 45,  NOW() - INTERVAL 45 DAY),
(8, '订单管理', 2, 'OrderController.create()',    'POST', 1, 'wangfang',  '/api/order/create',   '172.16.0.23',  '同城', 0, 189, NOW() - INTERVAL 55 DAY),
(9, '收藏管理', 2, 'FavoriteController.add()',    'POST', 1, 'zhaowei',   '/api/favorite/add',   '192.168.2.34', '教学楼', 0, 34,  NOW() - INTERVAL 8 DAY),
(10, '商品管理', 1, 'ProductController.search()',  'GET',  1, 'sunli',     '/api/product/search', '192.168.2.34', '教学楼', 0, 78,  NOW() - INTERVAL 15 DAY),
(11, '用户管理', 2, 'UserController.login()',      'POST', 1, 'huangjie',  '/api/user/login',     '10.0.1.100',   '同城', 0, 38,  NOW() - INTERVAL 180 DAY),
(12, '商品管理', 2, 'ProductController.create()',  'POST', 1, 'huangjie',  '/api/product/create', '10.0.1.100',   '同城', 0, 95,  NOW() - INTERVAL 14 DAY),
(13, '订单管理', 2, 'OrderController.cancel()',    'PUT',  1, 'wanghai',   '/api/order/20/cancel','10.0.0.88',    '图书馆', 0, 123, NOW() - INTERVAL 7 DAY),
(14, '支付管理', 2, 'PaymentController.refund()',  'POST', 1, 'zhangmei',  '/api/payment/refund', '172.16.0.45',  '同城', 0, 456, NOW() - INTERVAL 6 DAY),
(15, '系统管理', 4, 'AdminController.exportLog()',  'GET',  2, 'admin',     '/api/admin/log/export','10.0.0.1',    '服务器', 0, 2345, NOW() - INTERVAL 3 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `title` = new.`title`,
    `business_type` = new.`business_type`,
    `method` = new.`method`,
    `oper_time` = new.`oper_time`;

-- ===================================================================
-- 27. 补充消息订阅数据（新用户）
-- ===================================================================

INSERT INTO `eo_message_subscription` (
    `id`, `user_id`, `message_type`, `push_channel`, `enabled`, `create_time`, `update_time`
) VALUES
(21, 11, 'order',   'websocket', 1, NOW(), NOW()),
(22, 11, 'system',  'websocket', 1, NOW(), NOW()),
(23, 11, 'chat',    'websocket', 1, NOW(), NOW()),
(24, 12, 'order',   'websocket', 1, NOW(), NOW()),
(25, 12, 'system',  'websocket', 1, NOW(), NOW()),
(26, 12, 'chat',    'websocket', 1, NOW(), NOW()),
(27, 13, 'order',   'websocket', 1, NOW(), NOW()),
(28, 13, 'system',  'websocket', 1, NOW(), NOW()),
(29, 13, 'chat',    'websocket', 0, NOW(), NOW()),
(30, 14, 'order',   'websocket', 1, NOW(), NOW()),
(31, 14, 'system',  'websocket', 1, NOW(), NOW()),
(32, 14, 'chat',    'websocket', 1, NOW(), NOW()),
(33, 15, 'order',   'websocket', 1, NOW(), NOW()),
(34, 15, 'system',  'websocket', 1, NOW(), NOW()),
(35, 15, 'chat',    'websocket', 1, NOW(), NOW()),
(36, 16, 'order',   'websocket', 1, NOW(), NOW()),
(37, 16, 'system',  'websocket', 1, NOW(), NOW()),
(38, 16, 'chat',    'websocket', 1, NOW(), NOW()),
(39, 17, 'order',   'websocket', 1, NOW(), NOW()),
(40, 17, 'system',  'websocket', 1, NOW(), NOW()),
(41, 17, 'chat',    'websocket', 1, NOW(), NOW()),
(42, 18, 'order',   'websocket', 1, NOW(), NOW()),
(43, 18, 'system',  'websocket', 1, NOW(), NOW()),
(44, 18, 'chat',    'websocket', 1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `enabled` = new.`enabled`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 28. 补充商品数据（ID 71-100，来自原 test-data.sql）
-- 注意：分类ID已修正为现有分类
--   - 教材类(101) → 20(教材)
--   - 考研类(102) → 21(考研资料)
--   - 课外读物(103,104) → 22(课外读物)
--   - 手机类(201) → 10(手机)
--   - 电脑类(202) → 11(电脑)
--   - 平板类(203) → 13(智能穿戴)
--   - 耳机音箱(204) → 12(耳机音箱)
--   - 相机无人机(205) → 14(游戏设备)
--   - 游戏机(206) → 14(游戏设备)
--   - 女装(301) → 31(服装)
--   - 男装(302) → 31(服装)
--   - 鞋靴(303) → 30(鞋靴)
--   - 护肤(601) → 40(生活用品)
--   - 彩妆(602) → 40(生活用品)
--   - 香水(603) → 40(生活用品)
--   - 美发(604) → 40(生活用品)
-- ===================================================================

INSERT INTO `eo_product` (
    `id`, `user_id`, `category_id`, `name`, `search_text`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `tags`, `contact_method`, `del_flag`, `create_time`, `update_time`, `version`
) VALUES
-- 更多图书教材类商品
(71, 2, 20, '线性代数 第五版', '线性代数 第五版 数学教材', 12.00, 32.00, 1, 1, 89, 2, '图书馆', '教材,数学,线性代数', '微信', 0, NOW(), NOW(), 0),
(72, 2, 20, '概率论与数理统计', '概率论与数理统计 数学教材', 15.00, 38.00, 1, 1, 78, 2, '图书馆', '教材,数学,概率论', '微信', 0, NOW(), NOW(), 0),
(73, 2, 20, '大学物理实验教程', '大学物理实验教程 物理教材', 18.00, 42.00, 1, 1, 56, 2, '教学楼', '教材,物理,实验', 'QQ', 0, NOW(), NOW(), 0),
(74, 5, 21, '考研数学真题大全解', '考研数学真题大全解 考研资料', 32.00, 78.00, 1, 1, 167, 2, '图书馆', '考研,数学,真题', '微信', 0, NOW(), NOW(), 0),
(75, 5, 21, '考研英语词汇红宝书', '考研英语词汇红宝书 考研资料', 25.00, 58.00, 1, 1, 145, 2, '图书馆', '考研,英语,词汇', '微信', 0, NOW(), NOW(), 0),
(76, 5, 21, '考研政治核心考点', '考研政治核心考点 考研资料', 22.00, 48.00, 1, 1, 134, 2, '图书馆', '考研,政治,考点', '微信', 0, NOW(), NOW(), 0),
(77, 11, 22, '围城 钱钟书', '围城 钱钟书 文学名著', 18.00, 39.00, 1, 1, 89, 2, '同城', '小说,文学,名著', '微信', 0, NOW(), NOW(), 0),
(78, 11, 22, '活着 余华', '活着 余华 文学名著', 15.00, 35.00, 1, 1, 112, 2, '同城', '小说,文学,名著', '微信', 0, NOW(), NOW(), 0),
(79, 11, 22, 'Python编程从入门到实践', 'Python编程从入门到实践 编程书籍', 45.00, 89.00, 1, 1, 234, 2, '教学楼', '编程,Python,入门', 'QQ', 0, NOW(), NOW(), 0),
(80, 11, 22, '算法导论 第三版', '算法导论 第三版 计算机经典', 78.00, 128.00, 1, 1, 189, 2, '教学楼', '计算机,算法,经典', 'QQ', 0, NOW(), NOW(), 0),
-- 更多电子产品类商品
(81, 3, 10, 'OPPO Find X3 Pro', 'OPPO Find X3 Pro 安卓手机', 2500.00, 4999.00, 1, 1, 189, 2, '南门', '手机,OPPO,安卓', '微信', 0, NOW(), NOW(), 0),
(82, 3, 10, 'vivo X70 Pro+', 'vivo X70 Pro+ 安卓手机', 2800.00, 5499.00, 1, 1, 156, 2, '南门', '手机,vivo,安卓', '微信', 0, NOW(), NOW(), 0),
(83, 3, 11, '华硕ROG幻14 游戏本', '华硕ROG幻14 游戏本 电竞笔记本', 7500.00, 10999.00, 1, 1, 234, 2, '同城', '电脑,华硕,游戏', 'QQ', 0, NOW(), NOW(), 0),
(84, 3, 11, '机械革命Code01 程序员本', '机械革命Code01 程序员笔记本', 4500.00, 6999.00, 1, 1, 145, 2, '图书馆', '电脑,机械革命,程序员', '微信', 0, NOW(), NOW(), 0),
(85, 10, 13, '华为MatePad Pro 11', '华为MatePad Pro 11 安卓平板', 2200.00, 4199.00, 1, 1, 189, 2, '食堂', '平板,华为,MatePad', '微信', 0, NOW(), NOW(), 0),
(86, 10, 12, 'Bose QC45 降噪耳机', 'Bose QC45 降噪耳机 头戴式', 1300.00, 2499.00, 1, 1, 178, 2, '教学楼', '耳机,Bose,降噪', 'QQ', 0, NOW(), NOW(), 0),
(87, 10, 12, 'JBL蓝牙音箱', 'JBL蓝牙音箱 便携音箱', 280.00, 599.00, 1, 1, 134, 2, '同城', '音箱,JBL,蓝牙', '微信', 0, NOW(), NOW(), 0),
(88, 12, 14, '大疆DJI Mini 2 无人机', '大疆DJI Mini 2 无人机 航拍', 2800.00, 4199.00, 1, 1, 167, 2, '北门', '相机,大疆,无人机', '微信', 0, NOW(), NOW(), 0),
(89, 12, 14, 'GoPro Hero10 运动相机', 'GoPro Hero10 运动相机', 1800.00, 3398.00, 1, 1, 123, 2, '北门', '相机,GoPro,运动', '微信', 0, NOW(), NOW(), 0),
(90, 10, 14, 'Xbox Series X 国行', 'Xbox Series X 国行 游戏机', 3500.00, 3899.00, 1, 1, 189, 1, '同城', '游戏机,Xbox,微软', 'QQ', 0, NOW(), NOW(), 0),
-- 更多服装鞋包类商品
(91, 6, 31, 'H&M针织开衫 M码', 'H&M针织开衫 M码 女装', 59.00, 149.00, 1, 1, 89, 2, '同城', '女装,开衫,H&M', '微信', 0, NOW(), NOW(), 0),
(92, 6, 31, 'ONLY牛仔裤 26码', 'ONLY牛仔裤 26码 女装', 89.00, 299.00, 1, 1, 78, 2, '同城', '女装,牛仔裤,ONLY', '微信', 0, NOW(), NOW(), 0),
(93, 6, 31, 'Levi\'s牛仔裤 30码', 'Levi\'s牛仔裤 30码 男装', 150.00, 599.00, 1, 1, 112, 2, '同城', '男装,牛仔裤,Levi\'s', 'QQ', 0, NOW(), NOW(), 0),
(94, 6, 31, 'Gap卫衣 L码', 'Gap卫衣 L码 男装', 79.00, 299.00, 1, 1, 98, 2, '同城', '男装,卫衣,Gap', 'QQ', 0, NOW(), NOW(), 0),
(95, 4, 30, 'Converse帆布鞋 41码', 'Converse帆布鞋 41码 休闲鞋', 180.00, 499.00, 1, 1, 145, 2, '体育馆', '鞋子,帆布鞋,Converse', '微信', 0, NOW(), NOW(), 0),
-- 美妆护肤类商品（归类到生活用品-生活用品）
(96, 9, 40, '兰蔻小黑瓶精华 50ml', '兰蔻小黑瓶精华 50ml 护肤', 380.00, 1080.00, 1, 1, 234, 1, '同城', '护肤,精华,兰蔻', '微信', 0, NOW(), NOW(), 0),
(97, 9, 40, 'SK-II神仙水 230ml', 'SK-II神仙水 230ml 护肤', 650.00, 1540.00, 1, 1, 189, 2, '同城', '护肤,神仙水,SK-II', '微信', 0, NOW(), NOW(), 0),
(98, 9, 40, 'MAC口红 Chili小辣椒', 'MAC口红 Chili小辣椒 彩妆', 89.00, 180.00, 1, 1, 267, 1, '同城', '彩妆,口红,MAC', '微信', 0, NOW(), NOW(), 0),
(99, 9, 40, '祖玛珑香水 蓝风铃30ml', '祖玛珑香水 蓝风铃30ml 香水', 280.00, 600.00, 1, 1, 145, 1, '同城', '香水,祖玛珑,蓝风铃', '微信', 0, NOW(), NOW(), 0),
(100, 9, 40, '戴森吹风机 HD08', '戴森吹风机 HD08 美发', 2200.00, 3290.00, 1, 1, 123, 2, '同城', '美发,吹风机,戴森', '微信', 0, NOW(), NOW(), 0)
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
    `tags` = new.`tags`,
    `search_text` = new.`search_text`,
    `del_flag` = new.`del_flag`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 29. 补充商品数据（ID 1001-1012，来自原 test_data.sql）
-- ===================================================================

INSERT INTO `eo_product` (
    `id`, `user_id`, `category_id`, `name`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `contact_method`, `tags`, `search_text`, `del_flag`, `create_time`, `update_time`
) VALUES
(1001, 1, 11, 'MacBook Pro 14寸 M3芯片 深空灰', 11999.00, 14999.00, 1, 1, 256, 2, '图书馆', '微信联系', '苹果,笔记本,高性能', 'MacBook Pro 14寸 M3芯片 苹果 笔记本 高性能', 0, NOW(), NOW()),
(1002, 1, 13, 'iPad Air 5 256G WiFi 蓝色', 3299.00, 4799.00, 1, 1, 189, 2, '同城', '微信联系', '苹果,平板,学习', 'iPad Air 5 256G 苹果 平板 学习', 0, NOW(), NOW()),
(1003, 1, 12, 'AirPods Pro 2 全新未拆封', 1299.00, 1899.00, 1, 1, 312, 1, '同城', '微信联系', '苹果,耳机,降噪', 'AirPods Pro 2 苹果 耳机 降噪', 0, NOW(), NOW()),
(1004, 1, 10, '小米13 Pro 256G 黑色', 2999.00, 4999.00, 1, 1, 145, 3, '教学楼', '微信联系', '小米,手机,拍照', '小米13 Pro 手机 拍照', 0, NOW(), NOW()),
(1005, 1, 14, 'Switch OLED 游戏机 白色', 1599.00, 2599.00, 1, 1, 198, 2, '体育馆', '微信联系', '游戏机,任天堂,娱乐', 'Switch OLED 游戏机 任天堂 娱乐', 0, NOW(), NOW()),
(1006, 1, 21, '考研英语词汇红宝书 2024版', 35.00, 68.00, 1, 1, 88, 3, '线下交易点', '微信联系', '考研,英语,词汇', '考研英语词汇 红宝书 考研 英语', 0, NOW(), NOW()),
(1007, 1, 20, '高等数学同济第七版 上下册', 45.00, 89.00, 1, 1, 67, 3, '数学楼', '微信联系', '教材,数学,高数', '高等数学 同济第七版 教材 数学', 0, NOW(), NOW()),
(1008, 1, 22, '数据结构与算法 Python版', 55.00, 99.00, 1, 1, 76, 2, '计算机学院', '微信联系', '计算机,算法,编程', '数据结构 算法 Python 计算机', 0, NOW(), NOW()),
(1009, 1, 30, 'Nike Air Jordan 1 经典黑白 42码', 699.00, 1299.00, 1, 1, 156, 2, '操场', '微信联系', 'Nike,球鞋,经典', 'Nike Air Jordan 1 球鞋 经典', 0, NOW(), NOW()),
(1010, 1, 31, '北面冲锋衣 黑色 M码 防水', 399.00, 899.00, 1, 1, 123, 2, '同城', '微信联系', '北面,外套,户外', '北面 冲锋衣 外套 户外', 0, NOW(), NOW()),
(1011, 1, 40, '小米台灯Pro 护眼阅读灯', 89.00, 149.00, 1, 1, 95, 2, '图书馆', '微信联系', '小米,台灯,护眼', '小米台灯Pro 护眼 台灯 阅读', 0, NOW(), NOW()),
(1012, 1, 51, '迪卡侬山地自行车 27速', 899.00, 1599.00, 1, 1, 112, 2, '停车场', '微信联系', '自行车,运动,出行', '迪卡侬 山地自行车 运动 出行', 0, NOW(), NOW())
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
-- 30. 补充商品图片数据（ID 71-100）
-- ===================================================================

INSERT INTO `eo_product_image` (
    `id`, `product_id`, `image_url`, `sort_order`, `is_main`, `create_time`, `update_time`
) VALUES
-- 线性代数
(75, 71, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 概率论
(76, 72, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 大学物理实验
(77, 73, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 考研数学真题
(78, 74, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 考研英语红宝书
(79, 75, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 考研政治核心考点
(80, 76, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 围城
(81, 77, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 活着
(82, 78, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Python编程
(83, 79, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 算法导论
(84, 80, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- OPPO Find X3
(85, 81, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- vivo X70
(86, 82, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 华硕ROG幻14
(87, 83, 'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 机械革命
(88, 84, 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 华为MatePad
(89, 85, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Bose QC45
(90, 86, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- JBL音箱
(91, 87, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 大疆无人机
(92, 88, 'https://images.unsplash.com/photo-1473968512647-3e447244af8f?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- GoPro
(93, 89, 'https://images.unsplash.com/photo-1564466809058-bf4114d55352?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Xbox Series X
(94, 90, 'https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- H&M针织开衫
(95, 91, 'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- ONLY牛仔裤
(96, 92, 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Levi's牛仔裤
(97, 93, 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Gap卫衣
(98, 94, 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Converse帆布鞋
(99, 95, 'https://images.unsplash.com/photo-1607522370275-f14206abe5d3?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 兰蔻小黑瓶
(100, 96, 'https://images.unsplash.com/photo-1617897903246-719242758050?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- SK-II神仙水
(101, 97, 'https://images.unsplash.com/photo-1617897903246-719242758050?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- MAC口红
(102, 98, 'https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 祖玛珑香水
(103, 99, 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 戴森吹风机
(104, 100, 'https://images.unsplash.com/photo-1522338140262-f46f5913618a?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url` = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main` = new.`is_main`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 31. 补充商品图片数据（ID 1001-1012）
-- ===================================================================

INSERT INTO `eo_product_image` (
    `id`, `product_id`, `image_url`, `sort_order`, `is_main`, `create_time`, `update_time`
) VALUES
-- MacBook Pro
(2001, 1001, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2002, 1001, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- iPad Air
(2003, 1002, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2004, 1002, 'https://images.unsplash.com/photo-1561154464-82e9b9a6f1c1?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- AirPods Pro
(2005, 1003, 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2006, 1003, 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 小米13 Pro
(2007, 1004, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2008, 1004, 'https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- Switch OLED
(2009, 1005, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2010, 1005, 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 考研英语词汇红宝书
(2011, 1006, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 高等数学
(2012, 1007, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 数据结构
(2013, 1008, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Nike AJ1
(2014, 1009, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2015, 1009, 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- 北面冲锋衣
(2016, 1010, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 小米台灯
(2017, 1011, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- 自行车
(2018, 1012, 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2019, 1012, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url` = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main` = new.`is_main`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 32. 补充商品详情数据（ID 71-100）
-- ===================================================================

INSERT INTO `eo_product_detail` (
    `product_id`, `description`, `create_time`, `update_time`
) VALUES
(71, '线性代数第五版，同济大学出版。内容完整，有少量笔记，不影响阅读。适合理工科学生使用。', NOW(), NOW()),
(72, '概率论与数理统计，浙大版。内容完整，有少量笔记，不影响阅读。适合理工科学生使用。', NOW(), NOW()),
(73, '大学物理实验教程，内容完整，有少量笔记。适合物理课程学习使用。', NOW(), NOW()),
(74, '考研数学真题大全解，包含近三十年真题，解析详细。适合考研数学复习使用。', NOW(), NOW()),
(75, '考研英语词汇红宝书，包含5500+核心词汇。适合考研英语复习使用。', NOW(), NOW()),
(76, '考研政治核心考点，内容全面，重点突出。适合考研政治复习使用。', NOW(), NOW()),
(77, '围城，钱钟书著，文学名著。品相良好，适合文学爱好者收藏。', NOW(), NOW()),
(78, '活着，余华著，文学名著。品相良好，适合文学爱好者收藏。', NOW(), NOW()),
(79, 'Python编程从入门到实践，经典编程书籍。内容详实，适合Python学习者使用。', NOW(), NOW()),
(80, '算法导论第三版，MIT经典教材。适合计算机专业学生学习使用。', NOW(), NOW()),
(81, 'OPPO Find X3 Pro，国行正品，使用一年。屏幕完好，电池健康，配件齐全。', NOW(), NOW()),
(82, 'vivo X70 Pro+，国行正品，使用半年。屏幕无划痕，电池健康，配件齐全。', NOW(), NOW()),
(83, '华硕ROG幻14游戏本，配置高，适合游戏和设计。使用半年，性能良好。', NOW(), NOW()),
(84, '机械革命Code01程序员笔记本，轻薄便携。使用一年，性能稳定。', NOW(), NOW()),
(85, '华为MatePad Pro 11，国行正品，屏幕完好，电池健康。适合学习和娱乐使用。', NOW(), NOW()),
(86, 'Bose QC45降噪耳机，头戴式，降噪效果一流。使用半年，配件齐全。', NOW(), NOW()),
(87, 'JBL蓝牙音箱，便携音箱，音质出色。使用三个月，配件齐全。', NOW(), NOW()),
(88, '大疆DJI Mini 2无人机，航拍神器。使用半年，配件齐全。', NOW(), NOW()),
(89, 'GoPro Hero10运动相机，防水防抖。使用三个月，配件齐全。', NOW(), NOW()),
(90, 'Xbox Series X国行，次世代游戏主机，性能强劲。配件齐全，游戏光盘另算。', NOW(), NOW()),
(91, 'H&M针织开衫M码，穿着舒适，百搭款式。适合日常穿搭。', NOW(), NOW()),
(92, 'ONLY牛仔裤26码，版型修身，穿着舒适。适合日常穿搭。', NOW(), NOW()),
(93, 'Levi\'s牛仔裤30码，经典款式，百搭百搭。适合日常穿搭。', NOW(), NOW()),
(94, 'Gap卫衣L码，穿着舒适，百搭款式。适合日常穿搭。', NOW(), NOW()),
(95, 'Converse帆布鞋41码，经典款式，百搭鞋款。穿着舒适，适合日常搭配。', NOW(), NOW()),
(96, '兰蔻小黑瓶精华50ml，全新未拆封。适合护肤使用。', NOW(), NOW()),
(97, 'SK-II神仙水230ml，使用少量，剩余90%。适合护肤使用。', NOW(), NOW()),
(98, 'MAC口红Chili小辣椒，全新未拆封。适合日常妆容。', NOW(), NOW()),
(99, '祖玛珑香水蓝风铃30ml，全新未拆封。适合日常使用。', NOW(), NOW()),
(100, '戴森吹风机HD08，国行正品，使用半年。配件齐全，功能正常。', NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `description` = new.`description`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 33. 补充商品详情数据（ID 1001-1012）
-- ===================================================================

INSERT INTO `eo_product_detail` (
    `product_id`, `description`, `create_time`, `update_time`
) VALUES
(1001, 'MacBook Pro 14寸 M3芯片 深空灰 16G+512G<br><br>【配置】M3 Pro芯片、16GB内存、512GB固态硬盘<br><br>【成色】9成新，仅使用3个月，电池循环42次<br><br>【配件】原装充电器、包装盒、说明书<br><br>【购买渠道】苹果官网购入，全国联保', NOW(), NOW()),
(1002, 'iPad Air 5 256G WiFi版 蓝色<br><br>【配置】M1芯片、256GB存储、10.9寸Liquid视网膜屏<br><br>【成色】8成新，屏幕无划痕，边框轻微使用痕迹<br><br>【配件】原装充电器、数据线<br><br>【用途】学习笔记、绘画、视频剪辑', NOW(), NOW()),
(1003, 'AirPods Pro 2 代 全新未拆封<br><br>【型号】AirPods Pro (第二代) 带MagSafe充电盒<br><br>【成色】全新未拆封，原厂塑封完整<br><br>【配件】耳机、充电盒、充电线、说明书<br><br>【保修】未激活，享受完整保修', NOW(), NOW()),
(1004, '小米13 Pro 256G 黑色<br><br>【配置】骁龙8Gen2、256GB存储、6.73寸2K屏<br><br>【成色】7成新，背面有轻微划痕，屏幕完好<br><br>【配件】原装充电器、手机壳<br><br>【功能】莱卡影像、无线充电、IP68防水', NOW(), NOW()),
(1005, 'Switch OLED 游戏机 白色<br><br>【配置】7寸OLED屏幕、64GB存储、可拆卸手柄<br><br>【成色】8成新，屏幕无划痕，底座完好<br><br>【配件】主机、底座、手柄、充电线<br><br>【游戏】不含游戏卡带，需单独购买', NOW(), NOW()),
(1006, '考研英语词汇红宝书 2024版<br><br>【版本】2024最新版，包含5500+核心词汇<br><br>【成色】6成新，有部分笔记划线<br><br>【内容】词汇分类、真题例句、记忆方法<br><br>【适合】考研英语一、英语二备考', NOW(), NOW()),
(1007, '高等数学同济第七版 上下册<br><br>【版本】第七版，同济大学数学系编<br><br>【成色】7成新，有少量笔记<br><br>【内容】函数极限、微积分、级数、空间解析几何<br><br>【适合】大一高数课程、考研数学复习', NOW(), NOW()),
(1008, '数据结构与算法 Python版<br><br>【作者】Goodrich等，机械工业出版社<br><br>【成色】8成新，书脊完好<br><br>【内容】数组、链表、树、图、排序、查找算法<br><br>【适合】计算机专业学生、算法竞赛、考研', NOW(), NOW()),
(1009, 'Nike Air Jordan 1 经典黑白 42码<br><br>【型号】AJ1 High OG 黑白熊猫配色<br><br>【尺码】42码（US 8.5）<br><br>【成色】7成新，鞋底轻微磨损，鞋面干净<br><br>【来源】得物购入，正品保障', NOW(), NOW()),
(1010, '北面冲锋衣 黑色 M码<br><br>【型号】The North Face 防水冲锋衣<br><br>【尺码】M码，适合身高170-175cm<br><br>【成色】8成新，无破损，拉链顺畅<br><br>【功能】防水透气、可拆卸内胆、多口袋设计', NOW(), NOW()),
(1011, '小米台灯Pro 护眼阅读灯<br><br>【功能】国AA级照度、无频闪、蓝光防护<br><br>【成色】9成新，使用不到2个月<br><br>【特点】智能调光、定时关灯、米家APP控制<br><br>【适用】学生学习、办公阅读', NOW(), NOW()),
(1012, '迪卡侬山地自行车 27速<br><br>【型号】Rockrider ST520 铝合金车架<br><br>【变速】27速禧玛诺变速系统<br><br>【成色】7成新，轮胎磨损正常<br><br>【配置】前后碟刹、避震前叉、水壶架', NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `description` = new.`description`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 34. 补充热门关键词数据
-- ===================================================================

INSERT INTO `eo_hot_keyword` (
    `id`, `keyword`, `search_count`, `last_search_time`, `del_flag`, `create_time`, `update_time`, `version`
) VALUES
(21, 'iPad', 734, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(22, '自行车', 654, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(23, '教材', 534, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(24, '运动鞋', 456, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(25, '耳机', 423, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(26, '电脑', 398, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(27, '手机', 378, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(28, '电动车', 356, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(29, '相机', 334, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(30, '高等数学', 312, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(31, 'PS5', 289, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(32, 'Java', 267, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(33, '瑜伽垫', 245, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(34, '护肤', 234, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0),
(35, 'Nike', 223, NOW(), 0, NOW() - INTERVAL 30 DAY, NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    `search_count` = new.`search_count`,
    `last_search_time` = new.`last_search_time`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 35. 商品评价数据（基于已完成订单）
-- 评分分布：5星50%、4星30%、3星12%、2星5%、1星3%
-- 约40%有资产方回复
-- ===================================================================

INSERT INTO `eo_product_review` (
    `id`, `product_id`, `user_id`, `order_id`, `rating`, `content`,
    `reply_content`, `reply_time`, `likes`, `status`,
    `create_time`, `update_time`, `del_flag`, `version`
) VALUES
-- 5星评价（好评）
(1,  1,  3,  1,  5, '手机成色很好，和描述一致，资产方很热情，还送了手机壳，非常满意！', '感谢支持，祝使用愉快！', NOW() - INTERVAL 57 DAY, 23, 1, NOW() - INTERVAL 58 DAY, NOW() - INTERVAL 57 DAY, 0, 0),
(2,  8,  4,  2,  5, 'AirPods 全新未拆封，正品保障，价格比官网便宜很多，超值！', '谢谢好评，欢迎再来~', NOW() - INTERVAL 52 DAY, 18, 1, NOW() - INTERVAL 53 DAY, NOW() - INTERVAL 52 DAY, 0, 0),
(3,  2,  5,  3,  5, '华为Mate60拍照太棒了，卫星通话功能很酷，资产方发货快，包装仔细。', NULL, NULL, 12, 1, NOW() - INTERVAL 48 DAY, NOW(), 0, 0),
(4,  13, 3,  11, 5, 'Switch OLED屏幕效果惊艳，掌机模式太爽了，生活必备！', '哈哈，游戏愉快！', NOW() - INTERVAL 7 DAY, 15, 1, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 7 DAY, 0, 0),
(5,  46, 14, 13, 5, '三星S24 Ultra的S Pen太好用了，AI功能也很强大，资产方服务态度超好！', '感谢认可，有问题随时联系~', NOW() - INTERVAL 9 DAY, 8, 1, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 9 DAY, 0, 0),
(6,  50, 11, 18, 5, 'Bose降噪效果一流，戴上后世界都安静了，图书馆神器！', NULL, NULL, 11, 1, NOW() - INTERVAL 17 DAY, NOW(), 0, 0),

-- 4星评价（较好）
(7,  11, 5,  9,  4, '小米手环功能齐全，NFC门禁很方便，就是腕带有点硬。', '腕带戴一段时间就软了，感谢反馈！', NOW() - INTERVAL 27 DAY, 6, 1, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 27 DAY, 0, 0),
(8,  24, 6,  10, 4, 'NB990颜值很高，穿着舒服，就是鞋底有点硬，需要磨合。', NULL, NULL, 9, 1, NOW() - INTERVAL 22 DAY, NOW(), 0, 0),
(9,  38, 8,  12, 4, '羽毛球拍手感不错，就是线断了需要重新穿，总体满意。', '抱歉线的问题，可以推荐穿线师傅', NOW() - INTERVAL 4 DAY, 4, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY, 0, 0),
(10, 47, 15, 14, 4, 'vivo X100 Pro拍照确实厉害，蔡司镜头不是盖的，就是电池续航一般。', NULL, NULL, 7, 1, NOW() - INTERVAL 6 DAY, NOW(), 0, 0),
(11, 51, 12, 19, 4, '漫步者耳机性价比很高，降噪效果不错，就是有点夹头。', '可以调节一下头梁位置试试', NOW() - INTERVAL 12 DAY, 5, 1, NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 12 DAY, 0, 0),

-- 3星评价（一般）
(12, 1,  3,  1,  3, '手机整体还行，但电池健康度只有88%，和描述的92%有差距。', '抱歉描述有误，可以退差价', NOW() - INTERVAL 58 DAY, 3, 1, NOW() - INTERVAL 58 DAY, NOW() - INTERVAL 58 DAY, 0, 0),
(13, 2,  5,  3,  3, '手机功能正常，但边框有轻微磕碰没在描述中提到，希望能更诚实。', NULL, NULL, 2, 1, NOW() - INTERVAL 48 DAY, NOW(), 0, 0),

-- 2星评价（较差）
(14, 24, 6,  10, 2, '鞋子尺码偏小，和资产方说的不一样，只能送人了。', '抱歉尺码问题，可以联系我换货', NOW() - INTERVAL 23 DAY, 1, 1, NOW() - INTERVAL 23 DAY, NOW() - INTERVAL 23 DAY, 0, 0),

-- 1星评价（差评）
(15, 11, 5,  9,  1, '手环用了两天就充不进电了，质量太差！', '可以联系售后，可能是充电器问题', NOW() - INTERVAL 28 DAY, 0, 1, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 28 DAY, 0, 0)
AS new
ON DUPLICATE KEY UPDATE
    `rating` = new.`rating`,
    `content` = new.`content`,
    `reply_content` = new.`reply_content`,
    `reply_time` = new.`reply_time`,
    `likes` = new.`likes`,
    `status` = new.`status`,
    `update_time` = new.`update_time`;
