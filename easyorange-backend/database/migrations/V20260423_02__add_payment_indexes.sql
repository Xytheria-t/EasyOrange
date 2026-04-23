-- =====================================================
-- Phase 5: 查询优化 - Payment 表索引添加
-- 日期：2026-04-23
-- 说明：为支付表添加查询优化索引
-- =====================================================

-- 1. 订单 ID 索引 - 加速查询订单的支付记录
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON eo_payment(order_id);

-- 2. 用户 ID 索引 - 加速查询用户的支付记录
CREATE INDEX IF NOT EXISTS idx_payment_user_id ON eo_payment(user_id);

-- 3. 支付状态索引 - 加速按状态筛选支付
CREATE INDEX IF NOT EXISTS idx_payment_status ON eo_payment(status);

-- 4. 支付方式索引 - 加速按支付方式统计
CREATE INDEX IF NOT EXISTS idx_payment_method ON eo_payment(payment_method);

-- 5. 支付流水号索引 - 加速支付流水号查询
CREATE INDEX IF NOT EXISTS idx_payment_no ON eo_payment(payment_no);

-- 6. 交易 ID 索引 - 加速第三方交易 ID 查询
CREATE INDEX IF NOT EXISTS idx_payment_transaction_id ON eo_payment(transaction_id);

-- 7. 复合索引：订单 + 状态 - 优化按订单和状态查询
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON eo_payment(order_id, status);

-- 8. 复合索引：用户 + 状态 - 优化用户按状态查支付
CREATE INDEX IF NOT EXISTS idx_payment_user_status ON eo_payment(user_id, status);

-- 9. 复合索引：状态 + 创建时间 - 优化按状态和时间筛选
CREATE INDEX IF NOT EXISTS idx_payment_status_create_time ON eo_payment(status, create_time DESC);

-- 10. 创建时间索引 - 加速按时间排序和筛选
CREATE INDEX IF NOT EXISTS idx_payment_create_time ON eo_payment(create_time DESC);

COMMENT ON INDEX idx_payment_order_id IS '订单 ID 索引 - 查询订单的支付记录';
COMMENT ON INDEX idx_payment_user_id IS '用户 ID 索引 - 查询用户支付记录';
COMMENT ON INDEX idx_payment_status IS '支付状态索引 - 按状态筛选';
COMMENT ON INDEX idx_payment_method IS '支付方式索引 - 按支付方式统计';
COMMENT ON INDEX idx_payment_no IS '支付流水号索引 - 支付流水号查询';
COMMENT ON INDEX idx_payment_transaction_id IS '交易 ID 索引 - 第三方交易 ID 查询';
COMMENT ON INDEX idx_payment_order_status IS '订单 + 状态复合索引 - 按订单和状态查询';
COMMENT ON INDEX idx_payment_user_status IS '用户 + 状态复合索引 - 用户按状态查支付';
COMMENT ON INDEX idx_payment_status_create_time IS '状态 + 时间复合索引 - 按状态和时间筛选';
COMMENT ON INDEX idx_payment_create_time IS '创建时间索引 - 时间排序';
