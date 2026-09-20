-- ===================================================================
-- V3: AI 建议快照（字段级采纳率的数据源）
-- 职责: 拍照识别的六个建议字段（title/description/price/categoryName/conditionLevel/location）
--       原样留档，让「AI 建议 vs 资产方最终值」可以**按字段**同行比对 ——
--       采纳率因此从「只有价格」升级为六字段分布，且口径变更无需回填历史数据。
--       替代 V1 的 ai_suggested_price 单列：单列只够算价格采纳率，判定口径还被写死在报表 SQL 里。
-- Database: MySQL 8.0
-- Charset: utf8mb4
-- ===================================================================

ALTER TABLE `eo_product`
    DROP COLUMN `ai_suggested_price`,
    ADD COLUMN `ai_suggestion` JSON DEFAULT NULL COMMENT 'AI 建议快照（拍照识别产出，未识别则 NULL）';
