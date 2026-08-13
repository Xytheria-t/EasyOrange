-- 移除 eo_audit_log.oper_location（预留列，从未落值，与 AuditLog 实体字段同步删除）
ALTER TABLE `eo_audit_log` DROP COLUMN `oper_location`;
