package com.cartethyia.easyorange.user.domain.port;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 管理端用户读写端口 — 供组合模块经 ACL 执行用户管理操作。
 * <p>
 * 查询逻辑与业务规则（状态/角色 code 契约解析、解锁前置、最后一个管理员保护等）统一由 user 模块实现，
 * 跨模块禁止直接依赖 {@code UserDO}/{@code UserMapper}，统一走本端口。
 */
public interface AdminUserManagementPort {

    /**
     * 查询用户基础信息，不存在时返回 null。
     */
    AdminUserInfo getInfo(String userId);

    /**
     * 批量查询用户基础信息（缺失的 ID 不返回）。
     */
    Map<String, AdminUserInfo> getInfos(Collection<String> userIds);

    /**
     * 查询用户详情，不存在或已删除时返回 null。
     */
    AdminUserDetail getDetail(String userId);

    /**
     * 分页条件查询（keyword 匹配用户名/昵称/邮箱/手机号，userType/status 为枚举 code）。
     */
    AdminUserPage query(AdminUserQuery query);

    /**
     * 查询用户认证信息（角色/状态 code），不存在或已删除时返回 null。
     */
    AdminUserAuth getAuth(String userId);

    /**
     * 更新用户状态（statusCode 为 'NORMAL'/'DISABLED'/'LOCKED'），非法值抛出 BusinessException。
     */
    void updateStatus(String userId, String statusCode);

    /**
     * 解锁/启用用户：仅当状态为 LOCKED 或 DISABLED 时置为 NORMAL，否则抛出 BusinessException。
     */
    void unlock(String userId);

    /**
     * 变更用户角色（typeCode 为 '00'/'01'/'02'），非法值/已是该角色/最后一个管理员被降级时抛出 BusinessException。
     */
    void setUserType(String userId, String typeCode);

    /**
     * 更新用户密码（encodedPassword 为已编码密文）。
     */
    void setPassword(String userId, String encodedPassword);

    /**
     * 用户总数与今日新增（均不含已删除）。
     */
    AdminUserStats getStats();

    /**
     * 查询今日注册用户（按注册时间倒序，limit 条）。
     */
    List<AdminRecentUser> getRecentUsers(int limit);

    /**
     * 用户基础信息
     */
    record AdminUserInfo(String id, String username, String nickName, String avatar, String phone) {}

    /**
     * 用户详情
     */
    record AdminUserDetail(
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
    record AdminUserQuery(
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
    record AdminUserPage(List<AdminUserDetail> records, long total, int pageNum, int pageSize) {}

    /**
     * 用户认证信息 — userType/status 为枚举 code
     */
    record AdminUserAuth(String userType, String status) {}

    /**
     * 用户统计
     */
    record AdminUserStats(long totalUsers, long todayNewUsers) {}

    /**
     * 最近注册用户
     */
    record AdminRecentUser(
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
