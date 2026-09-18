-- 商品侧记录 AI 建议售价
-- 背景：智能估值发生在商品创建之前，调用日志的 subject_id 那时还没有值，商品也没有字段可落
-- 「AI 当时建议多少」。所以采纳率只能由商品侧自己记：拍照识别把建议价随创建请求一起提交，
-- 与资产方最终成交价存在同一行里，发布后直接可比。
-- 这是全项目唯一不依赖 LLM 评 LLM 的质量指标（hit@5 有语料免责、Judge 均分有自评偏差）。
ALTER TABLE `eo_product`
    ADD COLUMN `ai_suggested_price` DECIMAL(10, 2) NULL COMMENT 'AI 建议售价（拍照识别给出，资产方未采到则 NULL）' AFTER `original_price`;
