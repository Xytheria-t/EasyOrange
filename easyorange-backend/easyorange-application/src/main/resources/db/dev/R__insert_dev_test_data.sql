-- ===================================================================
-- EasyOrange 校园二手交易平台 - 开发环境测试数据
-- 说明：仅在 dev profile 中通过 classpath:db/dev 加载
-- ===================================================================

INSERT INTO `sys_user` (
    `user_id`, `username`, `password`, `user_type`, `nick_name`,
    `sex`, `status`, `del_flag`, `create_time`, `update_time`
) VALUES (
    1,
    'testuser',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E',
    '01',
    '测试用户',
    '0',
    '0',
    0,
    NOW(),
    NOW()
) AS new
ON DUPLICATE KEY UPDATE
    `nick_name` = new.`nick_name`,
    `status` = new.`status`,
    `del_flag` = new.`del_flag`,
    `update_time` = new.`update_time`;

INSERT INTO `product` (
    `id`, `user_id`, `category_id`, `name`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `contact_method`, `tags`, `search_text`, `del_flag`, `create_time`, `update_time`
) VALUES
(1, 1, 1, 'iPhone 14 Pro Max 256G 暗紫色', 5999.00, 7999.00, 1, 1, 128, 9, '校园内', '微信: test123', '苹果,手机,旗舰', 'iPhone 14 Pro Max 256G 暗紫色 苹果 手机 旗舰', 0, NOW(), NOW()),
(2, 1, 1, 'MacBook Air M2 13寸轻薄笔记本', 6499.00, 8999.00, 1, 1, 86, 8, '图书馆', '微信: test123', '苹果,电脑,轻薄', 'MacBook Air M2 13寸轻薄笔记本 苹果 电脑 轻薄', 0, NOW(), NOW()),
(3, 1, 2, '高等数学教材全套 上下册', 89.00, 128.00, 1, 1, 45, 7, '教学楼', '电话: 13800138000', '教材,数学,大一', '高等数学教材全套 上下册 教材 数学 大一', 0, NOW(), NOW()),
(4, 1, 2, '数据结构与算法 Python实现', 45.00, 69.00, 1, 1, 32, 6, '宿舍区', '微信: test123', '计算机,算法,编程', '数据结构与算法 Python实现 计算机 算法 编程', 0, NOW(), NOW()),
(5, 1, 1, 'AirPods Pro 2 代 全新未拆封', 1299.00, 1899.00, 1, 1, 156, 9, '校园内', '微信: test123', '苹果,耳机,降噪', 'AirPods Pro 2 代 全新未拆封 苹果 耳机 降噪', 0, NOW(), NOW()),
(6, 1, 1, '小米手环8 NFC版 黑色', 299.00, 349.00, 1, 1, 78, 8, '体育场', '电话: 13800138000', '小米,手环,NFC', '小米手环8 NFC版 黑色 小米 手环 NFC', 0, NOW(), NOW()),
(7, 1, 2, '研究生英语教材 全新', 35.00, 58.00, 1, 1, 23, 5, '研究生楼', '微信: test123', '英语,研究生,教材', '研究生英语教材 全新 英语 研究生 教材', 0, NOW(), NOW()),
(8, 1, 3, '阿迪达斯 UltraBoost 运动鞋 42码', 299.00, 899.00, 1, 1, 67, 7, '操场旁', '微信: test123', '阿迪达斯,运动鞋,跑步', '阿迪达斯 UltraBoost 运动鞋 42码 阿迪达斯 运动鞋 跑步', 0, NOW(), NOW()),
(9, 1, 3, 'Nike 运动双肩背包 黑色', 159.00, 299.00, 1, 1, 41, 8, '宿舍区', '电话: 13800138000', 'Nike,背包,运动', 'Nike 运动双肩背包 黑色 Nike 背包 运动', 0, NOW(), NOW()),
(10, 1, 4, '懒人加湿器 超声波静音款', 89.00, 159.00, 1, 1, 55, 9, '宿舍区', '微信: test123', '加湿器,静音,家用', '懒人加湿器 超声波静音款 加湿器 静音 家用', 0, NOW(), NOW()),
(11, 1, 6, 'Switch 游戏卡带 塞尔达传说', 268.00, 359.00, 1, 1, 92, 7, '宿舍区', '微信: test123', 'Switch,游戏,塞尔达', 'Switch 游戏卡带 塞尔达传说 Switch 游戏 塞尔达', 0, NOW(), NOW()),
(12, 1, 5, '健身瑜伽垫加厚加宽防滑', 69.00, 99.00, 1, 1, 38, 8, '体育馆', '电话: 13800138000', '瑜伽,健身,防滑', '健身瑜伽垫加厚加宽防滑 瑜伽 健身 防滑', 0, NOW(), NOW())
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

