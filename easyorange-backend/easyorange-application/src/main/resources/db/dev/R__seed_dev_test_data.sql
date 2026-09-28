-- ===================================================================
-- EasyOrange - dev 演示数据集（全量单代次造数）
-- 说明：仅在 dev / it profile 中通过 classpath:db/dev 加载；本文件是 dev 库演示数据的**唯一事实来源**，
--       刷新全库跑 scripts/db-reset.sh 后重启应用即从头重放，不需要任何补数据脚本。
-- 幂等：全部 INSERT 带 ON DUPLICATE KEY UPDATE，可重复执行；固定 ID 让重放覆盖同一批行。
-- 刷新方式：scripts/db-reset.sh -y（重放前提是该库为空库；文件不做「删除旧行」——
--       R__ 迁移只 upsert，历史行的清理由 reset 承担，不在这里写破坏性 DELETE）。
--
-- 【造数与代码语义对齐的硬约束】（改动本文件前先核这几条，改错一处演示就穿帮）：
--   ① 下单即扣库存：OrderCommandHandler.createOrderFlow 在建单事务内 decreaseStock，
--      因此**待付款订单的商品同样已扣库存**（stock=0），不付款取消才 RESTORE 恢复。
--   ② 确认收货才置 SOLD：OrderLifecycleEventConsumer.onOrderCompleted → markAsSold。
--      所以每个 COMPLETED 订单的商品一律 SOLD + stock=0；PAID / SHIPPED / PENDING_PAYMENT
--      的商品仍为 ONLINE + stock=0（货在买家流程中、平台未放款）。
--   ③ 退款与取消都 RESTORE（onOrderRefunded / onOrderCancelled），商品回到 ONLINE + stock=1。
--   ④ 库存流水按上述时点从 eo_order_item **结构化推导**（见第 9 段），不手写数值，
--      保证「eo_product.stock == 最新流水 stock_after」恒成立，StockReconcileScheduler 零漂移。
--      （推导含单件库存假设：quantity=1、初始库存=1；将来出现多件商品需改写推导式。）
--   ⑤ eo_message.conversation_id 一律留 NULL：写路径 MessageDataMapper.toEntity 根本不落该列，
--      会话列表按 sender/receiver 聚合（MessageMapper.xml），造 conv_xxx 只会与代码行为不符。
--   ⑥ 审计日志 / 请求 URL 只引用真实存在的 Controller 与路由。
--   ⑦ 每个商品必配 1 张主图（is_main=1 唯一，上架校验主图>1 会拒）；多图商品供详情页轮播演示。
--   ⑧ 固定账号密码同哈希（BCrypt），演示用；真实发布商品是 UUID v7 主键，不会落在这些数字 ID 上。
--
-- 覆盖矩阵（演示点）：
--   商品状态 ×6：DRAFT(70) PENDING_REVIEW(30) REJECTED(58) ONLINE(56) OFFLINE(16) SOLD(10)
--   订单状态 ×6：PENDING_PAYMENT(15,16) PAID(11,12) SHIPPED(13,14) COMPLETED(1-10) CANCELLED(17,18) REFUNDED(19,20)
--   testuser 四状态演示位全在本人名下（待审核/在售/下架/已售 + 草稿），管理端待审队列与「我的发布」分组都有货。
--   相机类目 6 件（24-29）为语义检索演示语料，主打查询「适合拍夜景的相机」可召回多张卡。
--   用户 16 个：1 管理员（不卖货）+ 13 活跃 + LOCKED / DISABLED 各 1（不持有在售商品）。
-- ===================================================================

START TRANSACTION;

-- ===================================================================
-- 1. 用户
-- ===================================================================

