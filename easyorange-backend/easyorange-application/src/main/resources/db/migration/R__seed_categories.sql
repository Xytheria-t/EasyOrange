-- ===================================================================
-- EasyOrange - 分类种子数据
-- Description: Repeatable Migration - 全部分类基础数据（含二级）
-- Type: DML（可重复执行，ON DUPLICATE KEY UPDATE 保证幂等）
--
-- 【ID 口径】主键是 UUID v7 字符串（与全项目 ID 铁律一致）。
--   旧版这里用的是 1/2/10 这类自增数字，与「ID 统一 UUID v7 String（36 位）」冲突，
--   还倒逼 ES mapping 把 categoryId 映成 integer —— 数字 ID 一旦换成 UUID 就会全线崩。
--   固定 ID 让重放覆盖同一批行（dev 演示数据的商品行按这些 id 关联）。
--
-- 【根节点口径】一级分类 parent_id 为 NULL（不是字符串 '0'）。
--   库里的 '0' 与代码里的 null 是两套「根」的表示，导致「一级重名校验」恒不命中。
--
-- 【status 口径】VARCHAR '0'/'1'，对应 domain 的 CategoryStatus 枚举（@EnumValue 落库）。
-- ===================================================================

START TRANSACTION;

INSERT INTO `eo_category` (
    `id`, `name`, `parent_id`, `level`, `sort_order`, `status`,
    `del_flag`, `create_time`, `update_time`
) VALUES
-- 一级分类
('018bcfe5-6800-752e-89a7-834df2a74de4', '电子数码',   NULL, 1, 1, '1', 0, NOW(), NOW()),
('018bcfe5-6be8-7651-8317-1ff4a6a3a450', '书籍教材',   NULL, 1, 2, '1', 0, NOW(), NOW()),
('018bcfe5-6fd0-7128-a24b-e40ad23f0824', '服饰鞋包',   NULL, 1, 3, '1', 0, NOW(), NOW()),
('018bcfe5-73b8-7181-a54c-66175d9dc9f8', '生活用品',   NULL, 1, 4, '1', 0, NOW(), NOW()),
('018bcfe5-77a0-70ed-a079-d3bde8e25d94', '运动健身',   NULL, 1, 5, '1', 0, NOW(), NOW()),
('018bcfe5-7b88-736f-8580-28d6099950d8', '虚拟物品',   NULL, 1, 6, '1', 0, NOW(), NOW()),
-- 二级分类 · 电子数码
('018bcfe5-7f70-76f0-8478-82e36b0d549b', '手机',       '018bcfe5-6800-752e-89a7-834df2a74de4', 2, 1, '1', 0, NOW(), NOW()),
('018bcfe5-8358-73d9-a344-5bb31738f7d9', '电脑',       '018bcfe5-6800-752e-89a7-834df2a74de4', 2, 2, '1', 0, NOW(), NOW()),
('018bcfe5-8740-76ca-b4eb-252b0f21ddb6', '耳机音箱',   '018bcfe5-6800-752e-89a7-834df2a74de4', 2, 3, '1', 0, NOW(), NOW()),
('018bcfe5-8b28-790c-bca3-04171fb17c23', '智能穿戴',   '018bcfe5-6800-752e-89a7-834df2a74de4', 2, 4, '1', 0, NOW(), NOW()),
('018bcfe5-8f10-7392-a827-ddada170b338', '游戏设备',   '018bcfe5-6800-752e-89a7-834df2a74de4', 2, 5, '1', 0, NOW(), NOW()),
('018bcfe5-92f8-7953-83f5-8c3cf29d0da9', '相机',       '018bcfe5-6800-752e-89a7-834df2a74de4', 2, 6, '1', 0, NOW(), NOW()),
-- 二级分类 · 书籍教材
('018bcfe5-96e0-793b-9963-368595e60af5', '教材',       '018bcfe5-6be8-7651-8317-1ff4a6a3a450', 2, 1, '1', 0, NOW(), NOW()),
('018bcfe5-9ac8-70cb-8e26-3464f9ebdacc', '考研资料',   '018bcfe5-6be8-7651-8317-1ff4a6a3a450', 2, 2, '1', 0, NOW(), NOW()),
('018bcfe5-9eb0-70be-b6f1-25b28e81973e', '课外读物',   '018bcfe5-6be8-7651-8317-1ff4a6a3a450', 2, 3, '1', 0, NOW(), NOW()),
-- 二级分类 · 服饰鞋包
('018bcfe5-a298-7221-9ad3-2c904a23d596', '鞋靴',       '018bcfe5-6fd0-7128-a24b-e40ad23f0824', 2, 1, '1', 0, NOW(), NOW()),
('018bcfe5-a680-724e-8789-e8708a6a63ec', '服装',       '018bcfe5-6fd0-7128-a24b-e40ad23f0824', 2, 2, '1', 0, NOW(), NOW()),
('018bcfe5-aa68-7922-a3db-41564ef8aa38', '箱包',       '018bcfe5-6fd0-7128-a24b-e40ad23f0824', 2, 3, '1', 0, NOW(), NOW()),
-- 二级分类 · 生活用品
('018bcfe5-ae50-7d0e-8b91-0562ae97ba94', '宿舍资产',   '018bcfe5-73b8-7181-a54c-66175d9dc9f8', 2, 1, '1', 0, NOW(), NOW()),
('018bcfe5-b238-71a6-a48e-9cda94e3bf91', '数码配件',   '018bcfe5-73b8-7181-a54c-66175d9dc9f8', 2, 2, '1', 0, NOW(), NOW()),
-- 二级分类 · 运动健身
('018bcfe5-b620-7a38-97d5-5c80301850c5', '健身器材',   '018bcfe5-77a0-70ed-a079-d3bde8e25d94', 2, 1, '1', 0, NOW(), NOW()),
('018bcfe5-ba08-718f-ad93-39088c38fb29', '户外运动',   '018bcfe5-77a0-70ed-a079-d3bde8e25d94', 2, 2, '1', 0, NOW(), NOW()),
-- 二级分类 · 虚拟物品
('018bcfe5-bdf0-7101-83d0-816d907a70c3', '游戏账号',   '018bcfe5-7b88-736f-8580-28d6099950d8', 2, 1, '1', 0, NOW(), NOW()),
('018bcfe5-c1d8-79e7-9fc5-414934b9b5df', '会员卡券',   '018bcfe5-7b88-736f-8580-28d6099950d8', 2, 2, '1', 0, NOW(), NOW()),
('018bcfe5-c5c0-7ae2-9b5d-ac1f881ed162', '数字素材',   '018bcfe5-7b88-736f-8580-28d6099950d8', 2, 3, '1', 0, NOW(), NOW())
AS new
ON DUPLICATE KEY UPDATE
    `name`       = new.`name`,
    `parent_id`  = new.`parent_id`,
    `level`      = new.`level`,
    `sort_order` = new.`sort_order`,
    `status`     = new.`status`,
    `update_time` = NOW();

COMMIT;
