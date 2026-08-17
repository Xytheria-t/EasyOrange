-- 收藏降价提醒：记录收藏时的价格快照，商品更新事件到达后比对快照判定降价。
-- 快照语义：初始为收藏时价格；每次发出降价通知后更新为最新价格（只提醒"再创新低"）。
ALTER TABLE `eo_favorite`
    ADD COLUMN `price_snapshot` DECIMAL(10,2) NULL COMMENT '价格快照（收藏时价格，降价通知后更新为最近通知价）' AFTER `product_id`;

-- 存量收藏回填现价，避免迁移后首次商品更新事件误报降价
UPDATE `eo_favorite` f
    JOIN `eo_product` p ON p.id = f.product_id AND p.del_flag = 0
SET f.price_snapshot = p.price
WHERE f.del_flag = 0 AND f.price_snapshot IS NULL;
