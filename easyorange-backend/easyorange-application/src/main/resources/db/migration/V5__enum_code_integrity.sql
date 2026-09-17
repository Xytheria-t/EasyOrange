-- 枚举码完整性：让"DB 默认值"与"脏数据"都不再可能产生非法枚举码。
-- 背景：eo_message.type 默认值 0 不是合法 MessageType 码（1 系统 2 聊天 3 订单 4 支付 5 活动），
-- 读侧枚举转换遇到非法码直接抛异常 → 消息列表接口整体 500。

-- 1) 兜平存量非法值（0 = 历史默认值，语义上等价于系统消息）
UPDATE `eo_message` SET `type` = 1 WHERE `type` NOT IN (1, 2, 3, 4, 5);
UPDATE `eo_message_archive` SET `type` = 1 WHERE `type` NOT IN (1, 2, 3, 4, 5);

-- 2) 去掉非法默认值：type 必须显式赋值，漏传立即报错，而不是静默写入一个非法码
ALTER TABLE `eo_message`
    MODIFY COLUMN `type` TINYINT NOT NULL COMMENT '消息类型（1 系统 2 聊天 3 订单 4 支付 5 活动）';

ALTER TABLE `eo_message_archive`
    MODIFY COLUMN `type` TINYINT NOT NULL COMMENT '消息类型（1 系统 2 聊天 3 订单 4 支付 5 活动）';

-- 3) DB 级约束：非法码写不进来（与既有 chk_eo_message_is_read / chk_eo_user_sex 等同一约定；
--    枚举扩码时须同步放宽本约束）
ALTER TABLE `eo_message` ADD CONSTRAINT `chk_eo_message_type` CHECK (`type` IN (1, 2, 3, 4, 5));

ALTER TABLE `eo_message_archive` ADD CONSTRAINT `chk_eo_message_archive_type` CHECK (`type` IN (1, 2, 3, 4, 5));

-- 4) eo_user.sex 默认值 0 在 Sex 枚举里是 FEMALE（女），注册不带性别会被静默落成"女"，
--    与"未设置"语义冲突；默认改为 2（UNKNOWN 未知）并订正列注释（原注释 0/2 语义写反）。
--    存量行不动：无法区分"显式选女"与"被默认值落成的女"。
ALTER TABLE `eo_user`
    MODIFY COLUMN `sex` TINYINT NOT NULL DEFAULT 2 COMMENT '用户性别（0 女 1 男 2 未知）';
