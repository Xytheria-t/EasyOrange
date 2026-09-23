-- =====================================================================
-- IT 独立库（docker-entrypoint-initdb.d，仅数据卷首次初始化时执行）
-- 集成测试残留与 dev 演示库物理隔离（技术债务 TD-001）：
-- application-it.yaml 连本库，dev 应用连 MYSQL_DATABASE 主库，互不可见。
-- 存量卷不会重跑本脚本 —— scripts/db-reset.sh 用 root 幂等补执行同一段 SQL。
-- 用户名须与 EASYORANGE_DB_USERNAME（.env / compose 插值）一致，否则 IT 认证失败。
-- =====================================================================
CREATE DATABASE IF NOT EXISTS `easyorange_it` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON `easyorange_it`.* TO 'easyorange_app'@'%';
FLUSH PRIVILEGES;
