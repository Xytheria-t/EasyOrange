-- =====================================================
-- Phase 5: 查询优化 - Product 表索引添加
-- 日期：2026-04-23
-- 说明：为商品表添加查询优化索引
-- =====================================================

-- 1. 分类 ID 索引 - 加速查询分类下的商品
CREATE INDEX IF NOT EXISTS idx_product_category_id ON product(category_id);

-- 2. 用户 ID 索引 - 加速查询用户的商品
CREATE INDEX IF NOT EXISTS idx_product_user_id ON product(user_id);

-- 3. 商品状态索引 - 加速按状态筛选商品
CREATE INDEX IF NOT EXISTS idx_product_status ON product(status);

-- 4. 商品名称索引 - 支持商品名称模糊查询
CREATE INDEX IF NOT EXISTS idx_product_name ON product(name);

-- 5. 创建时间索引 - 加速按时间排序（最新商品）
CREATE INDEX IF NOT EXISTS idx_product_create_time ON product(create_time DESC);

-- 6. 价格索引 - 加速按价格排序和筛选
CREATE INDEX IF NOT EXISTS idx_product_price ON product(price);

-- 7. 浏览量索引 - 加速按浏览量排序（热门商品）
CREATE INDEX IF NOT EXISTS idx_product_view_count ON product(view_count DESC);

-- 8. 复合索引：分类 + 状态 - 优化分类按状态查询商品
CREATE INDEX IF NOT EXISTS idx_product_category_status ON product(category_id, status);

-- 9. 复合索引：用户 + 状态 - 优化用户按状态查商品
CREATE INDEX IF NOT EXISTS idx_product_user_status ON product(user_id, status);

-- 10. 复合索引：分类 + 创建时间 - 优化分类商品时间排序
CREATE INDEX IF NOT EXISTS idx_product_category_create_time ON product(category_id, create_time DESC);

-- 11. 复合索引：状态 + 创建时间 - 优化按状态和时间筛选
CREATE INDEX IF NOT EXISTS idx_product_status_create_time ON product(status, create_time DESC);

-- 12. 全文索引（如果支持）- 商品名称全文搜索
-- 注意：MySQL 5.6+ 支持全文索引，如不需要可注释
-- CREATE FULLTEXT INDEX IF NOT EXISTS idx_product_name_fulltext ON product(name);

COMMENT ON INDEX idx_product_category_id IS '分类 ID 索引 - 查询分类下的商品';
COMMENT ON INDEX idx_product_user_id IS '用户 ID 索引 - 查询用户的商品';
COMMENT ON INDEX idx_product_status IS '商品状态索引 - 按状态筛选';
COMMENT ON INDEX idx_product_name IS '商品名称索引 - 支持模糊查询';
COMMENT ON INDEX idx_product_create_time IS '创建时间索引 - 最新商品排序';
COMMENT ON INDEX idx_product_price IS '价格索引 - 价格排序和筛选';
COMMENT ON INDEX idx_product_view_count IS '浏览量索引 - 热门商品排序';
COMMENT ON INDEX idx_product_category_status IS '分类 + 状态复合索引 - 分类按状态查商品';
COMMENT ON INDEX idx_product_user_status IS '用户 + 状态复合索引 - 用户按状态查商品';
COMMENT ON INDEX idx_product_category_create_time IS '分类 + 时间复合索引 - 分类商品时间排序';
COMMENT ON INDEX idx_product_status_create_time IS '状态 + 时间复合索引 - 按状态和时间筛选';
