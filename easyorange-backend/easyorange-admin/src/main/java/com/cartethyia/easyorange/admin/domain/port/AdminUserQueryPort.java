package com.cartethyia.easyorange.admin.domain.port;

import java.util.List;
import java.util.Map;

/**
 * Admin 模块的用户查询端口
 * 用于跨模块查询用户信息，遵循防腐层原则
 */
public interface AdminUserQueryPort {

    /**
     * 根据用户 ID 查询用户信息
     */
    UserInfo getUserInfo(String userId);

    /**
     * 根据用户 ID 列表批量查询用户信息
     */
    Map<String, UserInfo> getUserInfos(List<String> userIds);

    /**
     * 用户信息
     */
    record UserInfo(
        String id,
        String username,
        String nickName,
        String avatar,
        String phone
    ) {}
}