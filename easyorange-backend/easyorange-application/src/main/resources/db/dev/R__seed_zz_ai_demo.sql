-- ===================================================================
-- AI 建议采纳率演示数据（拍照识别的建议快照）
-- 目的: GET /api/admin/ai/listing-adoption 在 dev 库上直接有数，不必先手工走一遍发布流程。
--       报表只统计 ai_suggestion 非空的行（没给出建议的商品进分母会把采纳率稀释成无意义的数），
--       不补数据则报表恒为 0 样本。
-- 为什么单独一个文件: 快照要读 eo_category 的类目名与商品行，而 Flyway 按**名称字典序**执行可重复迁移 ——
--   本文件以 zz 前缀排在最后，此时商品（R__seed_dev_test_data）与类目（R__seed_categories）都已入库。
-- 造数思路: 快照存的是**建议原文**（六字段 JSON），采纳与否由报表 SQL 与商品最终值比对得出 ——
--   所以只要控制「建议值与最终值差在哪几个字段」，字段级采纳率就自然拉开：
--   ①A 全字段一致（全采纳）；①B 只改价 ≤10%；①C 改价 >30% 且标题被资产方重写（文案不被采信）。
-- 建议值是构造值，不代表模型真实输出；三条 UPDATE 幂等且只命中种子商品
-- （比较值加引号的原因：id 是 VARCHAR，裸数字比较会在脏库上报 1292；
--   应用内真实发布的商品是 UUID v7 主键，不会落在这些 ID 上）。
-- Database: MySQL 8.0
-- ===================================================================

START TRANSACTION;

-- ①A 全字段采纳：快照与最终值六项全等
UPDATE `eo_product` p
SET p.ai_suggestion = JSON_OBJECT(
        'title', p.name,
        'description', (SELECT d.description FROM `eo_product_detail` d WHERE d.product_id = p.id),
        'price', p.price,
        'categoryName', (SELECT c.name FROM `eo_category` c WHERE c.id = p.category_id AND c.del_flag = 0),
        'conditionLevel', p.condition_level,
        'location', COALESCE(p.location, ''))
WHERE p.id IN ('1', '5', '11', '15', '23', '33', '50', '65');

-- ①B 采纳但改价 ≤10%：只有价格与建议不同
UPDATE `eo_product` p
SET p.ai_suggestion = JSON_OBJECT(
        'title', p.name,
        'description', (SELECT d.description FROM `eo_product_detail` d WHERE d.product_id = p.id),
        'price', ROUND(p.price * 1.08, 2),
        'categoryName', (SELECT c.name FROM `eo_category` c WHERE c.id = p.category_id AND c.del_flag = 0),
        'conditionLevel', p.condition_level,
        'location', COALESCE(p.location, ''))
WHERE p.id IN ('3', '8', '16', '24', '30', '37', '58', '66');

-- ①C 偏离 >30%：价格大改，且标题被资产方重写
UPDATE `eo_product` p
SET p.ai_suggestion = JSON_OBJECT(
        'title', CONCAT('九成新 ', p.name),
        'description', (SELECT d.description FROM `eo_product_detail` d WHERE d.product_id = p.id),
        'price', ROUND(p.price * 1.5, 2),
        'categoryName', (SELECT c.name FROM `eo_category` c WHERE c.id = p.category_id AND c.del_flag = 0),
        'conditionLevel', p.condition_level,
        'location', COALESCE(p.location, ''))
WHERE p.id IN ('2', '9', '12', '26', '36', '54', '64', '96');

COMMIT;
