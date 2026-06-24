-- ===================================================================
-- EasyOrange AI 智能托管平台 - 分类种子数据
-- Description: Repeatable Migration - 商品分类基础数据
-- Type: DML（可重复执行）
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- 设计说明:
--   使用 ON DUPLICATE KEY UPDATE 保证幂等性
--   使用 MySQL 8.0.20+ 推荐的 AS new 别名语法
--   如需重置，执行: DELETE FROM eo_category;
-- ===================================================================

START TRANSACTION;

-- ===================================================================
-- 一级分类
-- ===================================================================

INSERT INTO `eo_category` (
    `id`, `name`, `parent_id`, `level`, `sort_order`, `status`,
    `del_flag`, `create_time`, `update_time`
) VALUES
(1, '电子数码', 0, 1, 1, 1, 0, NOW(), NOW()),
(2, '书籍教材', 0, 1, 2, 1, 0, NOW(), NOW()),
(3, '服饰鞋包', 0, 1, 3, 1, 0, NOW(), NOW()),
(4, '生活用品', 0, 1, 4, 1, 0, NOW(), NOW()),
(5, '运动健身', 0, 1, 5, 1, 0, NOW(), NOW()),
(6, '虚拟物品', 0, 1, 6, 1, 0, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `name` = new.`name`,
    `sort_order` = new.`sort_order`,
    `status` = new.`status`,
    `update_time` = NOW();

-- ===================================================================
-- 二级分类
-- ===================================================================

INSERT INTO `eo_category` (
    `id`, `name`, `parent_id`, `level`, `sort_order`, `status`,
    `del_flag`, `create_time`, `update_time`
) VALUES
(10, '手机',     1, 2, 1, 1, 0, NOW(), NOW()),
(11, '电脑',     1, 2, 2, 1, 0, NOW(), NOW()),
(12, '耳机音箱', 1, 2, 3, 1, 0, NOW(), NOW()),
(13, '智能穿戴', 1, 2, 4, 1, 0, NOW(), NOW()),
(14, '游戏设备', 1, 2, 5, 1, 0, NOW(), NOW()),
(20, '教材',     2, 2, 1, 1, 0, NOW(), NOW()),
(21, '考研资料', 2, 2, 2, 1, 0, NOW(), NOW()),
(22, '课外读物', 2, 2, 3, 1, 0, NOW(), NOW()),
(30, '鞋靴',     3, 2, 1, 1, 0, NOW(), NOW()),
(31, '服装',     3, 2, 2, 1, 0, NOW(), NOW()),
(32, '箱包',     3, 2, 3, 1, 0, NOW(), NOW()),
(40, '宿舍好物', 4, 2, 1, 1, 0, NOW(), NOW()),
(41, '数码配件', 4, 2, 2, 1, 0, NOW(), NOW()),
(50, '健身器材', 5, 2, 1, 1, 0, NOW(), NOW()),
(51, '户外运动', 5, 2, 2, 1, 0, NOW(), NOW()),
(60, '游戏账号', 6, 2, 1, 1, 0, NOW(), NOW()),
(61, '会员卡券', 6, 2, 2, 1, 0, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `name` = new.`name`,
    `parent_id` = new.`parent_id`,
    `level` = new.`level`,
    `sort_order` = new.`sort_order`,
    `status` = new.`status`,
    `update_time` = NOW();

COMMIT;