INSERT INTO `product_image` (
    `id`, `product_id`, `image_url`, `sort_order`, `is_main`, `create_time`, `update_time`
) VALUES
(1, 1, 'https://picsum.photos/seed/iphone/400/400', 0, 1, NOW(), NOW()),
(2, 2, 'https://picsum.photos/seed/macbook/400/400', 0, 1, NOW(), NOW()),
(3, 3, 'https://picsum.photos/seed/mathbook/400/400', 0, 1, NOW(), NOW()),
(4, 4, 'https://picsum.photos/seed/dsalgo/400/400', 0, 1, NOW(), NOW()),
(5, 5, 'https://picsum.photos/seed/airpods/400/400', 0, 1, NOW(), NOW()),
(6, 6, 'https://picsum.photos/seed/miband/400/400', 0, 1, NOW(), NOW()),
(7, 7, 'https://picsum.photos/seed/english/400/400', 0, 1, NOW(), NOW()),
(8, 8, 'https://picsum.photos/seed/nikeshoe/400/400', 0, 1, NOW(), NOW()),
(9, 9, 'https://picsum.photos/seed/nikebag/400/400', 0, 1, NOW(), NOW()),
(10, 10, 'https://picsum.photos/seed/humidifier/400/400', 0, 1, NOW(), NOW()),
(11, 11, 'https://picsum.photos/seed/switch/400/400', 0, 1, NOW(), NOW()),
(12, 12, 'https://picsum.photos/seed/yogamat/400/400', 0, 1, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url` = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main` = new.`is_main`,
    `update_time` = new.`update_time`;

INSERT INTO `product_detail` (
    `id`, `product_id`, `description`, `create_time`, `update_time`
) VALUES
(1, 1, 'iPhone 14 Pro Max 256G 暗紫色 国行正品 在保<br><br>【配置】256G存储、暗紫色、灵动岛、全网通5G<br><br>【成色】9成新，轻微使用痕迹，屏幕无划痕，电池健康度92%<br><br>【配件】原装充电线、说明书<br><br>【购买渠道】官网购入，有购买凭证', NOW(), NOW()),
(2, 2, 'MacBook Air M2 13寸轻薄笔记本 深空灰 16+512<br><br>【配置】M2芯片、16G内存、512G固态硬盘<br><br>【成色】8成新，A面有轻微划痕，功能全部正常<br><br>【配件】原装充电器、包装盒<br><br>【电池循环】仅78次，性能依旧强劲', NOW(), NOW()),
(3, 3, '高等数学教材全套 上下册第七版 同济大学<br><br>【版本】第七版，同济大学数学系编<br><br>【成色】7成新，笔记较多但不影响阅读<br><br>【内容】上册：函数与极限、导数与微分等；下册：积分、空间解析几何等<br><br>【适合】大一新生考研复习', NOW(), NOW()),
(4, 4, '数据结构与算法 Python实现 机械工业出版社<br><br>【作者】邓俊辉<br><br>【成色】6成新，书脊有折痕<br><br>【内容】涵盖数组、链表、树、图等数据结构，以及排序、查找等算法<br><br>【适合】计算机专业学生、考研复习', NOW(), NOW()),
(5, 5, 'AirPods Pro 2 代 全新未拆封 正品保障<br><br>【型号】AirPods Pro (第二代) 带MagSafe充电盒<br><br>【成色】全新未拆封，原厂塑封完整<br><br>【配件】原装耳机、充电盒、充电线、说明书<br><br>【保修】未激活，在保', NOW(), NOW()),
(6, 6, '小米手环8 NFC版 黑色 原装配件齐全<br><br>【功能】NFC门禁、NFC支付、心率监测、睡眠监测、血氧饱和度<br><br>【成色】8成新，屏幕无划痕，腕带无破损<br><br>【配件】原装充电器、说明书<br><br>【电池续航】约7天', NOW(), NOW()),
(7, 7, '研究生英语教材 全新未使用 指定教材<br><br>【书名】研究生英语系列教程<br><br>【成色】全新，购入后未使用<br><br>【版本】最新版<br><br>【适合】研究生一年级学生', NOW(), NOW()),
(8, 8, '阿迪达斯 UltraBoost 运动鞋 42码 黑色<br><br>【型号】UltraBoost 22 实战篮球鞋<br><br>【尺码】42码，适合脚长26cm左右<br><br>【成色】7成新，鞋底略有磨损，鞋面无破损<br><br>【技术】Boost中底，舒适回弹', NOW(), NOW()),
(9, 9, 'Nike 运动双肩背包 黑色 30L大容量<br><br>【容量】30L，可放置15.6寸笔记本<br><br>【成色】8成新，整体清洁，无明显破损<br><br>【功能】多口袋设计透气背垫电脑隔层<br><br>【适用】上学、旅行、健身', NOW(), NOW()),
(10, 10, '懒人加湿器 超声波静音款 4.5L大容量<br><br>【容量】4.5L，持续加湿12小时<br><br>【特点】超声波静音技术，运行时噪音低于35dB<br><br>【成色】9成新，使用时间不超过1个月<br><br>【功能】智能恒湿、定时关机、过夜保护', NOW(), NOW()),
(11, 11, 'Switch 游戏卡带 塞尔达传说 旷野之息<br><br>【游戏】塞尔达传说：旷野之息<br><br>【成色】7成新，卡带轻微磨损，不影响读取<br><br>【版本】实体卡带版<br><br>【附赠】无，需要单独购买', NOW(), NOW()),
(12, 12, '健身瑜伽垫加厚加宽防滑 默认送收纳绑带<br><br>【尺寸】183cm*80cm*10mm加宽加厚款<br><br>【材质】TPE环保材质<br><br>【成色】8成新，无异味，表面防滑性能良好<br><br>【适用】瑜伽、普拉提、健身训练', NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `description` = new.`description`,
    `update_time` = new.`update_time`;
