-- 通知点击要跳到哪张页面取决于 business_id 是商品还是订单，此前类型没落库，
-- 前端只能按标题中文猜，订单类通知一律跳 /products/{order_id} 落到 404。
-- 补 TINYINT 列承载业务对象类型：MessageBizType 是 0 起点枚举，DEFAULT 0 即 NONE（合法码），
-- 存量行按 NONE 收敛——不追溯改写，读侧对 0 一律不给跳转入口，新通知由生产者显式赋值。
-- 合法码用 CHECK 钉死，与 eo_message.type 同口径，避免非法码落到读侧枚举转换才炸。
ALTER TABLE `eo_message`
    ADD COLUMN `biz_type` TINYINT NOT NULL DEFAULT 0 COMMENT '业务对象类型（0 无 1 商品 2 订单）' AFTER `business_id`,
    ADD CONSTRAINT `chk_eo_message_biz_type` CHECK (`biz_type` IN (0, 1, 2));