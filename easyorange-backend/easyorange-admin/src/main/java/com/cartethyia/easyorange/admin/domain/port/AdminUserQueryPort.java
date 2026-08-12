package com.cartethyia.easyorange.admin.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的用户查询/操作端口
 * 用于跨模块访问用户信息与执行管理操作，遵循防腐层原则
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
     * 根据用户 ID 查询用户详情，不存在或已删除时返回 null
     */
    UserDetail getUserDetail(String userId);

    /**
     * 分页查询用户列表（带条件查询）
     */
    UserQueryResult queryUsers(UserQueryCondition condition);

    /**
     * 查询用户认证信息（角色/状态），不存在或已删除时返回 null
     */
    UserAuth getUserAuth(String userId);

    /**
     * 更新用户状态（statusCode 为 'NORMAL'/'DISABLED'/'LOCKED'），非法值抛出 BusinessException
     */
    void updateUserStatus(String userId, String statusCode);

    /**
     * 解锁/启用用户：仅当状态为 LOCKED 或 DISABLED 时置为 NORMAL，否则抛出 BusinessException
     */
    void unlockUser(String userId);

    /**
     * 变更用户角色（typeCode 为 '00'/'01'/'02'），非法值/已是该角色/最后一个管理员被变更时抛出 BusinessException
     */
    void setUserType(String userId, String typeCode);

    /**
     * 更新用户密码（encodedPassword 为已编码密文）
     */
    void setPassword(String userId, String encodedPassword);

    /**
     * 仪表板统计：用户总数与今日新增
     */
    UserStats getUserStats();

    /**
     * 查询今日注册用户（按注册时间倒序，limit 条）
     */
    List<RecentUser> getRecentUsers(int limit);

    /**
     * 用户信息
     */
    record UserInfo(String id, String username, String nickName, String avatar, String phone) {}

    /**
     * 用户详情
     */
    record UserDetail(
            String id,
            String username,
            String nickName,
            String avatar,
            String email,
            String phone,
            String studentId,
            String realName,
            String userType,
            String userTypeDesc,
            String status,
            String statusDesc,
            String loginIp,
            LocalDateTime loginDate,
            LocalDateTime createTime,
            LocalDateTime updateTime) {}

    /**
     * 用户查询条件 — userType/status 为枚举 code（'00'/'01'/'02'、'NORMAL'/'DISABLED'/'LOCKED'）
     */
    record UserQueryCondition(
            String keyword,
            String userType,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {}

    /**
     * 用户查询结果
     */
    record UserQueryResult(List<UserDetail> records, long total, int pageNum, int pageSize) {}

    /**
     * 用户认证信息 — userType/status 为枚举 code
     */
    record UserAuth(String userType, String status) {}

    /**
     * 用户统计
     */
    record UserStats(long totalUsers, long todayNewUsers) {}

    /**
     * 最近注册用户
     */
    record RecentUser(
            String id,
            String username,
            String nickName,
            String avatar,
            String email,
            String phone,
            String userType,
            String userTypeDesc,
            String status,
            String statusDesc,
            LocalDateTime createTime) {}
}
