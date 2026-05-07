-- ===================================================================
-- EasyOrange 校园二手交易平台 - 测试数据：用户模块
-- 说明：此文件包含开发环境测试用户数据
-- 用途：通过 TestDataLoader 或手动执行加载
-- ===================================================================

-- ===================================================================
-- 1. 用户数据（10个用户：不同类型、性别、状态）
-- ===================================================================

INSERT INTO `eo_user` (
    `user_id`, `username`, `password`, `user_type`, `nick_name`,
    `sex`, `status`, `del_flag`, `email`, `phonenumber`, `student_id`,
    `real_name`, `avatar`, `create_time`, `update_time`
) VALUES
(1,  'testuser',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '测试用户',   '0', '0', 0, 'testuser@campus.edu',    '13800138001', '2023001', '张三',   'https://picsum.photos/seed/avatar1/100/100',  NOW() - INTERVAL 90 DAY, NOW()),
(2,  'admin',     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '02', '管理员',     '1', '0', 0, 'admin@campus.edu',       '13800138002', NULL,      '李管理', 'https://picsum.photos/seed/avatar2/100/100',  NOW() - INTERVAL 120 DAY, NOW()),
(3,  'liming',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '黎明',       '1', '0', 0, 'liming@campus.edu',      '13800138003', '2023002', '黎明',   'https://picsum.photos/seed/avatar3/100/100',  NOW() - INTERVAL 60 DAY, NOW()),
(4,  'wangfang',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '小芳同学',   '2', '0', 0, 'wangfang@campus.edu',    '13800138004', '2023003', '王芳',   'https://picsum.photos/seed/avatar4/100/100',  NOW() - INTERVAL 45 DAY, NOW()),
(5,  'zhaowei',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '赵伟',       '1', '0', 0, 'zhaowei@campus.edu',     '13800138005', '2023004', '赵伟',   'https://picsum.photos/seed/avatar5/100/100',  NOW() - INTERVAL 30 DAY, NOW()),
(6,  'sunli',     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '孙丽',       '2', '0', 0, 'sunli@campus.edu',       '13800138006', '2023005', '孙丽',   'https://picsum.photos/seed/avatar6/100/100',  NOW() - INTERVAL 20 DAY, NOW()),
(7,  'zhouyang',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '周洋',       '1', '0', 0, 'zhouyang@campus.edu',    '13800138007', '2023006', '周洋',   'https://picsum.photos/seed/avatar7/100/100',  NOW() - INTERVAL 15 DAY, NOW()),
(8,  'chenxiao',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '陈晓',       '2', '0', 0, 'chenxiao@campus.edu',    '13800138008', '2023007', '陈晓',   'https://picsum.photos/seed/avatar8/100/100',  NOW() - INTERVAL 10 DAY, NOW()),
(9,  'lockeduser','$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '被锁定用户', '0', '2', 0, 'locked@campus.edu',      '13800138009', '2023008', '刘锁',   NULL,                                           NOW() - INTERVAL 5 DAY, NOW()),
(10, 'disableduser','$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG','01', '已禁用用户', '0', '1', 0, 'disabled@campus.edu',    '13800138010', '2023009', '吴禁',   NULL,                                           NOW() - INTERVAL 3 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `nick_name` = new.`nick_name`,
    `status` = new.`status`,
    `del_flag` = new.`del_flag`,
    `update_time` = new.`update_time`;

-- ===================================================================
-- 2. 补充用户数据（8个新用户：不同专业、年级、活跃度）
-- ===================================================================

INSERT INTO `eo_user` (
    `user_id`, `username`, `password`, `user_type`, `nick_name`,
    `sex`, `status`, `del_flag`, `email`, `phonenumber`, `student_id`,
    `real_name`, `avatar`, `create_time`, `update_time`
) VALUES
(11, 'huangjie',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '黄杰学长',   '1', '0', 0, 'huangjie@campus.edu',    '13800138011', '2021001', '黄杰', 'https://picsum.photos/seed/avatar11/100/100', NOW() - INTERVAL 180 DAY, NOW()),
(12, 'liuyan',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '刘燕',       '2', '0', 0, 'liuyan@campus.edu',      '13800138012', '2022001', '刘燕', 'https://picsum.photos/seed/avatar12/100/100', NOW() - INTERVAL 120 DAY, NOW()),
(13, 'wanghai',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '王海',       '1', '0', 0, 'wanghai@campus.edu',     '13800138013', '2022002', '王海', 'https://picsum.photos/seed/avatar13/100/100', NOW() - INTERVAL 90 DAY, NOW()),
(14, 'zhangmei',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '张梅',       '2', '0', 0, 'zhangmei@campus.edu',    '13800138014', '2023003', '张梅', 'https://picsum.photos/seed/avatar14/100/100', NOW() - INTERVAL 60 DAY, NOW()),
(15, 'liguang',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '李光',       '1', '0', 0, 'liguang@campus.edu',     '13800138015', '2023004', '李光', 'https://picsum.photos/seed/avatar15/100/100', NOW() - INTERVAL 45 DAY, NOW()),
(16, 'xujia',     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '许佳',       '2', '0', 0, 'xujia@campus.edu',       '13800138016', '2023005', '许佳', 'https://picsum.photos/seed/avatar16/100/100', NOW() - INTERVAL 30 DAY, NOW()),
(17, 'qianlei',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '钱磊',       '1', '0', 0, 'qianlei@campus.edu',     '13800138017', '2024001', '钱磊', 'https://picsum.photos/seed/avatar17/100/100', NOW() - INTERVAL 14 DAY, NOW()),
(18, 'hanxue',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '01', '韩雪',       '2', '0', 0, 'hanxue@campus.edu',      '13800138018', '2024002', '韩雪', 'https://picsum.photos/seed/avatar18/100/100', NOW() - INTERVAL 7 DAY, NOW())
AS new
ON DUPLICATE KEY UPDATE
    `nick_name` = new.`nick_name`,
    `status` = new.`status`,
    `del_flag` = new.`del_flag`,
    `update_time` = new.`update_time`;
