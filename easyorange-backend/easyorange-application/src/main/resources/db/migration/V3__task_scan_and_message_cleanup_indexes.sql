-- V3：定时任务与消息清理链路的索引补齐
-- eo_message：每日清理/月度归档按 create_time 范围扫描，现有复合索引首列（sender_id/receiver_id/conversation_id）均不适用
CREATE INDEX idx_eo_message_create_time ON eo_message (create_time);

-- eo_order：订单超时取消（status + create_time）与自动确认收货（status + update_time）定时批量扫描
CREATE INDEX idx_eo_order_status_create ON eo_order (status, create_time);
CREATE INDEX idx_eo_order_status_update ON eo_order (status, update_time);
