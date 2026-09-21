-- 清理已下线能力留下的表与列。
--
-- 背景：收藏 / 商品评价 / 消息归档三条旁挂能力已从代码中删除（模块、REST 面、前端入口全部下线），
-- 对应的表与 eo_ai_call_log 上「离线评审」与「调用主体」两组列随之失去写入方与读取方。
-- 这些表在演示数据量下从未接近保留策略门槛，留着只会让「表清单 ↔ 代码」对不上。
--
-- 说明：删列用 DROP COLUMN（MySQL 8 会同步删除列上的索引），不做数据搬迁——
-- 本仓库未上线，dev 数据可由 R__insert_dev_test_data.sql 重建。

DROP TABLE IF EXISTS `eo_favorite`;
DROP TABLE IF EXISTS `eo_product_review`;
DROP TABLE IF EXISTS `eo_message_archive`;

ALTER TABLE `eo_ai_call_log`
    DROP COLUMN `subject_id`,
    DROP COLUMN `judge_score`,
    DROP COLUMN `judge_comment`;
