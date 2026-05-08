-- MySQL 字符集初始化脚本
-- 用途：确保数据库使用 utf8mb4 字符集
-- 注意：表级别的字符集由 Flyway 迁移脚本管理

SET NAMES utf8mb4;

-- 仅设置数据库级别字符集
ALTER DATABASE easyorange CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
