-- 私聊会话 ID 由双 UUID 排序推导（conv_{min}_{max}），实际 78 字符；
-- V1 按单 UUID 宽度给了 VARCHAR(36)，MySQL 严格模式下私聊消息 INSERT 必然 1406 静默失败
-- （dev 种子用户是短数字 ID，掩住了该问题）。扩到 80 留富余。
ALTER TABLE `eo_message`
    MODIFY COLUMN `conversation_id` VARCHAR(80) DEFAULT NULL COMMENT '会话 ID（conv_{min}_{max}，78 字符）';
