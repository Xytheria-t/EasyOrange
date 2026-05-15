ALTER TABLE `eo_product_report`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态（0 待处理 1 处理中 2 已解决 3 已驳回）';

ALTER TABLE `eo_product_report`
    DROP CHECK `chk_eo_product_report_status`;

ALTER TABLE `eo_product_report`
    ADD CONSTRAINT `chk_eo_product_report_status` CHECK (`status` IN (0, 1, 2, 3));

ALTER TABLE `eo_product_report`
    ADD COLUMN `reason_type` TINYINT DEFAULT NULL COMMENT '举报类型（1 虚假信息 2 侵权投诉 3 违规内容 4 其他）' AFTER `reason`;
