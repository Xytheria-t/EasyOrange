-- ===================================================================
-- 紧急密码重置脚本
-- 用途：修复 BCrypt 密码格式问题并重置所有测试用户密码
-- 默认密码：Password123 (BCrypt 加密后)
-- ===================================================================

-- 重置所有普通用户密码为 "Password123"
-- BCrypt 加密强度: 10
-- 加密后长度: 60 字符
UPDATE `eo_user` SET `password` = '$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK'
WHERE `del_flag` = 0;

-- 清除被锁定的用户状态（status = 2 -> 0）
UPDATE `eo_user` SET `status` = 0, `update_time` = NOW()
WHERE `status` = 2 AND `del_flag` = 0;
