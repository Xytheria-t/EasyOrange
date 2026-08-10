-- 订单退款独立归因列：refund_reason/refund_time 专职退款，cancel_reason/cancel_time 仅承载取消
ALTER TABLE `eo_order`
    ADD COLUMN `refund_reason` VARCHAR(500) DEFAULT NULL COMMENT '退款原因' AFTER `cancel_time`,
    ADD COLUMN `refund_time`   DATETIME      DEFAULT NULL COMMENT '退款时间' AFTER `refund_reason`;

-- 存量 REFUNDED 数据回填：此前退款原因/时间被写入取消列，搬入退款列并清空误用的取消列
UPDATE `eo_order`
SET `refund_reason` = `cancel_reason`,
    `refund_time`   = `cancel_time`,
    `cancel_reason` = NULL,
    `cancel_time`   = NULL
WHERE `status` = 'REFUNDED' AND `del_flag` = 0;