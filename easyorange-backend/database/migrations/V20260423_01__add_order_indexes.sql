-- =====================================================
-- Phase 5: 查询优化 - Order 表索引添加
-- 日期：2026-04-23
-- 说明：为订单表添加查询优化索引
-- =====================================================

-- 1. 买家 ID 索引 - 加速查询买家订单列表
CREATE INDEX IF NOT EXISTS idx_order_buyer_id ON eo_order(buyer_id);

-- 2. 卖家 ID 索引 - 加速查询卖家订单列表
CREATE INDEX IF NOT EXISTS idx_order_seller_id ON eo_order(seller_id);

-- 3. 订单状态索引 - 加速按状态筛选订单
CREATE INDEX IF NOT EXISTS idx_order_status ON eo_order(status);

-- 4. 创建时间索引 - 加速按时间排序和筛选
CREATE INDEX IF NOT EXISTS idx_order_create_time ON eo_order(create_time);

-- 5. 复合索引：买家 + 状态 - 优化买家按状态查询订单
CREATE INDEX IF NOT EXISTS idx_order_buyer_status ON eo_order(buyer_id, status);

-- 6. 复合索引：卖家 + 状态 - 优化卖家按状态查询订单
CREATE INDEX IF NOT EXISTS idx_order_seller_status ON eo_order(seller_id, status);

-- 7. 复合索引：买家 + 创建时间 - 优化买家订单时间排序
CREATE INDEX IF NOT EXISTS idx_order_buyer_create_time ON eo_order(buyer_id, create_time DESC);

-- 8. 产品 ID 索引 - 加速查询某个商品的所有订单
CREATE INDEX IF NOT EXISTS idx_order_product_id ON eo_order(product_id);

-- 9. 订单号索引 - 加速订单号查询（通常是唯一索引）
CREATE INDEX IF NOT EXISTS idx_order_order_no ON eo_order(order_no);

-- 10. 支付状态索引 - 加速按支付状态筛选
CREATE INDEX IF NOT EXISTS idx_order_payment_status ON eo_order(payment_status);

-- 11. 复合索引：状态 + 创建时间 - 优化按状态和时间筛选
CREATE INDEX IF NOT EXISTS idx_order_status_create_time ON eo_order(status, create_time DESC);

COMMENT ON INDEX idx_order_buyer_id IS '买家 ID 索引 - 查询买家订单列表';
COMMENT ON INDEX idx_order_seller_id IS '卖家 ID 索引 - 查询卖家订单列表';
COMMENT ON INDEX idx_order_status IS '订单状态索引 - 按状态筛选';
COMMENT ON INDEX idx_order_create_time IS '创建时间索引 - 时间排序';
COMMENT ON INDEX idx_order_buyer_status IS '买家 + 状态复合索引 - 买家按状态查订单';
COMMENT ON INDEX idx_order_seller_status IS '卖家 + 状态复合索引 - 卖家按状态查订单';
COMMENT ON INDEX idx_order_buyer_create_time IS '买家 + 时间复合索引 - 买家订单时间排序';
COMMENT ON INDEX idx_order_product_id IS '产品 ID 索引 - 查询商品的所有订单';
COMMENT ON INDEX idx_order_order_no IS '订单号索引 - 订单号查询';
COMMENT ON INDEX idx_order_payment_status IS '支付状态索引 - 按支付状态筛选';
COMMENT ON INDEX idx_order_status_create_time IS '状态 + 时间复合索引 - 按状态和时间筛选';
