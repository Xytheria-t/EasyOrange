package com.cartethyia.easyorange.order.domain.port;

import java.util.Map;
import java.util.Set;

/**
 * 订单侧的用户名查询端口 — 订单视图只需要 userName，故只返回 id → username；跨模块经 ACL 适配器实现。
 * 与消息侧的 {@code MessageUserProfilePort}（id / username / avatar 三字段）不是同一个契约，不合并。
 */
public interface OrderUserNamesPort {

    /** 批量查询用户名（userId → username），缺失的 ID 不返回。 */
    Map<String, String> findUsernames(Set<String> userIds);
}
