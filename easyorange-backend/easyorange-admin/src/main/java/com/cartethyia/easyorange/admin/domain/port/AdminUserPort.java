package com.cartethyia.easyorange.admin.domain.port;

import com.cartethyia.easyorange.admin.domain.model.RecentActivity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的用户查询/操作端口 — 跨模块访问用户信息的唯一出口。
 * <p>
 * <b>取舍</b>：写操作都收 {@code reason} + {@code operatorId}，且真的落库 ——
 * {@code reason} 进 {@code eo_user.remark}，{@code operatorId} 进 {@code audit_info.update_by}。
 * 管理端对用户下手必须能回答「谁、为什么」，接口挂一个不落库的字段等于假审计。
 * <p>
 * <b>边界</b>：所有查询都是跨模块只读，读不到即返回 null / 空集合，由 admin 侧决定对外错误码
 * （见 {@code AdminDomainException}）——端口不抛「用户不存在」这类业务异常，那是调用方的口径。
 */
public interface AdminUserPort {

    UserInfo getUserInfo(String userId);

    Map<String, UserInfo> getUserInfos(List<String> userIds);

    UserDetail getUserDetail(String userId);

    UserQueryResult queryUsers(UserQueryCondition condition);

    UserAuth getUserAuth(String userId);

    /**
     * 更新用户状态（statusCode 为 'NORMAL'/'DISABLED'/'LOCKED'），非法值抛出 BusinessException
     */
    void updateUserStatus(String userId, String statusCode, String reason, String operatorId);

    /**
     * 解锁/启用用户：仅当状态为 LOCKED 或 DISABLED 时置为 NORMAL，否则抛出 BusinessException
     */
    void unlockUser(String userId, String operatorId);

    /**
     * 变更用户角色（typeCode 为 '00'/'01'/'02'），非法值/已是该角色/最后一个管理员被变更时抛出 BusinessException
     */
    void setUserType(String userId, String typeCode, String reason, String operatorId);

    /**
     * 更新用户密码（encodedPassword 为已编码密文）
     */
    void setPassword(String userId, String encodedPassword, String reason, String operatorId);

    UserStats getUserStats();

    /**
     * 注册趋势：{@code yyyy-MM} → 新增用户数，键按创建时间升序
     */
    Map<String, Long> getCreateTrend(LocalDate since);

    /**
     * 最近注册用户（按创建时间倒序取 limit 条）
     */
    List<RecentActivity> findRecentRegistrations(int limit);

    record UserInfo(String id, String username, String nickName, String avatar, String phone) {}

    record UserDetail(
            String id,
            String username,
            String nickName,
            String avatar,
            String email,
            String phone,
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

    record UserQueryResult(List<UserDetail> records, long total, int pageNum, int pageSize) {}

    record UserAuth(String userType, String status) {}

    record UserStats(long totalUsers, long todayNewUsers) {}
}
