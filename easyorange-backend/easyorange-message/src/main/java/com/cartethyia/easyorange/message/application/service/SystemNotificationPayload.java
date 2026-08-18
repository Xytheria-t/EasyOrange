package com.cartethyia.easyorange.message.application.service;

import com.cartethyia.easyorange.message.domain.aggregate.Message;
import java.util.HashMap;
import java.util.Map;

/**
 * /queue/notification 站内信通知帧 — 与前端订阅协议约定的字段形状。
 * <p>
 * 在线即时推送（{@code MessageCommandHandler}）与离线上线补推（{@code OfflineMessageStoreService}）
 * 共用同一形状，保证两条路径的客户端解析一致。
 */
public final class SystemNotificationPayload {

    private SystemNotificationPayload() {}

    public static Map<String, Object> toMap(Message message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", message.id());
        payload.put("title", message.title() != null ? message.title() : "");
        payload.put("content", message.content() != null ? message.content() : "");
        payload.put("businessId", message.businessId() != null ? message.businessId() : "");
        payload.put("type", message.type() == null ? null : Integer.valueOf(message.type().getCode()));
        payload.put("createTime", message.createTime() != null ? message.createTime().toString() : "");
        return payload;
    }
}