INSERT INTO `eo_user` (
    `user_id`, `username`, `password`, `user_type`, `nick_name`, `sex`, `status`,
    `email`, `phone`, `real_name`, `avatar`, `create_time`, `update_time`, `del_flag`
) VALUES
(1,  'testuser',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '橙子同学',    1, 'NORMAL',   'testuser@example.com',    '13800138001', '张橙',   'https://picsum.photos/seed/eo-avatar-1/100/100',  NOW() - INTERVAL 120 DAY, NOW(), 0),
(2,  'admin',        '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '02', '平台管理员', 1, 'NORMAL',   'admin@example.com',       '13800138002', '易橙',   'https://picsum.photos/seed/eo-avatar-2/100/100',  NOW() - INTERVAL 365 DAY, NOW(), 0),
(3,  'liming',       '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '黎明',        1, 'NORMAL',   'liming@example.com',      '13800138003', '黎明',   'https://picsum.photos/seed/eo-avatar-3/100/100',  NOW() - INTERVAL 100 DAY, NOW(), 0),
(4,  'wangfang',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '考研的小方',  2, 'NORMAL',   'wangfang@example.com',    '13800138004', '王芳',   'https://picsum.photos/seed/eo-avatar-4/100/100',  NOW() - INTERVAL 80 DAY,  NOW(), 0),
(5,  'zhaowei',      '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '赵伟',        1, 'NORMAL',   'zhaowei@example.com',     '13800138005', '赵伟',   'https://picsum.photos/seed/eo-avatar-5/100/100',  NOW() - INTERVAL 60 DAY,  NOW(), 0),
(6,  'sunli',        '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '孙丽',        2, 'NORMAL',   'sunli@example.com',       '13800138006', '孙丽',   'https://picsum.photos/seed/eo-avatar-6/100/100',  NOW() - INTERVAL 55 DAY,  NOW(), 0),
(7,  'zhouyang',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '周洋',        1, 'NORMAL',   'zhouyang@example.com',    '13800138007', '周洋',   'https://picsum.photos/seed/eo-avatar-7/100/100',  NOW() - INTERVAL 50 DAY,  NOW(), 0),
(8,  'chenxiao',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '陈晓',        1, 'NORMAL',   'chenxiao@example.com',    '13800138008', '陈晓',   'https://picsum.photos/seed/eo-avatar-8/100/100',  NOW() - INTERVAL 45 DAY,  NOW(), 0),
(9,  'huangjie',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '黄杰学长',    1, 'NORMAL',   'huangjie@example.com',    '13800138009', '黄杰',   'https://picsum.photos/seed/eo-avatar-9/100/100',  NOW() - INTERVAL 95 DAY,  NOW(), 0),
(10, 'liuyan',       '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '刘燕',        2, 'NORMAL',   'liuyan@example.com',      '13800138010', '刘燕',   'https://picsum.photos/seed/eo-avatar-10/100/100', NOW() - INTERVAL 40 DAY,  NOW(), 0),
(11, 'wanghai',      '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '王海',        1, 'NORMAL',   'wanghai@example.com',     '13800138011', '王海',   'https://picsum.photos/seed/eo-avatar-11/100/100', NOW() - INTERVAL 35 DAY,  NOW(), 0),
(12, 'zhangmei',     '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '张梅',        2, 'NORMAL',   'zhangmei@example.com',    '13800138012', '张梅',   'https://picsum.photos/seed/eo-avatar-12/100/100', NOW() - INTERVAL 30 DAY,  NOW(), 0),
(13, 'qianlei',      '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '钱磊',        1, 'NORMAL',   'qianlei@example.com',     '13800138013', '钱磊',   'https://picsum.photos/seed/eo-avatar-13/100/100', NOW() - INTERVAL 25 DAY,  NOW(), 0),
(14, 'liguang',      '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '李光',        1, 'NORMAL',   'liguang@example.com',     '13800138014', '李光',   'https://picsum.photos/seed/eo-avatar-14/100/100', NOW() - INTERVAL 20 DAY,  NOW(), 0),
(15, 'lockeduser',   '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '连续登录失败', 2, 'LOCKED',   'locked@example.com',      '13800138015', '刘锁',   NULL,                                            NOW() - INTERVAL 15 DAY,  NOW(), 0),
(16, 'disableduser', '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK', '01', '违规暂停中',  2, 'DISABLED', 'disabled@example.com',    '13800138016', '吴禁',   NULL,                                            NOW() - INTERVAL 12 DAY,  NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    `nick_name`   = new.`nick_name`,
    `sex`         = new.`sex`,
    `status`      = new.`status`,
    `del_flag`    = new.`del_flag`,
    `update_time` = NOW();

-- ===================================================================
-- 2. 商品（70 件：覆盖全部 18 个二级类目 + 六种商品状态）
--     condition_level 与描述文案强绑定：1 全新/未拆封 · 2 几乎全新(95新~9成)
--     · 3 轻微使用痕迹(8~7成) · 4 明显使用痕迹(6成及以下)
--     stock 口径见文件头约束 ①②③：完成订单 0 / 在途订单 0 / 其余 1
--     search_text = 名称 + 标签词，供 ngram 全文降级检索
-- ===================================================================

INSERT INTO `eo_product` (
    `id`, `user_id`, `category_id`, `name`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `contact_method`, `tags`, `search_text`, `del_flag`, `create_time`, `update_time`
) VALUES
-- 电子数码 - 手机(10)
(1,  1,  '018bcfe5-7f70-76f0-8478-82e36b0d549b', 'iPhone 14 Pro Max 256G 暗紫色',           5999.00,  8999.00, 1, 'ONLINE',         328, '2', '同城面交',   '微信: testuser_wx',  '苹果,手机,旗舰',            'iPhone 14 Pro Max 256G 暗紫色 苹果 手机 旗舰',            0, NOW() - INTERVAL 45 DAY, NOW()),
(2,  3,  '018bcfe5-7f70-76f0-8478-82e36b0d549b', '华为 Mate 60 Pro 512G 雅丹黑',            4599.00,  6999.00, 1, 'ONLINE',         256, '3', '东校区',     '微信: liming_wx',    '华为,手机,影像,卫星通话',   '华为 Mate 60 Pro 512G 雅丹黑 华为 手机 影像 卫星通话',    0, NOW() - INTERVAL 40 DAY, NOW()),
(3,  5,  '018bcfe5-7f70-76f0-8478-82e36b0d549b', '小米14 Ultra 16+512 白色',                3999.00,  5999.00, 1, 'ONLINE',         189, '3', '图书馆',     '微信: zhaowei_wx',   '小米,手机,徕卡',            '小米14 Ultra 16+512 白色 小米 手机 徕卡',                0, NOW() - INTERVAL 30 DAY, NOW()),
(4,  4,  '018bcfe5-7f70-76f0-8478-82e36b0d549b', 'OPPO Find X7 Ultra 天青蓝',               3299.00,  5499.00, 0, 'SOLD',           412, '3', '南校区',     '微信: wangfang_wx',  'OPPO,手机,影像',            'OPPO Find X7 Ultra 天青蓝 OPPO 手机 影像',               0, NOW() - INTERVAL 75 DAY, NOW()),
(5,  9,  '018bcfe5-7f70-76f0-8478-82e36b0d549b', '三星 Galaxy S24 Ultra 12+256 钛灰',       5499.00,  7999.00, 1, 'ONLINE',         198, '2', '同城面交',   '微信: huangjie_wx',  '三星,手机,手写笔',          '三星 Galaxy S24 Ultra 12+256 钛灰 三星 手机 手写笔',      0, NOW() - INTERVAL 22 DAY, NOW()),
(6,  10,  '018bcfe5-7f70-76f0-8478-82e36b0d549b', 'vivo X100 Pro 16+512 落日橙',             3499.00,  4999.00, 1, 'ONLINE',         156, '2', '教学楼',     '微信: liuyan_wx',    'vivo,手机,蔡司影像',        'vivo X100 Pro 16+512 落日橙 vivo 手机 蔡司影像',          0, NOW() - INTERVAL 18 DAY, NOW()),
-- 电子数码 - 电脑(11)
(7,  1,  '018bcfe5-8358-73d9-a344-5bb31738f7d9', 'MacBook Air M2 13寸 16+512 深空灰',        6499.00,  8999.00, 0, 'SOLD',           286, '3', '图书馆',     '微信: testuser_wx',  '苹果,笔记本,轻薄,M2',        'MacBook Air M2 13寸 16+512 深空灰 苹果 笔记本 轻薄 M2',   0, NOW() - INTERVAL 70 DAY, NOW()),
(8,  7,  '018bcfe5-8358-73d9-a344-5bb31738f7d9', 'ThinkPad X1 Carbon Gen11 14寸',           5299.00,  8499.00, 1, 'ONLINE',         134, '4', '计算机学院', '微信: zhouyang_wx',  'ThinkPad,笔记本,商务',     'ThinkPad X1 Carbon Gen11 14寸 ThinkPad 笔记本 商务',     0, NOW() - INTERVAL 35 DAY, NOW()),
(9,  11,  '018bcfe5-8358-73d9-a344-5bb31738f7d9', '联想拯救者 Y9000P 2024 i9款',            7999.00,  9999.00, 1, 'ONLINE',         289, '3', '宿舍区',     '微信: wanghai_wx',   '联想,游戏本,RTX4060',       '联想拯救者 Y9000P 2024 i9款 联想 游戏本 RTX4060',       0, NOW() - INTERVAL 28 DAY, NOW()),
(10,  12,  '018bcfe5-8358-73d9-a344-5bb31738f7d9', '华硕天选5 Pro 锐龙版 16寸',               5999.00,  7499.00, 1, 'ONLINE',         167, '3', '同城面交',   '微信: zhangmei_wx',  '华硕,游戏本,锐龙',          '华硕天选5 Pro 锐龙版 16寸 华硕 游戏本 锐龙',             0, NOW() - INTERVAL 20 DAY, NOW()),
(11,  1,  '018bcfe5-8358-73d9-a344-5bb31738f7d9', 'MacBook Pro 14寸 M3 Pro 16+512',         11999.00, 14999.00, 0, 'SOLD',           355, '2', '图书馆',     '微信: testuser_wx',  '苹果,笔记本,高性能,M3',     'MacBook Pro 14寸 M3 Pro 16+512 苹果 笔记本 高性能 M3',   0, NOW() - INTERVAL 65 DAY, NOW()),
-- 电子数码 - 耳机音箱(12)
(12,  1,  '018bcfe5-8740-76ca-b4eb-252b0f21ddb6', 'AirPods Pro 2 全新未拆封',                 1299.00,  1899.00, 0, 'ONLINE',         456, '1', '同城面交',   '微信: testuser_wx',  '苹果,耳机,降噪,未拆封',      'AirPods Pro 2 全新未拆封 苹果 耳机 降噪 未拆封',          0, NOW() - INTERVAL 33 DAY, NOW()),
(13,  6,  '018bcfe5-8740-76ca-b4eb-252b0f21ddb6', 'Sony WH-1000XM5 头戴降噪耳机',           1599.00,  2499.00, 1, 'ONLINE',         198, '3', '南校区',     '微信: sunli_wx',     '索尼,耳机,降噪',            'Sony WH-1000XM5 头戴降噪耳机 索尼 耳机 降噪',            0, NOW() - INTERVAL 26 DAY, NOW()),
(14,  14,  '018bcfe5-8740-76ca-b4eb-252b0f21ddb6', 'Bose QC45 头戴消噪耳机 黑色',             1299.00,  2299.00, 1, 'ONLINE',         134, '3', '图书馆',     '微信: liguang_wx',   'Bose,耳机,消噪',            'Bose QC45 头戴消噪耳机 黑色 Bose 耳机 消噪',            0, NOW() - INTERVAL 18 DAY, NOW()),
(15,  8,  '018bcfe5-8740-76ca-b4eb-252b0f21ddb6', 'JBL Charge 5 蓝牙音箱 黑色',               599.00,   899.00, 1, 'ONLINE',          87, '4', '操场',       '微信: chenxiao_wx',  'JBL,音箱,蓝牙,防水',         'JBL Charge 5 蓝牙音箱 黑色 JBL 音箱 蓝牙 防水',          0, NOW() - INTERVAL 15 DAY, NOW()),
(16,  1,  '018bcfe5-8740-76ca-b4eb-252b0f21ddb6', 'AKG K72 头戴监听耳机',                     329.00,   499.00, 1, 'OFFLINE',         77, '3', '教学楼',     '微信: testuser_wx',  'AKG,耳机,监听',             'AKG K72 头戴监听耳机 AKG 耳机 监听',                    0, NOW() - INTERVAL 50 DAY, NOW()),
-- 电子数码 - 智能穿戴(13)
(17,  1,  '018bcfe5-8b28-790c-bca3-04171fb17c23', '小米手环8 NFC版 黑色',                      299.00,   349.00, 0, 'ONLINE',         178, '3', '体育馆',     '微信: testuser_wx',  '小米,手环,NFC',             '小米手环8 NFC版 黑色 小米 手环 NFC',                    0, NOW() - INTERVAL 38 DAY, NOW()),
(18,  4,  '018bcfe5-8b28-790c-bca3-04171fb17c23', 'Apple Watch SE 2代 40mm 星光色',          1499.00,  1999.00, 1, 'ONLINE',         145, '3', '同城面交',   '微信: wangfang_wx',  '苹果,手表,健康',            'Apple Watch SE 2代 40mm 星光色 苹果 手表 健康',          0, NOW() - INTERVAL 21 DAY, NOW()),
(19,  13,  '018bcfe5-8b28-790c-bca3-04171fb17c23', '华为 Watch GT4 46mm 棕色皮带',             899.00,  1488.00, 0, 'ONLINE',         112, '2', '东校区',     '微信: qianlei_wx',   '华为,手表,运动',            '华为 Watch GT4 46mm 棕色皮带 华为 手表 运动',            0, NOW() - INTERVAL 16 DAY, NOW()),
-- 电子数码 - 游戏设备(14)
(20,  5,  '018bcfe5-8f10-7392-a827-ddada170b338', 'Switch OLED 白色 含底座',                  1599.00,  2599.00, 1, 'ONLINE',         267, '3', '同城面交',   '微信: zhaowei_wx',   '任天堂,游戏机,OLED',        'Switch OLED 白色 含底座 任天堂 游戏机 OLED',           0, NOW() - INTERVAL 44 DAY, NOW()),
(21,  7,  '018bcfe5-8f10-7392-a827-ddada170b338', 'PS5 光驱版 国行主机',                      2899.00,  3899.00, 0, 'ONLINE',         312, '3', '宿舍区',     '微信: zhouyang_wx',  '索尼,游戏机,PS5',           'PS5 光驱版 国行主机 索尼 游戏机 PS5',                  0, NOW() - INTERVAL 32 DAY, NOW()),
(22,  9,  '018bcfe5-8f10-7392-a827-ddada170b338', 'Xbox Series X 国行 1TB',                    2999.00,  3799.00, 0, 'SOLD',           278, '2', '同城面交',   '微信: huangjie_wx',  '微软,游戏机,Xbox',          'Xbox Series X 国行 1TB 微软 游戏机 Xbox',              0, NOW() - INTERVAL 58 DAY, NOW()),
(23,  11,  '018bcfe5-8f10-7392-a827-ddada170b338', 'Steam Deck OLED 512G 掌机',                3299.00,  4099.00, 1, 'ONLINE',         234, '2', '计算机学院', '微信: wanghai_wx',   'Steam,掌机,OLED',           'Steam Deck OLED 512G 掌机 Steam 掌机 OLED',            0, NOW() - INTERVAL 19 DAY, NOW()),
-- 电子数码 - 相机(15) — 语义检索演示语料，「适合拍夜景的相机」需召回多张卡
(24,  1,  '018bcfe5-92f8-7953-83f5-8c3cf29d0da9', '佳能 EOS R50 微单相机套机',                 4399.00,  5699.00, 1, 'ONLINE',         156, '2', '同城面交',   '微信: testuser_wx',  '佳能,微单,相机,夜景',        '佳能 EOS R50 微单相机套机 佳能 微单 相机 夜景',        0, NOW() - INTERVAL 29 DAY, NOW()),
(25,  9,  '018bcfe5-92f8-7953-83f5-8c3cf29d0da9', '索尼 A7M3 全画幅微单 机身',                6799.00,  9999.00, 1, 'ONLINE',         231, '3', '图书馆',     '微信: huangjie_wx',  '索尼,全画幅,相机,高感夜景',  '索尼 A7M3 全画幅微单 机身 索尼 全画幅 相机 高感 夜景',  0, NOW() - INTERVAL 36 DAY, NOW()),
(26,  5,  '018bcfe5-92f8-7953-83f5-8c3cf29d0da9', '尼康 Z5 入门全画幅 套机',                  5499.00,  7999.00, 1, 'ONLINE',         118, '2', '同城面交',   '微信: zhaowei_wx',   '尼康,全画幅,相机,防抖夜景',  '尼康 Z5 入门全画幅 套机 尼康 全画幅 相机 防抖 夜景',  0, NOW() - INTERVAL 17 DAY, NOW()),
(27,  3,  '018bcfe5-92f8-7953-83f5-8c3cf29d0da9', '富士 X-T30 II 复古微单',                   5899.00,  7499.00, 1, 'ONLINE',          97, '2', '南校区',     '微信: liming_wx',    '富士,微单,相机,复古夜景',    '富士 X-T30 II 复古微单 富士 微单 相机 复古 夜景',      0, NOW() - INTERVAL 26 DAY, NOW()),
(28,  9,  '018bcfe5-92f8-7953-83f5-8c3cf29d0da9', '佳能 200D II 单反相机 套机',               3299.00,  4599.00, 0, 'ONLINE',          86, '3', '宿舍区',     '微信: huangjie_wx',  '佳能,单反,相机,入门',        '佳能 200D II 单反相机 套机 佳能 单反 相机 入门',      0, NOW() - INTERVAL 12 DAY, NOW()),
(29,  11,  '018bcfe5-92f8-7953-83f5-8c3cf29d0da9', 'GoPro Hero12 Black 运动相机',              2699.00,  3398.00, 1, 'ONLINE',         142, '2', '体育馆',     '微信: wanghai_wx',   'GoPro,运动相机,夜景防抖',    'GoPro Hero12 Black 运动相机 GoPro 运动相机 夜景 防抖',  0, NOW() - INTERVAL 23 DAY, NOW()),
(30,  1,  '018bcfe5-92f8-7953-83f5-8c3cf29d0da9', '索尼 ZV-1 便携数码相机',                   3499.00,  4299.00, 1, 'PENDING_REVIEW',  64, '3', '宿舍区',     '微信: testuser_wx',  '索尼,便携相机,vlog',         '索尼 ZV-1 便携数码相机 索尼 便携 相机 vlog',          0, NOW() - INTERVAL 5 DAY,  NOW()),
-- 书籍教材 - 教材(20)
(31,  1,  '018bcfe5-96e0-793b-9963-368595e60af5', '高等数学 同济第七版 上下册',                  45.00,    89.00, 1, 'ONLINE',          67, '3', '教学楼',     '微信: testuser_wx',  '教材,数学,高数',             '高等数学 同济第七版 上下册 教材 数学 高数',            0, NOW() - INTERVAL 48 DAY, NOW()),
(32,  8,  '018bcfe5-96e0-793b-9963-368595e60af5', '线性代数 第五版 同济',                      12.00,    32.00, 1, 'ONLINE',          89, '3', '图书馆',     '微信: chenxiao_wx',  '教材,数学,线性代数',         '线性代数 第五版 同济 教材 数学 线性代数',              0, NOW() - INTERVAL 41 DAY, NOW()),
(33,  8,  '018bcfe5-96e0-793b-9963-368595e60af5', '数据结构 C语言版 严蔚敏',                    30.00,    45.00, 1, 'ONLINE',         178, '3', '计算机学院', '微信: chenxiao_wx',  '教材,数据结构,考研408',      '数据结构 C语言版 严蔚敏 教材 数据结构 考研408',          0, NOW() - INTERVAL 37 DAY, NOW()),
(34,  4,  '018bcfe5-96e0-793b-9963-368595e60af5', 'C语言程序设计 第五版 谭浩强',                25.00,    49.00, 0, 'SOLD',           201, '4', '教学楼',     '微信: wangfang_wx',  '教材,C语言,编程入门',        'C语言程序设计 第五版 谭浩强 教材 C语言 编程入门',        0, NOW() - INTERVAL 55 DAY, NOW()),
(35,  6,  '018bcfe5-96e0-793b-9963-368595e60af5', '大学物理 上下册 第四版',                     55.00,    89.00, 1, 'ONLINE',          67, '4', '教学楼',     '微信: sunli_wx',     '教材,物理,大学',             '大学物理 上下册 第四版 教材 物理 大学',                0, NOW() - INTERVAL 33 DAY, NOW()),
-- 书籍教材 - 考研资料(21)
(36,  4,  '018bcfe5-9ac8-70cb-8e26-3464f9ebdacc', '张宇考研数学基础30讲 2025版',                 45.00,    79.00, 1, 'ONLINE',         178, '3', '图书馆',     '微信: wangfang_wx',  '考研,数学,张宇',             '张宇考研数学基础30讲 2025版 考研 数学 张宇',            0, NOW() - INTERVAL 27 DAY, NOW()),
(37,  4,  '018bcfe5-9ac8-70cb-8e26-3464f9ebdacc', '肖秀荣考研政治全套 2025版',                   89.00,   158.00, 1, 'ONLINE',         345, '3', '图书馆',     '微信: wangfang_wx',  '考研,政治,肖秀荣',          '肖秀荣考研政治全套 2025版 考研 政治 肖秀荣',            0, NOW() - INTERVAL 22 DAY, NOW()),
(38,  6,  '018bcfe5-9ac8-70cb-8e26-3464f9ebdacc', '考研英语词汇红宝书 2025版',                   35.00,    68.00, 0, 'SOLD',           234, '4', '南校区',     '微信: sunli_wx',     '考研,英语,词汇',             '考研英语词汇红宝书 2025版 考研 英语 词汇',            0, NOW() - INTERVAL 52 DAY, NOW()),
(39,  8,  '018bcfe5-9ac8-70cb-8e26-3464f9ebdacc', '汤家凤考研数学1800题',                        25.00,    49.00, 1, 'ONLINE',         123, '4', '计算机学院', '微信: chenxiao_wx',  '考研,数学,汤家凤',          '汤家凤考研数学1800题 考研 数学 汤家凤',               0, NOW() - INTERVAL 14 DAY, NOW()),
-- 书籍教材 - 课外读物(22)
(40,  13,  '018bcfe5-9eb0-70be-b6f1-25b28e81973e', '三体 全三册 精装典藏版',                      68.00,   128.00, 1, 'ONLINE',         312, '2', '同城面交',   '微信: qianlei_wx',   '课外,科幻,三体',             '三体 全三册 精装典藏版 课外 科幻 三体',                0, NOW() - INTERVAL 25 DAY, NOW()),
(41,  6,  '018bcfe5-9eb0-70be-b6f1-25b28e81973e', '人类简史 从动物到上帝 精装',                   28.00,    49.00, 1, 'ONLINE',          56, '2', '图书馆',     '微信: sunli_wx',     '课外,历史,人类简史',         '人类简史 从动物到上帝 精装 课外 历史 人类简史',        0, NOW() - INTERVAL 13 DAY, NOW()),
(42,  8,  '018bcfe5-9eb0-70be-b6f1-25b28e81973e', '深入理解计算机系统 CSAPP 第三版',              89.00,   139.00, 1, 'ONLINE',         267, '3', '计算机学院', '微信: chenxiao_wx',  '计算机,经典,系统编程',      '深入理解计算机系统 CSAPP 第三版 计算机 经典 系统编程',  0, NOW() - INTERVAL 20 DAY, NOW()),
(43,  10,  '018bcfe5-9eb0-70be-b6f1-25b28e81973e', '活着 余华',                                   15.00,    35.00, 1, 'ONLINE',         112, '2', '东校区',     '微信: liuyan_wx',    '小说,文学,名著',             '活着 余华 小说 文学 名著',                            0, NOW() - INTERVAL 9 DAY,  NOW()),
-- 服饰鞋包 - 鞋靴(30)
(44,  3,  '018bcfe5-a298-7221-9ad3-2c904a23d596', 'Nike Air Jordan 1 黑白 42码',                699.00,  1299.00, 1, 'ONLINE',         289, '3', '操场',       '微信: liming_wx',    'Nike,球鞋,Jordan',           'Nike Air Jordan 1 黑白 42码 Nike 球鞋 Jordan',         0, NOW() - INTERVAL 31 DAY, NOW()),
(45,  4,  '018bcfe5-a298-7221-9ad3-2c904a23d596', 'New Balance 990v6 元祖灰 38码',              899.00,  1499.00, 1, 'ONLINE',         167, '3', '同城面交',   '微信: wangfang_wx',  'NB,跑鞋,复古',               'New Balance 990v6 元祖灰 38码 NB 跑鞋 复古',          0, NOW() - INTERVAL 24 DAY, NOW()),
(46,  7,  '018bcfe5-a298-7221-9ad3-2c904a23d596', 'Converse 1970s 黑色高帮 43码',               259.00,   459.00, 0, 'SOLD',           145, '3', '操场',       '微信: zhouyang_wx',  '匡威,帆布鞋,经典',           'Converse 1970s 黑色高帮 43码 匡威 帆布鞋 经典',        0, NOW() - INTERVAL 42 DAY, NOW()),
-- 服饰鞋包 - 服装(31)
(47,  6,  '018bcfe5-a680-724e-8789-e8708a6a63ec', '北面冲锋衣 黑色 M码',                         399.00,   899.00, 1, 'ONLINE',         156, '3', '同城面交',   '微信: sunli_wx',     '北面,冲锋衣,户外',           '北面冲锋衣 黑色 M码 北面 冲锋衣 户外',                  0, NOW() - INTERVAL 28 DAY, NOW()),
(48,  10,  '018bcfe5-a680-724e-8789-e8708a6a63ec', 'Patagonia 抓绒衣 蓝色 M码',                   349.00,   699.00, 1, 'ONLINE',          98, '3', '南校区',     '微信: liuyan_wx',    'Patagonia,抓绒,户外',        'Patagonia 抓绒衣 蓝色 M码 Patagonia 抓绒 户外',        0, NOW() - INTERVAL 15 DAY, NOW()),
(49,  12,  '018bcfe5-a680-724e-8789-e8708a6a63ec', '优衣库轻型羽绒服 黑色 L码',                   199.00,   499.00, 1, 'ONLINE',          78, '3', '宿舍区',     '微信: zhangmei_wx',  '优衣库,羽绒服,冬季',         '优衣库轻型羽绒服 黑色 L码 优衣库 羽绒服 冬季',          0, NOW() - INTERVAL 10 DAY, NOW()),
-- 服饰鞋包 - 箱包(32)
(50,  12,  '018bcfe5-aa68-7922-a3db-41564ef8aa38', 'Fjallraven Kanken 双肩包 森林绿',            599.00,   899.00, 1, 'ONLINE',         167, '2', '同城面交',   '微信: zhangmei_wx',  '北极狐,双肩包,经典',         'Fjallraven Kanken 双肩包 森林绿 北极狐 双肩包 经典',  0, NOW() - INTERVAL 26 DAY, NOW()),
(51,  5,  '018bcfe5-aa68-7922-a3db-41564ef8aa38', 'Nike 运动双肩包 黑色 30L',                   159.00,   299.00, 1, 'ONLINE',         112, '3', '体育馆',     '微信: zhaowei_wx',   'Nike,双肩包,运动',           'Nike 运动双肩包 黑色 30L Nike 双肩包 运动',            0, NOW() - INTERVAL 21 DAY, NOW()),
-- 生活用品 - 宿舍资产(40)
(52,  1,  '018bcfe5-ae50-7d0e-8b91-0562ae97ba94', '小米台灯Pro 护眼阅读灯',                      89.00,   149.00, 1, 'ONLINE',         167, '2', '宿舍区',     '微信: testuser_wx',  '小米,台灯,护眼',             '小米台灯Pro 护眼阅读灯 小米 台灯 护眼',                0, NOW() - INTERVAL 39 DAY, NOW()),
(53,  6,  '018bcfe5-ae50-7d0e-8b91-0562ae97ba94', '懒人加湿器 4.5L 静音款',                      89.00,   159.00, 0, 'SOLD',           134, '2', '南校区',     '微信: sunli_wx',     '加湿器,静音,家用',           '懒人加湿器 4.5L 静音款 加湿器 静音 家用',              0, NOW() - INTERVAL 52 DAY, NOW()),
(54,  14,  '018bcfe5-ae50-7d0e-8b91-0562ae97ba94', '得力碎纸机 家用4级保密',                     199.00,   399.00, 1, 'ONLINE',          56, '3', '计算机学院', '微信: liguang_wx',   '得力,碎纸机,办公',          '得力碎纸机 家用4级保密 得力 碎纸机 办公',              0, NOW() - INTERVAL 11 DAY, NOW()),
(55,  12,  '018bcfe5-ae50-7d0e-8b91-0562ae97ba94', '小米空气净化器4 Lite',                       399.00,   699.00, 1, 'ONLINE',         123, '2', '宿舍区',     '微信: zhangmei_wx',  '小米,净化器,静音',           '小米空气净化器4 Lite 小米 净化器 静音',                0, NOW() - INTERVAL 18 DAY, NOW()),
-- 生活用品 - 数码配件(41)
(56,  7,  '018bcfe5-b238-71a6-a48e-9cda94e3bf91', 'Anker 65W 氮化镓充电器',                     129.00,   199.00, 1, 'ONLINE',          78, '2', '同城面交',   '微信: zhouyang_wx',  'Anker,充电器,氮化镓',        'Anker 65W 氮化镓充电器 Anker 充电器 氮化镓',            0, NOW() - INTERVAL 13 DAY, NOW()),
(57,  13,  '018bcfe5-b238-71a6-a48e-9cda94e3bf91', '罗技 MX Master 3S 无线鼠标',                499.00,   749.00, 1, 'ONLINE',         189, '2', '图书馆',     '微信: qianlei_wx',   '罗技,鼠标,办公',            '罗技 MX Master 3S 无线鼠标 罗技 鼠标 办公',            0, NOW() - INTERVAL 17 DAY, NOW()),
(58,  1,  '018bcfe5-b238-71a6-a48e-9cda94e3bf91', 'Apple Magic Keyboard 妙控键盘',              999.00,  1499.00, 1, 'REJECTED',        41, '1', '宿舍区',     '微信: testuser_wx',  '苹果,键盘,妙控',             'Apple Magic Keyboard 妙控键盘 苹果 键盘 妙控',        0, NOW() - INTERVAL 7 DAY,  NOW()),
(59,  8,  '018bcfe5-b238-71a6-a48e-9cda94e3bf91', '绿联 Type-C 扩展坞 7合1',                     89.00,   159.00, 1, 'ONLINE',          45, '3', '计算机学院', '微信: chenxiao_wx',  '绿联,扩展坞,Type-C',         '绿联 Type-C 扩展坞 7合1 绿联 扩展坞 Type-C',          0, NOW() - INTERVAL 6 DAY,  NOW()),
-- 运动健身 - 健身器材(50)
(60,  5,  '018bcfe5-b620-7a38-97d5-5c80301850c5', '可调节哑铃 20kg 单只',                       159.00,   299.00, 1, 'ONLINE',          67, '4', '体育馆',     '微信: zhaowei_wx',   '哑铃,健身,可调节',          '可调节哑铃 20kg 单只 哑铃 健身 可调节',                0, NOW() - INTERVAL 16 DAY, NOW()),
(61,  6,  '018bcfe5-b620-7a38-97d5-5c80301850c5', '健身瑜伽垫 TPE 加厚防滑',                      69.00,    99.00, 1, 'ONLINE',          98, '3', '体育馆',     '微信: sunli_wx',     '瑜伽垫,健身,TPE',           '健身瑜伽垫 TPE 加厚防滑 瑜伽垫 健身 TPE',              0, NOW() - INTERVAL 8 DAY,  NOW()),
-- 运动健身 - 户外运动(51)
(62,  1,  '018bcfe5-ba08-718f-ad93-39088c38fb29', '迪卡侬山地自行车 ST520 27速',                899.00,  1599.00, 0, 'SOLD',           234, '3', '操场',       '微信: testuser_wx',  '迪卡侬,自行车,山地车',       '迪卡侬山地自行车 ST520 27速 迪卡侬 自行车 山地车',        0, NOW() - INTERVAL 47 DAY, NOW()),
(63,  10,  '018bcfe5-ba08-718f-ad93-39088c38fb29', '捷安特 ATX860 山地车 27.5寸',                1299.00,  2198.00, 0, 'ONLINE',         198, '3', '操场',       '微信: liuyan_wx',    '捷安特,自行车,山地车',       '捷安特 ATX860 山地车 27.5寸 捷安特 自行车 山地车',      0, NOW() - INTERVAL 34 DAY, NOW()),
(64,  7,  '018bcfe5-ba08-718f-ad93-39088c38fb29', '尤尼克斯羽毛球拍 ARC-7',                     289.00,   450.00, 1, 'ONLINE',          56, '3', '体育馆',     '微信: zhouyang_wx',  '尤尼克斯,羽毛球拍,全碳素',   '尤尼克斯羽毛球拍 ARC-7 尤尼克斯 羽毛球拍 全碳素',        0, NOW() - INTERVAL 12 DAY, NOW()),
-- 虚拟物品 - 游戏账号(60)
(65,  5,  '018bcfe5-bdf0-7101-83d0-816d907a70c3', '原神 60级 全图鉴账号 官服',                    599.00,   NULL, 1, 'ONLINE',         345, '1', '线上交易',   '微信: zhaowei_wx',   '原神,游戏账号,全图鉴',       '原神 60级 全图鉴账号 官服 原神 游戏账号 全图鉴',        0, NOW() - INTERVAL 10 DAY, NOW()),
(66,  11,  '018bcfe5-bdf0-7101-83d0-816d907a70c3', '王者荣耀 V10 贵族号 100+皮肤',                299.00,   NULL, 1, 'ONLINE',         456, '1', '线上交易',   '微信: wanghai_wx',   '王者荣耀,游戏账号,贵族',      '王者荣耀 V10 贵族号 100+皮肤 王者荣耀 游戏账号 贵族',   0, NOW() - INTERVAL 7 DAY,  NOW()),
-- 虚拟物品 - 会员卡券(61)
(67,  6,  '018bcfe5-c1d8-79e7-9fc5-414934b9b5df', '网易云音乐年卡VIP 黑胶',                       88.00,   158.00, 0, 'SOLD',            67, '1', '线上交易',   '微信: sunli_wx',     '网易云,音乐,会员',           '网易云音乐年卡VIP 黑胶 网易云 音乐 会员',              0, NOW() - INTERVAL 18 DAY, NOW()),
(68,  3,  '018bcfe5-c1d8-79e7-9fc5-414934b9b5df', '哔哩哔哩大会员 剩余10个月',                     45.00,    98.00, 1, 'ONLINE',         128, '1', '线上交易',   '微信: liming_wx',    '哔哩哔哩,会员,视频',         '哔哩哔哩大会员 剩余10个月 哔哩哔哩 会员 视频',        0, NOW() - INTERVAL 4 DAY,  NOW()),
-- 虚拟物品 - 数字素材(62)
(69,  8,  '018bcfe5-c5c0-7ae2-9b5d-ac1f881ed162', 'Figma 设计系统 UI 组件包',                     129.00,   299.00, 1, 'ONLINE',          89, '1', '线上交易',   '微信: chenxiao_wx',  'Figma,设计,素材',            'Figma 设计系统 UI 组件包 Figma 设计 素材',              0, NOW() - INTERVAL 3 DAY,  NOW()),
(70,  1,  '018bcfe5-c5c0-7ae2-9b5d-ac1f881ed162', 'Notion 个人版会员 剩余8个月',                  199.00,   388.00, 1, 'DRAFT',            0, '1', '线上交易',   '微信: testuser_wx',  'Notion,会员,效率',           'Notion 个人版会员 剩余8个月 Notion 会员 效率',          0, NOW() - INTERVAL 2 DAY,  NOW())
AS new
ON DUPLICATE KEY UPDATE
    `user_id`         = new.`user_id`,
    `category_id`     = new.`category_id`,
    `name`            = new.`name`,
    `price`           = new.`price`,
    `original_price`  = new.`original_price`,
    `stock`           = new.`stock`,
    `status`          = new.`status`,
    `view_count`      = new.`view_count`,
    `condition_level` = new.`condition_level`,
    `location`        = new.`location`,
    `contact_method`  = new.`contact_method`,
    `tags`            = new.`tags`,
    `search_text`     = new.`search_text`,
    `del_flag`        = new.`del_flag`,
    `update_time`     = NOW();

-- ===================================================================
-- 3. 商品详情（1:1；【成色】段措辞与 condition_level 严格对应）
-- ===================================================================

INSERT INTO `eo_product_detail` (
    `product_id`, `description`, `create_time`, `update_time`
) VALUES
(1,  'iPhone 14 Pro Max 256G 暗紫色 国行在保<br><br>【配置】256G 存储、灵动岛、全网通 5G<br><br>【成色】95新，轻微使用痕迹，屏幕无划痕，电池健康度 92%<br><br>【配件】原装充电线、说明书<br><br>【渠道】官网购入，有电子凭证', NOW() - INTERVAL 45 DAY, NOW()),
(2,  '华为 Mate 60 Pro 512G 雅丹黑 国行在保<br><br>【配置】512G 存储、昆仑玻璃、卫星通话<br><br>【成色】8成新，一直贴膜使用，边框有轻微磕碰，屏幕完好<br><br>【配件】原装充电器、数据线、手机壳', NOW() - INTERVAL 40 DAY, NOW()),
(3,  '小米14 Ultra 16+512 白色 徕卡影像旗舰<br><br>【配置】骁龙 8Gen3、16G+512G、1 英寸徕卡主摄<br><br>【成色】8成新，背板有一道细小划痕，不影响使用<br><br>【配件】原装充电器、手机壳、说明书', NOW() - INTERVAL 30 DAY, NOW()),
(4,  'OPPO Find X7 Ultra 天青蓝 哈苏影像<br><br>【配置】骁龙 8Gen3、16G+256G、双潜望长焦<br><br>【成色】8成新，屏幕无划痕，镜头无磕碰<br><br>【配件】原装充电器、数据线<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 75 DAY, NOW()),
(5,  '三星 Galaxy S24 Ultra 12+256 钛灰<br><br>【配置】骁龙 8Gen3、2 亿像素主摄、S Pen 手写笔<br><br>【成色】95新，全程戴壳贴膜，钛金属边框无磨损<br><br>【配件】原装充电器、S Pen、手机壳', NOW() - INTERVAL 22 DAY, NOW()),
(6,  'vivo X100 Pro 16+512 落日橙 蔡司影像<br><br>【配置】天玑 9300、16G+512G、蔡司 APO 长焦<br><br>【成色】95新，屏幕无划痕，机身无磕碰<br><br>【配件】原装充电器、数据线、手机壳', NOW() - INTERVAL 18 DAY, NOW()),
(7,  'MacBook Air M2 13寸 16+512 深空灰<br><br>【配置】M2 芯片、16G 内存、512G 固态<br><br>【成色】8成新，A 面有轻微使用痕迹，风扇噪音正常<br><br>【电池】循环 78 次，健康度 92%<br><br>【配件】原装充电器、包装盒', NOW() - INTERVAL 70 DAY, NOW()),
(8,  'ThinkPad X1 Carbon Gen11 14寸<br><br>【配置】i7-1365U、16G 内存、512G 固态<br><br>【成色】7成新，键盘字母区磨白明显，屏幕无亮点，转轴正常<br><br>【配件】原装充电器、小红帽<br><br>【适合】商务办公、编程开发', NOW() - INTERVAL 35 DAY, NOW()),
(9,  '联想拯救者 Y9000P 2024 i9 款<br><br>【配置】i9-14900HX、RTX4060、16G+1T、2.5K 240Hz<br><br>【成色】8成新，键盘有使用痕迹，屏幕无坏点，散热正常<br><br>【配件】原装充电器、包装盒', NOW() - INTERVAL 28 DAY, NOW()),
(10, '华硕天选5 Pro 锐龙版 16寸<br><br>【配置】R9-7940HX、RTX4070、16G+1T、2.5K 165Hz<br><br>【成色】8成新，A 面整洁，键盘无掉漆<br><br>【配件】原装充电器', NOW() - INTERVAL 20 DAY, NOW()),
(11, 'MacBook Pro 14寸 M3 Pro 16+512 深空灰<br><br>【配置】M3 Pro 芯片、16G 内存、512G 固态<br><br>【成色】95新，仅使用 3 个月，电池循环 42 次<br><br>【配件】原装充电器、包装盒、说明书<br><br>【渠道】官网购入，全国联保', NOW() - INTERVAL 65 DAY, NOW()),
(12, 'AirPods Pro 2 全新未拆封 原厂塑封<br><br>【型号】AirPods Pro 第二代，带 MagSafe 充电盒<br><br>【成色】全新未拆封，原厂塑封完整未剪<br><br>【保修】未激活，在保<br><br>【提醒】下单可到当面验封', NOW() - INTERVAL 33 DAY, NOW()),
(13, 'Sony WH-1000XM5 头戴降噪耳机 银色<br><br>【成色】8成新，耳罩无脱皮，头梁无断裂，降噪正常<br><br>【续航】约 30 小时，支持快充<br><br>【配件】原装收纳盒、充电线、飞机转接头', NOW() - INTERVAL 26 DAY, NOW()),
(14, 'Bose QC45 头戴消噪耳机 黑色<br><br>【成色】8成新，耳罩皮质有轻微磨损，消噪正常<br><br>【续航】约 24 小时，支持快充<br><br>【配件】原装收纳盒、充电线、飞机转接头', NOW() - INTERVAL 18 DAY, NOW()),
(15, 'JBL Charge 5 蓝牙音箱 黑色<br><br>【型号】IP67 防水便携<br><br>【成色】6成新，顶部按键周围有明显磨损，音质正常<br><br>【续航】约 20 小时，支持 PartyBoost 串联', NOW() - INTERVAL 15 DAY, NOW()),
(16, 'AKG K72 头戴监听耳机<br><br>【成色】8成新，耳罩无脱皮，线材完好<br><br>【说明】卖家临时下架，重新上架后恢复在售<br><br>【配件】原装线材、收纳袋', NOW() - INTERVAL 50 DAY, NOW()),
(17, '小米手环8 NFC版 黑色<br><br>【功能】NFC 门禁、NFC 支付、心率与睡眠监测<br><br>【成色】8成新，腕带有轻微使用痕迹，屏幕无划痕<br><br>【配件】原装充电器、说明书', NOW() - INTERVAL 38 DAY, NOW()),
(18, 'Apple Watch SE 2代 40mm 星光色 GPS 版<br><br>【成色】8成新，表带有使用痕迹，屏幕完好<br><br>【功能】心率监测、运动追踪、消息通知<br><br>【配件】原装磁力充电线、运动表带', NOW() - INTERVAL 21 DAY, NOW()),
(19, '华为 Watch GT4 46mm 棕色皮带<br><br>【成色】95新，表带轻微使用痕迹，屏幕完好<br><br>【功能】心率、血氧、睡眠监测、100+ 运动模式<br><br>【续航】约 14 天', NOW() - INTERVAL 16 DAY, NOW()),
(20, 'Switch OLED 白色 含底座 手柄 64G<br><br>【成色】8成新，屏幕无划痕，底座完好，手柄无漂移<br><br>【配件】主机、底座、一对 Joy-Con、充电线、腕带', NOW() - INTERVAL 44 DAY, NOW()),
(21, 'PS5 光驱版 国行主机 含手柄<br><br>【成色】8成新，主机有轻微灰尘，运行与读碟正常<br><br>【配件】主机、DualSense 手柄、HDMI 线、电源线<br><br>【版本】国行，可备份港服账号', NOW() - INTERVAL 32 DAY, NOW()),
(22, 'Xbox Series X 国行 1TB<br><br>【成色】95新，运行正常，无拆修记录<br><br>【配件】主机、原装手柄、HDMI 线、电源线<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 58 DAY, NOW()),
(23, 'Steam Deck OLED 512G 掌机<br><br>【成色】95新，屏幕无划痕，摇杆手感正常，电池健康<br><br>【配件】原装充电器、原装收纳盒<br><br>【特点】7.4 寸 OLED 屏、SteamOS、可装 Windows', NOW() - INTERVAL 19 DAY, NOW()),
(24, '佳能 EOS R50 微单相机套机 含 15-45mm 镜头<br><br>【成色】95新，快门数不足 5000，屏幕无划痕<br><br>【夜景】APS-C 传感器 + F4.0 光圈，手持夜景出片干净<br><br>【配件】原装电池×2、充电器、相机包', NOW() - INTERVAL 29 DAY, NOW()),
(25, '索尼 A7M3 全画幅微单 机身<br><br>【成色】8成新，底部有轻微使用痕迹，功能全正常<br><br>【夜景】全画幅高感纯净，ISO 6400 可用，夜景扫街利器<br><br>【配件】原装电池×2、64G 存储卡、肩带<br><br>【快门】约 2.1 万，远低于寿命上限', NOW() - INTERVAL 36 DAY, NOW()),
(26, '尼康 Z5 入门全画幅 套机 含 24-50mm<br><br>【成色】95新，屏幕贴膜未撕，快门数约 6000<br><br>【夜景】五轴防抖 + 全画幅，暗光手持不糊<br><br>【配件】原装电池、充电器、原箱', NOW() - INTERVAL 17 DAY, NOW()),
(27, '富士 X-T30 II 复古微单 银黑色<br><br>【成色】95新，快门数约 3000，机身无磕碰<br><br>【夜景】X-Trans 传感器 + 胶片模拟，夜景直出氛围感强<br><br>【配件】原装电池×2、皮质肩带', NOW() - INTERVAL 26 DAY, NOW()),
(28, '佳能 200D II 单反相机 套机 18-55mm<br><br>【成色】8成新，手柄有明显使用痕迹，镜片无霉<br><br>【配件】原装电池、充电器、128G 存储卡<br><br>【说明】已有买家付款，货物在途，平台未放款', NOW() - INTERVAL 12 DAY, NOW()),
(29, 'GoPro Hero12 Black 运动相机 全套<br><br>【成色】95新，机身无磕碰，防水盖完好<br><br>【夜景】超强防抖 + 夜景模式，骑行夜拍不糊<br><br>【配件】原装电池×2、充电线、吸盘支架、防水壳', NOW() - INTERVAL 23 DAY, NOW()),
(30, '索尼 ZV-1 便携数码相机 Vlog 神器<br><br>【成色】8成新，屏幕无划痕，镜头无划痕<br><br>【配件】原装电池、存储卡、相机包<br><br>【状态】已提交审核，等待管理员上架', NOW() - INTERVAL 5 DAY, NOW()),
(31, '高等数学 同济第七版 上下册<br><br>【版本】第七版，同济大学数学系编<br><br>【成色】8成新，书脊完好，习题册有少量铅笔笔记<br><br>【内容】上册函数与极限、微分；下册积分、多元函数<br><br>【适合】大一高数课程、考研数学复习', NOW() - INTERVAL 48 DAY, NOW()),
(32, '线性代数 第五版 同济<br><br>【成色】8成新，封面有轻微折痕，内页干净<br><br>【内容】行列式、矩阵、线性方程组、特征值与二次型<br><br>【适合】工科学生、考研数学一/二', NOW() - INTERVAL 41 DAY, NOW()),
(33, '数据结构 C语言版 严蔚敏<br><br>【成色】8成新，书角轻微磨损，内容完整<br><br>【内容】线性表、栈、队列、树、图、查找与排序<br><br>【适合】计算机专业、考研 408', NOW() - INTERVAL 37 DAY, NOW()),
(34, 'C语言程序设计 第五版 谭浩强<br><br>【成色】6成新，正文有较多笔记与划线，课后习题做过一部分<br><br>【内容】数据类型、指针、结构体、文件操作<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 55 DAY, NOW()),
(35, '大学物理 上下册 第四版 张三慧<br><br>【成色】6成新，有较多笔记，部分页面折角，不影响阅读<br><br>【内容】力学、热学、电磁学、光学、量子物理<br><br>【适合】理工科大学物理课程', NOW() - INTERVAL 33 DAY, NOW()),
(36, '张宇考研数学基础30讲 2025版<br><br>【成色】8成新，仅前几讲有零星笔记<br><br>【内容】高数 + 线代 + 概率论基础知识点全覆盖<br><br>【适合】考研数学基础阶段', NOW() - INTERVAL 27 DAY, NOW()),
(37, '肖秀荣考研政治全套 2025版<br><br>【成色】8成新，精讲精练有笔记，1000 题未做<br><br>【内容】精讲精练、1000 题、讲真题、形势与政策<br><br>【适合】考研政治全程复习', NOW() - INTERVAL 22 DAY, NOW()),
(38, '考研英语词汇红宝书 2025版<br><br>【成色】6成新，正文有较多笔记与划线，词表完整<br><br>【内容】5500+ 核心词汇、真题例句、记忆方法<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 52 DAY, NOW()),
(39, '汤家凤考研数学1800题<br><br>【成色】6成新，基础篇有大量手写笔记，提高篇未做<br><br>【内容】基础篇 + 提高篇全覆盖<br><br>【适合】考研数学强化阶段', NOW() - INTERVAL 14 DAY, NOW()),
(40, '三体 全三册 精装典藏版 刘慈欣<br><br>【成色】95新，书盒与三册齐全，几乎无翻阅痕迹<br><br>【内容】地球往事、黑暗森林、死神永生<br><br>【推荐】雨果奖作品，中文科幻经典', NOW() - INTERVAL 25 DAY, NOW()),
(41, '人类简史 从动物到上帝 精装版<br><br>【成色】95新，无折痕无划线，书脊完好<br><br>【作者】尤瓦尔·赫拉利<br><br>【推荐】从认知革命到科学革命，重新审视人类历史', NOW() - INTERVAL 13 DAY, NOW()),
(42, '深入理解计算机系统 CSAPP 第三版<br><br>【成色】8成新，扉页有少量笔记，其余干净<br><br>【内容】数据表示、汇编、存储器层次、链接、并发<br><br>【适合】计算机专业进阶、系统编程', NOW() - INTERVAL 20 DAY, NOW()),
(43, '活着 余华<br><br>【成色】95新，无笔记无划线，塑封已拆<br><br>【内容】福贵的一生，讲的是活着本身的意义<br><br>【适合】文学阅读、课余放松', NOW() - INTERVAL 9 DAY, NOW()),
(44, 'Nike Air Jordan 1 High OG 黑白 42码<br><br>【尺码】42码（US 8.5）<br><br>【成色】8成新，鞋面干净，鞋底有正常磨损<br><br>【来源】得物购入，正品', NOW() - INTERVAL 31 DAY, NOW()),
(45, 'New Balance 990v6 元祖灰 38码 美产<br><br>【尺码】38码（US 6.5）<br><br>【成色】8成新，鞋底磨损正常，鞋面无污渍<br><br>【特点】猪巴革 + 网面，ENCAP 中底', NOW() - INTERVAL 24 DAY, NOW()),
(46, 'Converse 1970s 黑色高帮 43码<br><br>【尺码】43码（US 9.5）<br><br>【成色】8成新，鞋头有轻微磨损，鞋带为原厂<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 42 DAY, NOW()),
(47, '北面冲锋衣 黑色 M码 防水透气<br><br>【尺码】M 码，适合身高 170-175cm<br><br>【成色】8成新，拉链顺畅，无破损，防水面料完好<br><br>【功能】可调节帽、多口袋设计', NOW() - INTERVAL 28 DAY, NOW()),
(48, 'Patagonia 抓绒衣 蓝色 M码<br><br>【尺码】M 码，适合身高 170-175cm<br><br>【成色】8成新，无起球，领口无变形<br><br>【特点】再生聚酯纤维，全拉链设计', NOW() - INTERVAL 15 DAY, NOW()),
(49, '优衣库轻型羽绒服 黑色 L码<br><br>【尺码】L 码，适合身高 175-180cm<br><br>【成色】8成新，有轻微压痕，保暖性良好<br><br>【特点】可收纳便携、90% 白鸭绒填充', NOW() - INTERVAL 10 DAY, NOW()),
(50, 'Fjallraven Kanken 经典双肩包 森林绿<br><br>【容量】16L，Vinylon 防水面料<br><br>【成色】95新，使用不到 3 个月，肩带无磨损<br><br>【适合】上学、通勤、短途出行', NOW() - INTERVAL 26 DAY, NOW()),
(51, 'Nike 运动双肩包 黑色 30L<br><br>【容量】30L，可放 15.6 寸笔记本<br><br>【成色】8成新，拉链正常，底部干净<br><br>【功能】电脑隔层、透气背垫、多口袋', NOW() - INTERVAL 21 DAY, NOW()),
(52, '小米台灯Pro 护眼阅读灯<br><br>【照度】国 AA 级，无频闪，蓝光防护<br><br>【成色】95新，使用约 3 个月，灯臂无变形<br><br>【功能】智能调光、定时关灯、米家 APP 控制', NOW() - INTERVAL 39 DAY, NOW()),
(53, '懒人加湿器 4.5L 静音款<br><br>【容量】4.5L，持续加湿约 12 小时<br><br>【成色】95新，使用不到 1 个月，噪音低于 35dB<br><br>【功能】智能恒湿、定时关机、过夜保护<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 52 DAY, NOW()),
(54, '得力碎纸机 家用办公 4级保密<br><br>【型号】得力 9922，4 级保密<br><br>【成色】8成新，运行正常，刀片锋利，可碎信用卡<br><br>【适合】生活办公、隐私文件销毁', NOW() - INTERVAL 11 DAY, NOW()),
(55, '小米空气净化器4 Lite 卧室款<br><br>【适用面积】20-40㎡ 卧室 / 书房<br><br>【成色】95新，滤芯使用约 2 个月<br><br>【功能】HEPA 滤芯、PM2.5 实时显示、米家 APP 控制', NOW() - INTERVAL 18 DAY, NOW()),
(56, 'Anker 65W 氮化镓充电器 三口<br><br>【规格】2C1A 三口输出，最大 65W<br><br>【成色】95新，插脚无氧化<br><br>【兼容】MacBook / iPad / 手机 / Switch 全兼容', NOW() - INTERVAL 13 DAY, NOW()),
(57, '罗技 MX Master 3S 无线鼠标 深灰<br><br>【连接】蓝牙 + 2.4G 双模，支持 3 设备切换<br><br>【成色】95新，滚轮无油光，按键无松动<br><br>【续航】约 70 天，Type-C 快充', NOW() - INTERVAL 17 DAY, NOW()),
(58, 'Apple Magic Keyboard 妙控键盘 带触控ID<br><br>【型号】带触控ID 和数字小键盘版<br><br>【成色】全新未使用，包装完整<br><br>【说明】主图非实物拍摄，审核被打回，补图后可重新提交', NOW() - INTERVAL 7 DAY, NOW()),
(59, '绿联 Type-C 扩展坞 7合1 银色<br><br>【接口】HDMI 4K + 3×USB3.0 + SD/TF + PD100W<br><br>【成色】8成新，接口完好无松动<br><br>【兼容】MacBook / 笔记本 / 平板通用', NOW() - INTERVAL 6 DAY, NOW()),
(60, '可调节哑铃 20kg 单只 快调式<br><br>【重量】2.5-20kg 可调，15 档快调<br><br>【成色】6成新，手柄握把有明显油汗痕迹，调节机构顺畅<br><br>【适合】生活健身、家庭训练', NOW() - INTERVAL 16 DAY, NOW()),
(61, '健身瑜伽垫 TPE 加厚防滑<br><br>【尺寸】183×80×10mm 加厚加宽<br><br>【成色】8成新，表面防滑纹路完好，无异味<br><br>【配件】收纳绑带', NOW() - INTERVAL 8 DAY, NOW()),
(62, '迪卡侬山地自行车 Rockrider ST520 27速<br><br>【配置】铝合金车架、27 速禧玛诺变速、前后碟刹<br><br>【成色】8成新，轮胎磨损正常，变速调试准确<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 47 DAY, NOW()),
(63, '捷安特 ATX860 山地车 27.5寸 蓝白<br><br>【配置】铝合金车架、24 速变速、液压碟刹<br><br>【成色】8成新，轮胎磨损正常，漆面无划伤<br><br>【说明】已有买家付款，货物在途，平台未放款', NOW() - INTERVAL 34 DAY, NOW()),
(64, '尤尼克斯羽毛球拍 ARC-7 进攻型<br><br>【规格】Arcsaber 7 全碳素，4UG5<br><br>【成色】8成新，拍框有细微磕碰，线已断需重新穿线<br><br>【适合】中高级球友，进攻打法', NOW() - INTERVAL 12 DAY, NOW()),
(65, '原神 60级 全图鉴账号 官服<br><br>【等级】冒险等阶 60 级<br><br>【内容】全图鉴含限定角色，多个满命角色<br><br>【安全】可改绑手机号，平台担保交易', NOW() - INTERVAL 10 DAY, NOW()),
(66, '王者荣耀 V10 贵族号 100+皮肤<br><br>【等级】V10 贵族，全英雄解锁<br><br>【皮肤】100+ 含限定、传说、史诗<br><br>【安全】可改绑手机号，平台担保交易', NOW() - INTERVAL 7 DAY, NOW()),
(67, '网易云音乐年卡VIP 黑胶<br><br>【类型】黑胶 VIP 年卡，官方直充<br><br>【权益】无损音质、免广告、专属皮肤<br><br>【说明】此商品已售出，仅供展示', NOW() - INTERVAL 18 DAY, NOW()),
(68, '哔哩哔哩大会员 剩余10个月<br><br>【权益】番剧、纪录片、电影免广告观看<br><br>【剩余】10 个月，成交后协助换绑<br><br>【交易】虚拟商品，线上交易', NOW() - INTERVAL 4 DAY, NOW()),
(69, 'Figma 设计系统 UI 组件包 500+ 矢量<br><br>【内容】500+ 组件与图标：按钮、表单、导航、数据可视化<br><br>【格式】.fig 源文件，可一键复制进自己的项目<br><br>【适合】UI 设计师与独立开发者快速搭原型', NOW() - INTERVAL 3 DAY, NOW()),
(70, 'Notion 个人版会员 剩余8个月<br><br>【权益】无限页面、无限块、访客协作<br><br>【剩余】8 个月，成交后转移时长到你的账号<br><br>【状态】草稿中，完善描述后即可提交上架', NOW() - INTERVAL 2 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `description` = new.`description`,
    `update_time` = NOW();

-- ===================================================================
-- 4. 商品图片（每件 1 张主图，is_main=1 唯一；8 件多图商品补 2-3 张供轮播演示）
--     素材为 Unsplash 外链（演示用，URL 沿用已探活素材池）
-- ===================================================================

INSERT INTO `eo_product_image` (
    `id`, `product_id`, `image_url`, `sort_order`, `is_main`, `create_time`, `update_time`
) VALUES
(1,  1,  'https://images.unsplash.com/photo-1678685888221-cda773a3acdb?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 45 DAY, NOW()),
(2,  1,  'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 45 DAY, NOW()),
(3,  1,  'https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=800&auto=format&fit=crop', 2, 0, NOW() - INTERVAL 45 DAY, NOW()),
(4,  2,  'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 40 DAY, NOW()),
(5,  3,  'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 30 DAY, NOW()),
(6,  4,  'https://images.unsplash.com/photo-1574944985070-8f3ebc6b79d2?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 75 DAY, NOW()),
(7,  5,  'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 22 DAY, NOW()),
(8,  6,  'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 18 DAY, NOW()),
(9,  7,  'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 70 DAY, NOW()),
(10, 7,  'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 70 DAY, NOW()),
(11, 7,  'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&auto=format&fit=crop', 2, 0, NOW() - INTERVAL 70 DAY, NOW()),
(12, 8,  'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 35 DAY, NOW()),
(13, 9,  'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 28 DAY, NOW()),
(14, 9,  'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 28 DAY, NOW()),
(15, 10, 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 20 DAY, NOW()),
(16, 11, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 65 DAY, NOW()),
(17, 11, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 65 DAY, NOW()),
(18, 12, 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 33 DAY, NOW()),
(19, 12, 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 33 DAY, NOW()),
(20, 13, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 26 DAY, NOW()),
(21, 14, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 24 DAY, NOW()),
(22, 15, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 15 DAY, NOW()),
(23, 16, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 50 DAY, NOW()),
(24, 17, 'https://images.unsplash.com/photo-1575311373937-040b8e1fd5b6?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 38 DAY, NOW()),
(25, 18, 'https://images.unsplash.com/photo-1546868871-af0de0ae72be?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 21 DAY, NOW()),
(26, 19, 'https://images.unsplash.com/photo-1546868871-af0de0ae72be?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 16 DAY, NOW()),
(27, 20, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 44 DAY, NOW()),
(28, 20, 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 44 DAY, NOW()),
(29, 21, 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 32 DAY, NOW()),
(30, 22, 'https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 58 DAY, NOW()),
(31, 23, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 19 DAY, NOW()),
(32, 24, 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 29 DAY, NOW()),
(33, 24, 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 29 DAY, NOW()),
(34, 25, 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 36 DAY, NOW()),
(35, 25, 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 36 DAY, NOW()),
(36, 26, 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 17 DAY, NOW()),
(37, 27, 'https://images.unsplash.com/photo-1495707902641-75cac588d2e9?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 26 DAY, NOW()),
(38, 28, 'https://images.unsplash.com/photo-1452780212940-6f5c0d14d848?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 12 DAY, NOW()),
(39, 29, 'https://images.unsplash.com/photo-1564466809058-bf4114d55352?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 23 DAY, NOW()),
(40, 30, 'https://images.unsplash.com/photo-1519638831568-d9897f54ed69?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 5 DAY, NOW()),
(41, 31, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 48 DAY, NOW()),
(42, 32, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 41 DAY, NOW()),
(43, 33, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 37 DAY, NOW()),
(44, 34, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 55 DAY, NOW()),
(45, 35, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 33 DAY, NOW()),
(46, 36, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 27 DAY, NOW()),
(47, 37, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 22 DAY, NOW()),
(48, 38, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 60 DAY, NOW()),
(49, 39, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 14 DAY, NOW()),
(50, 40, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 25 DAY, NOW()),
(51, 41, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 13 DAY, NOW()),
(52, 42, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 20 DAY, NOW()),
(53, 43, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 9 DAY, NOW()),
(54, 44, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 31 DAY, NOW()),
(55, 45, 'https://images.unsplash.com/photo-1539185441755-769473a23570?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 24 DAY, NOW()),
(56, 45, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 24 DAY, NOW()),
(57, 46, 'https://images.unsplash.com/photo-1607522370275-f14206abe5d3?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 42 DAY, NOW()),
(58, 47, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 28 DAY, NOW()),
(59, 48, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 15 DAY, NOW()),
(60, 49, 'https://images.unsplash.com/photo-1544923246-77307dd270b2?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 10 DAY, NOW()),
(61, 50, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 26 DAY, NOW()),
(62, 51, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 21 DAY, NOW()),
(63, 52, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 39 DAY, NOW()),
(64, 52, 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 39 DAY, NOW()),
(65, 53, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 52 DAY, NOW()),
(66, 54, 'https://images.unsplash.com/photo-1586953208448-b95a79798f07?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 11 DAY, NOW()),
(67, 55, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 18 DAY, NOW()),
(68, 56, 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 13 DAY, NOW()),
(69, 57, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 17 DAY, NOW()),
(70, 58, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 7 DAY, NOW()),
(71, 59, 'https://images.unsplash.com/photo-1625842268584-8f3296236761?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 6 DAY, NOW()),
(72, 60, 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 16 DAY, NOW()),
(73, 61, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 8 DAY, NOW()),
(74, 62, 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 47 DAY, NOW()),
(75, 62, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&auto=format&fit=crop', 1, 0, NOW() - INTERVAL 47 DAY, NOW()),
(76, 63, 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 34 DAY, NOW()),
(77, 64, 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 12 DAY, NOW()),
(78, 65, 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 10 DAY, NOW()),
(79, 66, 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 7 DAY, NOW()),
(80, 67, 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 18 DAY, NOW()),
(81, 68, 'https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 4 DAY, NOW()),
(82, 69, 'https://images.unsplash.com/photo-1561070791-2526d30994b5?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 3 DAY, NOW()),
(83, 70, 'https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=800&auto=format&fit=crop', 0, 1, NOW() - INTERVAL 2 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url`  = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main`    = new.`is_main`,
    `update_time` = NOW();

-- ===================================================================
-- 5. 商品审核记录（管理端审核历史演示；30 仍在待审队列故无记录）
-- ===================================================================

INSERT INTO `eo_product_audit_log` (
    `id`, `product_id`, `operator_id`, `operator_name`, `action`, `reason`,
    `before_status`, `after_status`, `create_time`, `update_time`
) VALUES
('PAL1', '24', '2', '平台管理员', 1, '信息完整，实拍图清晰，成色与描述一致', 'PENDING_REVIEW', 'ONLINE', NOW() - INTERVAL 28 DAY, NOW()),
('PAL2', '25', '2', '平台管理员', 1, '快门数与描述吻合，通过',              'PENDING_REVIEW', 'ONLINE', NOW() - INTERVAL 35 DAY, NOW()),
('PAL3', '62', '2', '平台管理员', 1, '配件齐全，描述如实，通过',            'PENDING_REVIEW', 'ONLINE', NOW() - INTERVAL 46 DAY, NOW()),
('PAL4', '12', '2', '平台管理员', 1, '原厂塑封未拆，通过',                  'PENDING_REVIEW', 'ONLINE', NOW() - INTERVAL 32 DAY, NOW()),
('PAL5', '58', '2', '平台管理员', 2, '主图非实物拍摄，不符合发布规范第 3 条；请补实拍图后重新提交', 'PENDING_REVIEW', 'REJECTED', NOW() - INTERVAL 6 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `action`        = new.`action`,
    `reason`        = new.`reason`,
    `before_status` = new.`before_status`,
    `after_status`  = new.`after_status`,
    `update_time`   = NOW();

-- ===================================================================
-- 6. 订单（20 笔，覆盖六种状态；状态口径见文件头 ①②③）
--     order_no / payment_no 的日期段由 create_time 表达式生成，重放时不会与订单创建日期脱节
-- ===================================================================

INSERT INTO `eo_order` (
    `id`, `order_no`, `buyer_id`, `seller_id`, `total_amount`, `status`, `payment_status`,
    `address`, `phone`, `remark`, `cancel_reason`, `cancel_time`, `refund_reason`, `refund_time`,
    `create_time`, `update_time`
) VALUES
(1,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 58 DAY, '%Y%m%d'), '001'), '5',  '4',  3299.00,  'COMPLETED',       'PAID',     '西校区1号楼205',  '13800138005', '周末图书馆自取', NULL, NULL, NULL, NULL, NOW() - INTERVAL 58 DAY, NOW() - INTERVAL 56 DAY),
(2,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 55 DAY, '%Y%m%d'), '002'), '3',  '1',  6499.00,  'COMPLETED',       'PAID',     '东校区3号楼302',  '13800138003', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 53 DAY),
(3,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 50 DAY, '%Y%m%d'), '003'), '8',  '1',  11999.00, 'COMPLETED',       'PAID',     '北校区2号楼410',  '13800138008', '周末图书馆面交', NULL, NULL, NULL, NULL, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 48 DAY),
(4,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 45 DAY, '%Y%m%d'), '004'), '10', '9',  2999.00,  'COMPLETED',       'PAID',     '南校区7号楼518',  '13800138010', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 43 DAY),
(5,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 40 DAY, '%Y%m%d'), '005'), '8',  '4',  25.00,    'COMPLETED',       'PAID',     '北校区2号楼410',  '13800138008', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 39 DAY),
(6,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 35 DAY, '%Y%m%d'), '006'), '12', '6',  35.00,    'COMPLETED',       'PAID',     '南校区3号楼608',  '13800138012', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 34 DAY),
(7,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 30 DAY, '%Y%m%d'), '007'), '3',  '7',  259.00,   'COMPLETED',       'PAID',     '东校区3号楼302',  '13800138003', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 29 DAY),
(8,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 25 DAY, '%Y%m%d'), '008'), '11', '1',  899.00,   'COMPLETED',       'PAID',     '西校区5号楼201',  '13800138011', '约周末操场面交', NULL, NULL, NULL, NULL, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 24 DAY),
(9,  CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 20 DAY, '%Y%m%d'), '009'), '4',  '6',  89.00,    'COMPLETED',       'PAID',     '南校区7号楼518',  '13800138004', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 19 DAY),
(10, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 15 DAY, '%Y%m%d'), '010'), '13', '6',  88.00,    'COMPLETED',       'PAID',     '东校区1号楼102',  '13800138013', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 14 DAY),
(11, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 3 DAY,  '%Y%m%d'), '011'), '14', '1',  299.00,   'PAID',            'PAID',     '南校区6号楼507',  '13800138014', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
(12, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 2 DAY,  '%Y%m%d'), '012'), '3',  '9',  3299.00,  'PAID',            'PAID',     '东校区3号楼302',  '13800138003', '尽快发货',       NULL, NULL, NULL, NULL, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR),
(13, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 6 DAY,  '%Y%m%d'), '013'), '4',  '7',  2899.00,  'SHIPPED',         'PAID',     '南校区7号楼518',  '13800138004', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 4 DAY),
(14, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 5 DAY,  '%Y%m%d'), '014'), '8',  '10', 1299.00,  'SHIPPED',         'PAID',     '北校区2号楼410',  '13800138008', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 3 DAY),
(15, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 1 DAY,  '%Y%m%d'), '015'), '6',  '1',  1299.00,  'PENDING_PAYMENT', 'UNPAID',   '南校区9号楼303',  '13800138006', '',              NULL, NULL, NULL, NULL, NOW() - INTERVAL 1 DAY,  NOW()),
(16, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 1 DAY,  '%Y%m%d'), '016'), '10', '13', 899.00,   'PENDING_PAYMENT', 'UNPAID',   '南校区7号楼518',  '13800138010', '可以同城面交吗', NULL, NULL, NULL, NULL, NOW() - INTERVAL 1 DAY,  NOW()),
(17, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 8 DAY,  '%Y%m%d'), '017'), '7',  '11', 3299.00,  'CANCELLED',       'UNPAID',   '北校区8号楼303',  '13800138007', '',              '卖家缺货，双方协商取消', NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR, NULL, NULL, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR),
(18, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 4 DAY,  '%Y%m%d'), '018'), '13', '8',  89.00,    'CANCELLED',       'UNPAID',   '东校区1号楼102',  '13800138013', '',              '买家临时不想要了',        NOW() - INTERVAL 4 DAY + INTERVAL 3 HOUR, NULL, NULL, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY + INTERVAL 3 HOUR),
(19, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 12 DAY, '%Y%m%d'), '019'), '4',  '3',  5899.00,  'REFUNDED',        'REFUNDED', '南校区7号楼518',  '13800138004', '',              NULL, NULL, '成色与描述不符，验收时发现背面明显划痕', NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 11 DAY),
(20, CONCAT('ORD', DATE_FORMAT(NOW() - INTERVAL 10 DAY, '%Y%m%d'), '020'), '6',  '12', 599.00,   'REFUNDED',        'REFUNDED', '南校区9号楼303',  '13800138006', '',              NULL, NULL, '背包尺寸不合适，买家反悔',            NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 9 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `buyer_id`       = new.`buyer_id`,
    `seller_id`      = new.`seller_id`,
    `total_amount`   = new.`total_amount`,
    `status`         = new.`status`,
    `payment_status` = new.`payment_status`,
    `address`        = new.`address`,
    `phone`          = new.`phone`,
    `remark`         = new.`remark`,
    `cancel_reason`  = new.`cancel_reason`,
    `cancel_time`    = new.`cancel_time`,
    `refund_reason`  = new.`refund_reason`,
    `refund_time`    = new.`refund_time`,
    `update_time`    = NOW();

-- ===================================================================
-- 7. 订单行项（1:1；快照 JSON 与写路径同形，在此直接由商品行算出）
--     为什么用 INSERT...SELECT 而不是 VALUES：VALUES 里不能引用同行其它列，
--     快照需要商品名称/主图/描述/成色四项，只能从 eo_product 关联取。
--     ODKU 不覆盖 product_snapshot：快照语义是「下单那一刻的」，
--     重放时商品改名改价都不该回写历史订单（与写路径的留痕行为一致）。
-- ===================================================================

INSERT INTO `eo_order_item` (
    `id`, `order_id`, `product_id`, `product_snapshot`, `unit_price`, `quantity`, `subtotal`,
    `create_time`, `update_time`, `del_flag`, `version`
)
SELECT
    CONCAT('OI', m.oid),
    m.oid,
    m.pid,
    JSON_OBJECT(
        'productId',      p.`id`,
        'name',           p.`name`,
        'image',          COALESCE((SELECT i.`image_url` FROM `eo_product_image` i
                                    WHERE i.`product_id` = p.`id`
                                    ORDER BY i.`is_main` DESC, i.`sort_order` ASC LIMIT 1), ''),
        'description',    COALESCE((SELECT d.`description` FROM `eo_product_detail` d
                                    WHERE d.`product_id` = p.`id` LIMIT 1), ''),
        'price',          p.`price`,
        'conditionLevel', CASE p.`condition_level`
                              WHEN '1' THEN '全新'
                              WHEN '2' THEN '几乎全新'
                              WHEN '3' THEN '轻微使用痕迹'
                              ELSE '明显使用痕迹' END
    ),
    p.`price`,
    1,
    p.`price`,
    o.`create_time`,
    o.`update_time`,
    0,
    0
FROM (
        SELECT '1'  AS oid, '4'  AS pid UNION ALL
        SELECT '2',  '7'  UNION ALL
        SELECT '3',  '11' UNION ALL
        SELECT '4',  '22' UNION ALL
        SELECT '5',  '34' UNION ALL
        SELECT '6',  '38' UNION ALL
        SELECT '7',  '46' UNION ALL
        SELECT '8',  '62' UNION ALL
        SELECT '9',  '53' UNION ALL
        SELECT '10', '67' UNION ALL
        SELECT '11', '17' UNION ALL
        SELECT '12', '28' UNION ALL
        SELECT '13', '21' UNION ALL
        SELECT '14', '63' UNION ALL
        SELECT '15', '12' UNION ALL
        SELECT '16', '19' UNION ALL
        SELECT '17', '23' UNION ALL
        SELECT '18', '59' UNION ALL
        SELECT '19', '27' UNION ALL
        SELECT '20', '50'
     ) m
JOIN `eo_order`   o ON o.`id` = m.oid
JOIN `eo_product` p ON p.`id` = m.pid
ON DUPLICATE KEY UPDATE `eo_order_item`.`id` = `eo_order_item`.`id`;

-- ===================================================================
-- 8. 支付单（PENDING 两单与未付款取消的两单除外：17/18 从未支付，不存在支付单）
-- ===================================================================

INSERT INTO `eo_payment` (
    `id`, `payment_no`, `order_id`, `user_id`, `amount`, `refunded_amount`,
    `payment_method`, `status`, `transaction_id`, `refund_reason`, `refund_time`,
    `create_time`, `update_time`
) VALUES
('PMT01', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 58 DAY, '%Y%m%d'), '01'), '1',  '5',  3299.00,  0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 58 DAY, '%Y%m%d'), '01'), NULL, NULL, NOW() - INTERVAL 58 DAY, NOW() - INTERVAL 58 DAY + INTERVAL 2 HOUR),
('PMT02', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 55 DAY, '%Y%m%d'), '02'), '2',  '3',  6499.00,  0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 55 DAY, '%Y%m%d'), '02'), NULL, NULL, NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 55 DAY + INTERVAL 2 HOUR),
('PMT03', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 50 DAY, '%Y%m%d'), '03'), '3',  '8',  11999.00, 0.00,    'ALIPAY', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 50 DAY, '%Y%m%d'), '03'), NULL, NULL, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 50 DAY + INTERVAL 2 HOUR),
('PMT04', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 45 DAY, '%Y%m%d'), '04'), '4',  '10', 2999.00,  0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 45 DAY, '%Y%m%d'), '04'), NULL, NULL, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 45 DAY + INTERVAL 2 HOUR),
('PMT05', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 40 DAY, '%Y%m%d'), '05'), '5',  '8',  25.00,    0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 40 DAY, '%Y%m%d'), '05'), NULL, NULL, NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY + INTERVAL 2 HOUR),
('PMT06', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 35 DAY, '%Y%m%d'), '06'), '6',  '12', 35.00,    0.00,    'ALIPAY', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 35 DAY, '%Y%m%d'), '06'), NULL, NULL, NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 35 DAY + INTERVAL 2 HOUR),
('PMT07', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 30 DAY, '%Y%m%d'), '07'), '7',  '3',  259.00,   0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 30 DAY, '%Y%m%d'), '07'), NULL, NULL, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY + INTERVAL 2 HOUR),
('PMT08', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 25 DAY, '%Y%m%d'), '08'), '8',  '11', 899.00,   0.00,    'ALIPAY', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 25 DAY, '%Y%m%d'), '08'), NULL, NULL, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY + INTERVAL 2 HOUR),
('PMT09', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 20 DAY, '%Y%m%d'), '09'), '9',  '4',  89.00,    0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 20 DAY, '%Y%m%d'), '09'), NULL, NULL, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY + INTERVAL 2 HOUR),
('PMT10', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 15 DAY, '%Y%m%d'), '10'), '10', '13', 88.00,    0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 15 DAY, '%Y%m%d'), '10'), NULL, NULL, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY + INTERVAL 2 HOUR),
('PMT11', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 3 DAY,  '%Y%m%d'), '11'), '11', '14', 299.00,   0.00,    'ALIPAY', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 3 DAY,  '%Y%m%d'), '11'), NULL, NULL, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR),
('PMT12', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 2 DAY,  '%Y%m%d'), '12'), '12', '3',  3299.00,  0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 2 DAY,  '%Y%m%d'), '12'), NULL, NULL, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR),
('PMT13', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 6 DAY,  '%Y%m%d'), '13'), '13', '4',  2899.00,  0.00,    'WECHAT', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 6 DAY,  '%Y%m%d'), '13'), NULL, NULL, NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY + INTERVAL 5 HOUR),
('PMT14', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 5 DAY,  '%Y%m%d'), '14'), '14', '8',  1299.00,  0.00,    'ALIPAY', 'SUCCESS',  CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 5 DAY,  '%Y%m%d'), '14'), NULL, NULL, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR),
('PMT15', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 1 DAY,  '%Y%m%d'), '15'), '15', '6',  1299.00,  0.00,    NULL,     'PENDING', NULL,                                                                 NULL, NULL, NOW() - INTERVAL 1 DAY,  NOW()),
('PMT16', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 1 DAY,  '%Y%m%d'), '16'), '16', '10', 899.00,   0.00,    NULL,     'PENDING', NULL,                                                                 NULL, NULL, NOW() - INTERVAL 1 DAY,  NOW()),
('PMT19', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 12 DAY, '%Y%m%d'), '19'), '19', '4',  5899.00,  5899.00, 'WECHAT', 'REFUNDED', CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 12 DAY, '%Y%m%d'), '19'), '成色与描述不符，验收时发现背面明显划痕', NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 11 DAY),
('PMT20', CONCAT('PAY', DATE_FORMAT(NOW() - INTERVAL 10 DAY, '%Y%m%d'), '20'), '20', '6',  599.00,   599.00,  'ALIPAY', 'REFUNDED', CONCAT('TXN', DATE_FORMAT(NOW() - INTERVAL 10 DAY, '%Y%m%d'), '20'), '背包尺寸不合适，买家反悔',             NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 9 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `user_id`         = new.`user_id`,
    `amount`          = new.`amount`,
    `refunded_amount` = new.`refunded_amount`,
    `payment_method`  = new.`payment_method`,
    `status`          = new.`status`,
    `refund_reason`   = new.`refund_reason`,
    `refund_time`     = new.`refund_time`,
    `update_time`     = NOW();

-- ===================================================================
-- 9. 库存流水（与订单状态结构化耦合，不手写数值——见文件头约束 ④）
--     INIT：每件商品一条基线（初始库存 1，biz_id 留空脱离幂等约束）
--     DECREASE：每笔订单行一条（建单即扣，含待付款单）
--     RESTORE：取消 / 退款的订单行各一条恢复
--     ODKU 为 no-op：流水是 append-only 事实，重放不重写历史行
-- ===================================================================

INSERT INTO `eo_stock_ledger` (
    `id`, `product_id`, `biz_id`, `change_type`, `delta`, `stock_after`, `create_time`, `update_time`
)
SELECT CONCAT('LGI', LPAD(p.`id`, 4, '0')), p.`id`, NULL, 'INIT', 1, 1, p.`create_time`, p.`create_time`
FROM `eo_product` p
ON DUPLICATE KEY UPDATE `eo_stock_ledger`.`id` = `eo_stock_ledger`.`id`;

INSERT INTO `eo_stock_ledger` (
    `id`, `product_id`, `biz_id`, `change_type`, `delta`, `stock_after`, `create_time`, `update_time`
)
SELECT CONCAT('LGD', LPAD(oi.`order_id`, 4, '0')), oi.`product_id`, oi.`order_id`, 'DECREASE', -oi.`quantity`, 0, o.`create_time`, o.`create_time`
FROM `eo_order_item` oi
JOIN `eo_order` o ON o.`id` = oi.`order_id`
ON DUPLICATE KEY UPDATE `eo_stock_ledger`.`id` = `eo_stock_ledger`.`id`;

INSERT INTO `eo_stock_ledger` (
    `id`, `product_id`, `biz_id`, `change_type`, `delta`, `stock_after`, `create_time`, `update_time`
)
SELECT CONCAT('LGR', LPAD(oi.`order_id`, 4, '0')), oi.`product_id`, oi.`order_id`, 'RESTORE', oi.`quantity`, 1,
       COALESCE(o.`cancel_time`, o.`refund_time`), COALESCE(o.`cancel_time`, o.`refund_time`)
FROM `eo_order_item` oi
JOIN `eo_order` o ON o.`id` = oi.`order_id`
WHERE o.`status` IN ('CANCELLED', 'REFUNDED')
ON DUPLICATE KEY UPDATE `eo_stock_ledger`.`id` = `eo_stock_ledger`.`id`;

-- ===================================================================
-- 10. 消息（46 条：欢迎/上架提醒/订单链/私聊；conversation_id 一律 NULL，见约束 ⑤）
--      type：1 系统 2 聊天 3 订单 4 支付；business_id 指向商品或订单
-- ===================================================================

INSERT INTO `eo_message` (
    `id`, `sender_id`, `receiver_id`, `type`, `title`, `content`,
    `is_read`, `read_time`, `business_id`, `create_time`, `update_time`
) VALUES
-- 欢迎消息
('MSG01', NULL, '1',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 119 DAY, NULL, NOW() - INTERVAL 120 DAY, NOW()),
('MSG02', NULL, '3',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 99 DAY,  NULL, NOW() - INTERVAL 100 DAY, NOW()),
('MSG03', NULL, '4',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 79 DAY,  NULL, NOW() - INTERVAL 80 DAY,  NOW()),
('MSG04', NULL, '5',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 59 DAY,  NULL, NOW() - INTERVAL 60 DAY,  NOW()),
('MSG05', NULL, '6',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 54 DAY,  NULL, NOW() - INTERVAL 55 DAY,  NOW()),
('MSG06', NULL, '7',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 49 DAY,  NULL, NOW() - INTERVAL 50 DAY,  NOW()),
('MSG07', NULL, '8',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 44 DAY,  NULL, NOW() - INTERVAL 45 DAY,  NOW()),
('MSG08', NULL, '9',  1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 94 DAY,  NULL, NOW() - INTERVAL 95 DAY,  NOW()),
('MSG09', NULL, '10', 1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 39 DAY,  NULL, NOW() - INTERVAL 40 DAY,  NOW()),
('MSG10', NULL, '11', 1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 34 DAY,  NULL, NOW() - INTERVAL 35 DAY,  NOW()),
('MSG11', NULL, '12', 1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 29 DAY,  NULL, NOW() - INTERVAL 30 DAY,  NOW()),
('MSG12', NULL, '13', 1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 24 DAY,  NULL, NOW() - INTERVAL 25 DAY,  NOW()),
('MSG13', NULL, '14', 1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！在这里你可以发布闲置资产，AI 工程化能力帮你定价、写描述，快去发布你的第一件资产吧~', 1, NOW() - INTERVAL 19 DAY,  NULL, NOW() - INTERVAL 20 DAY,  NOW()),
('MSG14', NULL, '15', 1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！你的账号因连续登录失败已被临时锁定，如非本人操作请联系客服。', 1, NOW() - INTERVAL 14 DAY, NULL, NOW() - INTERVAL 15 DAY, NOW()),
('MSG15', NULL, '16', 1, '欢迎加入 EasyOrange', '欢迎来到 EasyOrange！你的账号因发布违规商品已被限制交易，完成整改后可申请恢复。', 1, NOW() - INTERVAL 11 DAY, NULL, NOW() - INTERVAL 12 DAY, NOW()),
-- 商品上架提醒
('MSG16', NULL, '1',  1, '商品上架提醒', '你发布的商品「iPhone 14 Pro Max 256G 暗紫色」已成功上架，祝早日售出！', 1, NOW() - INTERVAL 45 DAY, '1', NOW() - INTERVAL 45 DAY, NOW()),
('MSG17', NULL, '9',  1, '商品上架提醒', '你发布的商品「Xbox Series X 国行 1TB」已成功上架，祝早日售出！', 1, NOW() - INTERVAL 58 DAY, '22', NOW() - INTERVAL 58 DAY, NOW()),
-- 订单 2（liming 买 testuser 的 MacBook Air，已完成）消息链
('MSG18', NULL, '3',  3, '订单创建成功', '你已成功下单「MacBook Air M2 13寸 16+512 深空灰」，请尽快完成支付。', 1, NOW() - INTERVAL 55 DAY, '2', NOW() - INTERVAL 55 DAY, NOW()),
('MSG19', NULL, '1',  3, '收到新订单',   '你的商品「MacBook Air M2 13寸 16+512 深空灰」有新订单，请尽快处理。', 1, NOW() - INTERVAL 55 DAY, '2', NOW() - INTERVAL 55 DAY, NOW()),
('MSG20', NULL, '3',  4, '支付成功',     '订单支付成功，资产方将尽快为你发货。', 1, NOW() - INTERVAL 55 DAY, '2', NOW() - INTERVAL 55 DAY, NOW()),
('MSG21', NULL, '1',  4, '认领方已付款', '认领方已付款，请尽快发货。', 1, NOW() - INTERVAL 55 DAY, '2', NOW() - INTERVAL 55 DAY, NOW()),
('MSG22', NULL, '3',  3, '订单已完成',   '你已确认收货，订单完成，交易款已结算给资产方。', 1, NOW() - INTERVAL 53 DAY, '2', NOW() - INTERVAL 53 DAY, NOW()),
('MSG23', NULL, '1',  3, '交易完成',     '买家已确认收货，交易款已结算至你的账户。', 1, NOW() - INTERVAL 53 DAY, '2', NOW() - INTERVAL 53 DAY, NOW()),
-- 订单 3（chenxiao 买 testuser 的 MacBook Pro，已完成）消息链
('MSG24', NULL, '8',  3, '订单创建成功', '你已成功下单「MacBook Pro 14寸 M3 Pro 16+512」，请尽快完成支付。', 1, NOW() - INTERVAL 50 DAY, '3', NOW() - INTERVAL 50 DAY, NOW()),
('MSG25', NULL, '1',  3, '收到新订单',   '你的商品「MacBook Pro 14寸 M3 Pro 16+512」有新订单，请尽快处理。', 1, NOW() - INTERVAL 50 DAY, '3', NOW() - INTERVAL 50 DAY, NOW()),
('MSG26', NULL, '8',  3, '订单已完成',   '你已确认收货，订单完成，交易款已结算给资产方。', 1, NOW() - INTERVAL 48 DAY, '3', NOW() - INTERVAL 48 DAY, NOW()),
('MSG27', NULL, '1',  3, '交易完成',     '买家已确认收货，交易款已结算至你的账户。', 1, NOW() - INTERVAL 48 DAY, '3', NOW() - INTERVAL 48 DAY, NOW()),
-- 订单 13（wangfang 买 zhouyang 的 PS5，已发货）消息链
('MSG28', NULL, '4',  3, '订单创建成功', '你已成功下单「PS5 光驱版 国行主机」，请尽快完成支付。', 1, NOW() - INTERVAL 6 DAY, '13', NOW() - INTERVAL 6 DAY, NOW()),
('MSG29', NULL, '7',  3, '收到新订单',   '你的商品「PS5 光驱版 国行主机」有新订单，请尽快处理。', 1, NOW() - INTERVAL 6 DAY, '13', NOW() - INTERVAL 6 DAY, NOW()),
('MSG30', NULL, '4',  4, '支付成功',     '订单支付成功，资产方将尽快为你发货。', 1, NOW() - INTERVAL 5 DAY, '13', NOW() - INTERVAL 5 DAY, NOW()),
('MSG31', NULL, '7',  4, '认领方已付款', '认领方已付款，请尽快发货。', 0, NULL, '13', NOW() - INTERVAL 5 DAY, NOW()),
-- 订单 15（sunli 买 testuser 的 AirPods Pro 2，待付款）消息链
('MSG32', NULL, '6',  3, '订单创建成功', '你已成功下单「AirPods Pro 2 全新未拆封」，请尽快完成支付，30 分钟未付款订单自动取消。', 0, NULL, '15', NOW() - INTERVAL 1 DAY, NOW()),
('MSG33', NULL, '1',  3, '收到新订单',   '你的商品「AirPods Pro 2 全新未拆封」有新订单，请尽快处理。', 0, NULL, '15', NOW() - INTERVAL 1 DAY, NOW()),
-- 订单 19（wangfang 买 liming 的富士 X-T30 II，已退款）消息链
('MSG34', NULL, '4',  4, '退款成功',     '你的退款申请已通过平台仲裁，款项将原路退回支付账户。', 1, NOW() - INTERVAL 11 DAY, '19', NOW() - INTERVAL 11 DAY, NOW()),
('MSG35', NULL, '3',  3, '订单已退款',   '买家申请退款并通过平台仲裁，款项已原路退回。', 1, NOW() - INTERVAL 11 DAY, '19', NOW() - INTERVAL 11 DAY, NOW()),
-- 私聊：liming ↔ testuser 聊 MacBook Air（对应订单 2）
('MSG36', '3',  '1',  2, NULL, 'MacBook Air 还在吗？电池循环多少次了？', 1, NOW() - INTERVAL 56 DAY, NULL, NOW() - INTERVAL 56 DAY, NOW()),
('MSG37', '1',  '3',  2, NULL, '在的，循环 78 次，电池健康度 92%', 1, NOW() - INTERVAL 56 DAY, NULL, NOW() - INTERVAL 56 DAY, NOW()),
('MSG38', '3',  '1',  2, NULL, '6499 还能少点吗？', 1, NOW() - INTERVAL 56 DAY, NULL, NOW() - INTERVAL 56 DAY, NOW()),
('MSG39', '1',  '3',  2, NULL, '诚心要的话 6300 拿走', 1, NOW() - INTERVAL 56 DAY, NULL, NOW() - INTERVAL 56 DAY, NOW()),
('MSG40', '3',  '1',  2, NULL, '成交，我直接下单了', 1, NOW() - INTERVAL 55 DAY, NULL, NOW() - INTERVAL 55 DAY, NOW()),
-- 私聊：chenxiao ↔ testuser 聊 MacBook Pro（对应订单 3）
('MSG41', '8',  '1',  2, NULL, 'MBP 屏幕有划痕吗？能当面验机吗？', 1, NOW() - INTERVAL 51 DAY, NULL, NOW() - INTERVAL 51 DAY, NOW()),
('MSG42', '1',  '8',  2, NULL, '没有划痕，一直贴膜用的，图书馆可以当面验', 1, NOW() - INTERVAL 51 DAY, NULL, NOW() - INTERVAL 51 DAY, NOW()),
('MSG43', '8',  '1',  2, NULL, '好，那我周末过去', 1, NOW() - INTERVAL 50 DAY, NULL, NOW() - INTERVAL 50 DAY, NOW()),
-- 私聊：wangfang ↔ zhouyang 聊 PS5（对应订单 13）
('MSG44', '4',  '7',  2, NULL, 'PS5 配几个手柄？拆修过吗？', 1, NOW() - INTERVAL 7 DAY, NULL, NOW() - INTERVAL 7 DAY, NOW()),
('MSG45', '7',  '4',  2, NULL, '一个原装手柄，没拆修过，附赠两只游戏', 1, NOW() - INTERVAL 7 DAY, NULL, NOW() - INTERVAL 7 DAY, NOW()),
('MSG46', '4',  '7',  2, NULL, '行，我今天下单，麻烦尽快发', 0, NULL, NULL, NOW() - INTERVAL 6 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `type`       = new.`type`,
    `is_read`    = new.`is_read`,
    `read_time`  = new.`read_time`,
    `update_time` = NOW();

-- ===================================================================
-- 11. 搜索历史（(user_id, keyword) 唯一；关键词全部能在种子商品上命中）
-- ===================================================================

INSERT INTO `eo_search_history` (
    `id`, `user_id`, `keyword`, `search_time`, `create_time`, `update_time`
) VALUES
('SH01', '1',  'iPhone',         NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY, NOW()),
('SH02', '1',  'MacBook',        NOW() - INTERVAL 26 DAY, NOW() - INTERVAL 26 DAY, NOW()),
('SH03', '1',  '相机',            NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY, NOW()),
('SH04', '1',  '降噪耳机',        NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 8 DAY,  NOW()),
('SH05', '3',  '华为手机',        NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY, NOW()),
('SH06', '3',  '球鞋',            NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, NOW()),
('SH07', '3',  '富士相机',        NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY,  NOW()),
('SH08', '4',  '考研数学',        NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY, NOW()),
('SH09', '4',  'PS5',            NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 7 DAY,  NOW()),
('SH10', '4',  '球鞋',            NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY, NOW()),
('SH11', '5',  '相机',            NOW() - INTERVAL 22 DAY, NOW() - INTERVAL 22 DAY, NOW()),
('SH12', '5',  '自行车',          NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 9 DAY,  NOW()),
('SH13', '5',  'Switch',         NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY, NOW()),
('SH14', '6',  '加湿器',          NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY, NOW()),
('SH15', '6',  '考研英语',        NOW() - INTERVAL 16 DAY, NOW() - INTERVAL 16 DAY, NOW()),
('SH16', '7',  '游戏本',          NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 24 DAY, NOW()),
('SH17', '7',  '羽毛球拍',        NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY,  NOW()),
('SH18', '8',  '算法',            NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY, NOW()),
('SH19', '8',  '扩展坞',          NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY,  NOW()),
('SH20', '9',  '全画幅相机',      NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY, NOW()),
('SH21', '9',  'Xbox',           NOW() - INTERVAL 21 DAY, NOW() - INTERVAL 21 DAY, NOW()),
('SH22', '10', '冲锋衣',          NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY, NOW()),
('SH23', '10', '山地车',          NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY,  NOW()),
('SH24', '11', '掌机',            NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY, NOW()),
('SH25', '11', '游戏账号',        NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY,  NOW()),
('SH26', '12', '空气净化器',      NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 8 DAY,  NOW()),
('SH27', '12', '羽绒服',          NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY,  NOW()),
('SH28', '13', '智能手表',        NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY, NOW()),
('SH29', '13', '无线鼠标',        NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY,  NOW()),
('SH30', '14', '音箱',            NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 9 DAY,  NOW()),
('SH31', '14', '台灯',            NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY,  NOW()),
('SH32', '5',  '微单',            NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY,  NOW())
AS new
ON DUPLICATE KEY UPDATE
    `search_time` = new.`search_time`,
    `update_time` = NOW();

-- ===================================================================
-- 12. 热门关键词（词面与种子商品对齐；search_count 拉开梯度供榜单演示）
-- ===================================================================

INSERT INTO `eo_hot_keyword` (
    `id`, `keyword`, `search_count`, `last_search_time`, `create_time`, `update_time`
) VALUES
('HK01', 'iPhone',        356, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 90 DAY, NOW()),
('HK02', 'MacBook',       289, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 90 DAY, NOW()),
('HK03', '相机',          334, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 90 DAY, NOW()),
('HK04', 'PS5',           245, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 80 DAY, NOW()),
('HK05', '考研资料',       198, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 60 DAY, NOW()),
('HK06', '游戏本',        198, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 60 DAY, NOW()),
('HK07', '降噪耳机',       223, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 55 DAY, NOW()),
('HK08', '自行车',        210, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 50 DAY, NOW()),
('HK09', 'Switch',        156, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 50 DAY, NOW()),
('HK10', '教材',          176, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 45 DAY, NOW()),
('HK11', '考研数学',       167, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 45 DAY, NOW()),
('HK12', '华为手机',       145, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 40 DAY, NOW()),
('HK13', '球鞋',          134, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 40 DAY, NOW()),
('HK14', '智能手表',       98, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 30 DAY, NOW()),
('HK15', '瑜伽垫',         87, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 30 DAY, NOW()),
('HK16', '掌机',           89, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 30 DAY, NOW()),
('HK17', '空气净化器',      76, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 25 DAY, NOW()),
('HK18', '游戏账号',       72, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 25 DAY, NOW()),
('HK19', '加湿器',         64, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 20 DAY, NOW()),
('HK20', '三体',           56, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 20 DAY, NOW()),
('HK21', '羽毛球拍',       45, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 15 DAY, NOW()),
('HK22', '扩展坞',         38, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 15 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `search_count`     = new.`search_count`,
    `last_search_time` = new.`last_search_time`,
    `update_time`      = NOW();

-- ===================================================================
-- 13. 审计日志（只引用真实 Controller 与真实路由）
-- ===================================================================

INSERT INTO `eo_audit_log` (
    `id`, `title`, `business_type`, `method`, `request_method`, `operator_type`,
    `username`, `request_url`, `client_ip`, `status`, `duration`, `created_at`
) VALUES
('AUD01', '认证', '2', 'AuthController.login()',               'POST', 1, 'testuser',   '/api/auth/login',                 '192.168.1.100', 0, 156,  NOW() - INTERVAL 45 DAY),
('AUD02', '商品', '2', 'ProductController.create()',           'POST', 1, 'testuser',   '/api/products',                   '192.168.1.100', 0, 892,  NOW() - INTERVAL 45 DAY),
('AUD03', '商品', '2', 'ProductController.create()',           'POST', 1, 'liming',     '/api/products',                   '10.0.0.55',     0, 765,  NOW() - INTERVAL 40 DAY),
('AUD04', '商品', '2', 'ProductController.update()',           'PUT',  1, 'testuser',   '/api/products/1',                 '192.168.1.100', 0, 267,  NOW() - INTERVAL 30 DAY),
('AUD05', '搜索', '2', 'ProductSearchController.search()',     'GET',  1, 'wangfang',   '/api/products/search?keyword=PS5', '172.16.0.23',   0, 78,   NOW() - INTERVAL 20 DAY),
('AUD06', '订单', '2', 'OrderCommandController.create()',      'POST', 1, 'liming',     '/api/orders',                     '10.0.0.55',     0, 234,  NOW() - INTERVAL 55 DAY),
('AUD07', '支付', '2', 'PaymentCommandController.pay()',       'POST', 1, 'liming',     '/api/payments',                   '10.0.0.55',     0, 567,  NOW() - INTERVAL 55 DAY),
('AUD08', '订单', '2', 'OrderCommandController.create()',      'POST', 1, 'wangfang',   '/api/orders',                     '172.16.0.23',   0, 189,  NOW() - INTERVAL 6 DAY),
('AUD09', '消息', '2', 'MessageCommandController.send()',      'POST', 1, 'wangfang',   '/api/messages',                   '172.16.0.23',   0, 96,   NOW() - INTERVAL 6 DAY),
('AUD10', '商品审核', '2', 'AdminProductController.audit()',   'PUT',  2, 'admin',      '/api/admin/products/58/audit',    '10.0.0.1',      0, 312,  NOW() - INTERVAL 6 DAY),
('AUD11', '文件', '2', 'FileController.upload()',              'POST', 1, 'testuser',   '/api/file',                       '192.168.1.100', 0, 1204, NOW() - INTERVAL 5 DAY),
('AUD12', '搜索', '2', 'AdminSearchReindexController.reindex()', 'POST', 2, 'admin',   '/api/admin/search/reindex',       '10.0.0.1',      0, 2345, NOW() - INTERVAL 2 DAY)
AS new
ON DUPLICATE KEY UPDATE
    `method`      = new.`method`,
    `request_url` = new.`request_url`,
    `duration`    = new.`duration`,
    `status`      = new.`status`;

-- ===================================================================
-- 14. testuser 长期画像（Agent 记忆演示：首轮对话即注入既有偏好）
--      pref_key 限 ChatTools.PREFERENCE_KEYS：condition / price_range / style / location
-- ===================================================================

INSERT INTO `eo_user_preference` (
    `id`, `user_id`, `pref_key`, `pref_value`, `create_time`, `update_time`
) VALUES
('UP1', '1', 'condition',  '九成新以上',        NOW() - INTERVAL 30 DAY, NOW()),
('UP2', '1', 'price_range', '5000 以内',        NOW() - INTERVAL 21 DAY, NOW()),
('UP3', '1', 'style',      '数码与教材',       NOW() - INTERVAL 14 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `pref_value`  = new.`pref_value`,
    `update_time` = NOW();

COMMIT;
