package com.cartethyia.easyorange.message.domain.port;

import com.cartethyia.easyorange.message.domain.valueobject.MessageUserProfile;
import java.util.Collection;
import java.util.Map;

/**
 * 消息侧的用户展示档案端口 — 消息列表与会话头需要 id / username / avatar 三字段；
 * 与订单侧只查用户名的 {@code OrderUserNamesPort} 是两个契约，不合并。
 */
public interface MessageUserProfilePort {

    Map<String, MessageUserProfile> getProfiles(Collection<String> userIds);
}
