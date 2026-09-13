-- V4：库存流水表 — 库存变更的单一事实来源
-- 每次库存变更（初始化/下单扣减/取消退款恢复/人工调整）在同一事务内落一条流水，
-- 唯一索引 (change_type, biz_id, product_id) 承载幂等：同一订单对同一资产的同类变更只会落账一次，
-- 重复投递（MQ 重投、DLQ 重放）撞唯一键即跳过，不再重复加减库存。
CREATE TABLE `eo_stock_ledger` (
    `id` VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `product_id` VARCHAR(36) NOT NULL COMMENT '资产 ID',
    `biz_id` VARCHAR(36) DEFAULT NULL COMMENT '业务单号（订单 ID）；初始化/人工调整无常规业务单号，留空以脱离幂等约束',
    `change_type` VARCHAR(20) NOT NULL COMMENT '变更类型（INIT 初始化 DECREASE 下单扣减 RESTORE 取消/退款恢复 ADJUST 人工调整）',
    `delta` INT NOT NULL COMMENT '库存变化量（正数增加 / 负数减少）',
    `stock_after` INT NOT NULL COMMENT '变更后库存余额，对账基准',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(36) DEFAULT NULL COMMENT '创建者',
    `update_by` VARCHAR(36) DEFAULT NULL COMMENT '更新者',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0 正常 1 删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eo_stock_ledger_biz` (`change_type`, `biz_id`, `product_id`),
    KEY `idx_eo_stock_ledger_product_time` (`product_id`, `create_time`),
    KEY `idx_eo_stock_ledger_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存流水表';

-- 存量资产基线：以当前库存作为对账起点，避免对账任务把变更前的历史库存判成漂移
INSERT INTO `eo_stock_ledger` (`id`, `product_id`, `biz_id`, `change_type`, `delta`, `stock_after`)
SELECT UUID(), `id`, NULL, 'INIT', `stock`, `stock` FROM `eo_product`;
