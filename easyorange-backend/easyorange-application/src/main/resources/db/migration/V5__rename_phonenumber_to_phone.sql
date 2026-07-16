-- ===================================================================
-- V5: 统一列名 phonenumber → phone
-- 职责: 将 eo_user.phonenumber 重命名为 phone，与 Java 实体字段名一致
-- 原因: UserEntity 中的字段名为 phone，之前通过 @TableField("phonenumber") 映射。
--       为消除这一映射不一致，改列名以匹配字段名。
-- Database: MySQL 8.0
-- 2026-07-16
-- ===================================================================

-- 注：索引 uk_eo_user_phone 使用 phonenumber 列，CHANGE COLUMN 会自动更新索引引用为新列名
ALTER TABLE eo_user CHANGE COLUMN phonenumber phone VARCHAR(20) DEFAULT NULL COMMENT '手机号码';
