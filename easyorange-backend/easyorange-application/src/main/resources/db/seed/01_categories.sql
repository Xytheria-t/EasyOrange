-- ===================================================================
-- EasyOrange 校园二手交易平台 - 基础分类种子数据
-- 说明：此文件包含系统启动所需的基础分类数据
-- 用途：通过 Spring Boot data.sql 或手动执行加载
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
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `sort_order` = VALUES(`sort_order`),
    `status` = VALUES(`status`),
    `update_time` = NOW();
