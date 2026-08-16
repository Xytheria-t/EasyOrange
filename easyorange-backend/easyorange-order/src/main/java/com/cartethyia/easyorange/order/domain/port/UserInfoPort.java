package com.cartethyia.easyorange.order.domain.port;

import java.util.Map;
import java.util.Set;

/**
 * 用户公开信息查询端口 — 供订单视图填充买卖家用户名，跨模块经 ACL 适配器实现。
 */
public interface UserInfoPort {

    /** 批量查询用户名（userId → username），缺失的 ID 不返回。 */
    Map<String, String> findUsernames(Set<String> userIds);
}
