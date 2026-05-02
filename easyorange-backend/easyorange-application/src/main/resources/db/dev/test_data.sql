-- EasyOrange Test Data - Beautiful Products with Real Images
-- This file uses UTF-8 encoding

-- Clear existing test products
DELETE FROM product_image WHERE product_id IN (
    SELECT id FROM product WHERE user_id = 1 AND status IN (0,1,2,3)
);
DELETE FROM product_detail WHERE product_id IN (
    SELECT id FROM product WHERE user_id = 1 AND status IN (0,1,2,3)
);
DELETE FROM product WHERE user_id = 1 AND status IN (0,1,2,3);

-- Insert 12 Beautiful Products
INSERT INTO `product` (
    `id`, `user_id`, `category_id`, `name`, `price`, `original_price`,
    `stock`, `status`, `view_count`, `condition_level`, `location`,
    `contact_method`, `tags`, `search_text`, `del_flag`, `create_time`, `update_time`
) VALUES
-- Electronics
(1001, 1, 1, 'MacBook Pro 14寸 M3芯片 深空灰', 11999.00, 14999.00, 1, 1, 256, 2, '图书馆', '微信联系', '苹果,笔记本,高性能', 'MacBook Pro 14寸 M3芯片 苹果 笔记本 高性能', 0, NOW(), NOW()),
(1002, 1, 1, 'iPad Air 5 256G WiFi 蓝色', 3299.00, 4799.00, 1, 1, 189, 2, '校园内', '微信联系', '苹果,平板,学习', 'iPad Air 5 256G 苹果 平板 学习', 0, NOW(), NOW()),
(1003, 1, 1, 'AirPods Pro 2 全新未拆封', 1299.00, 1899.00, 1, 1, 312, 1, '宿舍区', '微信联系', '苹果,耳机,降噪', 'AirPods Pro 2 苹果 耳机 降噪', 0, NOW(), NOW()),
(1004, 1, 1, '小米13 Pro 256G 黑色', 2999.00, 4999.00, 1, 1, 145, 3, '教学楼', '微信联系', '小米,手机,拍照', '小米13 Pro 手机 拍照', 0, NOW(), NOW()),
(1005, 1, 1, 'Switch OLED 游戏机 白色', 1599.00, 2599.00, 1, 1, 198, 2, '体育馆', '微信联系', '游戏机,任天堂,娱乐', 'Switch OLED 游戏机 任天堂 娱乐', 0, NOW(), NOW()),

-- Books & Study
(1006, 1, 2, '考研英语词汇红宝书 2024版', 35.00, 68.00, 1, 1, 88, 3, '考研自习室', '微信联系', '考研,英语,词汇', '考研英语词汇 红宝书 考研 英语', 0, NOW(), NOW()),
(1007, 1, 2, '高等数学同济第七版 上下册', 45.00, 89.00, 1, 1, 67, 3, '数学楼', '微信联系', '教材,数学,高数', '高等数学 同济第七版 教材 数学', 0, NOW(), NOW()),
(1008, 1, 2, '数据结构与算法 Python版', 55.00, 99.00, 1, 1, 76, 2, '计算机学院', '微信联系', '计算机,算法,编程', '数据结构 算法 Python 计算机', 0, NOW(), NOW()),

-- Clothing & Accessories
(1009, 1, 3, 'Nike Air Jordan 1 经典黑白 42码', 699.00, 1299.00, 1, 1, 156, 2, '操场', '微信联系', 'Nike,球鞋,经典', 'Nike Air Jordan 1 球鞋 经典', 0, NOW(), NOW()),
(1010, 1, 3, '北面冲锋衣 黑色 M码 防水', 399.00, 899.00, 1, 1, 123, 2, '宿舍区', '微信联系', '北面,外套,户外', '北面 冲锋衣 外套 户外', 0, NOW(), NOW()),

-- Daily Life
(1011, 1, 4, '小米台灯Pro 护眼阅读灯', 89.00, 149.00, 1, 1, 95, 2, '图书馆', '微信联系', '小米,台灯,护眼', '小米台灯Pro 护眼 台灯 阅读', 0, NOW(), NOW()),
(1012, 1, 5, '迪卡侬山地自行车 27速', 899.00, 1599.00, 1, 1, 112, 2, '停车场', '微信联系', '自行车,运动,出行', '迪卡侬 山地自行车 运动 出行', 0, NOW(), NOW())
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

-- Insert Product Images (Different images for each product)
INSERT INTO `product_image` (
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
-- Xiaomi 13 Pro
(2007, 1004, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2008, 1004, 'https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- Switch OLED
(2009, 1005, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2010, 1005, 'https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- English Vocabulary Book
(2011, 1006, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Math Textbook
(2012, 1007, 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Data Structure Book
(2013, 1008, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Nike AJ1
(2014, 1009, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2015, 1009, 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW()),
-- North Face Jacket
(2016, 1010, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Xiaomi Lamp
(2017, 1011, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
-- Bicycle
(2018, 1012, 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?w=800&auto=format&fit=crop', 0, 1, NOW(), NOW()),
(2019, 1012, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&auto=format&fit=crop', 1, 0, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `image_url` = new.`image_url`,
    `sort_order` = new.`sort_order`,
    `is_main` = new.`is_main`,
    `update_time` = new.`update_time`;

-- Insert Product Details
INSERT INTO `product_detail` (
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
(1012, '迪卡山地自行车 27速<br><br>【型号】Rockrider ST520 铝合金车架<br><br>【变速】27速禧玛诺变速系统<br><br>【成色】7成新，轮胎磨损正常<br><br>【配置】前后碟刹、避震前叉、水壶架', NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `description` = new.`description`,
    `update_time` = new.`update_time`;
