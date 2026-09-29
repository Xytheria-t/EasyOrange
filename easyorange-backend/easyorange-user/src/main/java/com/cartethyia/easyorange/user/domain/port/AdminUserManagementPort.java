package com.cartethyia.easyorange.user.domain.port;

import java.time.LocalDate;
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

    AdminUserInfo getInfo(String userId);

    Map<String, AdminUserInfo> getInfos(Collection<String> userIds);

    AdminUserDetail getDetail(String userId);

    AdminUserPage query(AdminUserQuery query);

    AdminUserAuth getAuth(String userId);

    /** statusCode ∈ {NORMAL, DISABLED, LOCKED}，非法值抛 BusinessException。 */
    void updateStatus(String userId, String statusCode, String reason, String operatorId);

    /** 仅 LOCKED / DISABLED 可置回 NORMAL，否则抛 BusinessException。 */
    void unlock(String userId, String operatorId);

    /** typeCode ∈ {00, 01, 02}；已是该角色、或把最后一个管理员降级时抛 BusinessException。 */
    void setUserType(String userId, String typeCode, String reason, String operatorId);

    void setPassword(String userId, String encodedPassword, String reason, String operatorId);

    /** {@code yyyy-MM} → 新增用户数；聚合留在 user 模块，admin 不得自己查表。 */
    Map<String, Long> getCreateTrend(LocalDate since);

    /** 按创建时间倒序取 limit 条；昵称可空，展示层自行兜底。 */
    List<RecentUser> findRecentRegistrations(int limit);

    record RecentUser(String id, String nickName, LocalDateTime createTime) {}

    AdminUserStats getStats();

    record AdminUserInfo(String id, String username, String nickName, String avatar, String phone) {}

    /** userType / status 为枚举 code，userTypeDesc / statusDesc 为展示态文本。 */
    record AdminUserDetail(
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

    /** userType / status 为枚举 code（'00'/'01'/'02'、'NORMAL'/'DISABLED'/'LOCKED'）；startTime / endTime 为闭区间。 */
    record AdminUserQuery(
            String keyword,
            String userType,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {}

    record AdminUserPage(List<AdminUserDetail> records, long total, int pageNum, int pageSize) {}

    /** userType / status 为枚举 code。 */
    record AdminUserAuth(String userType, String status) {}

    /** 均不含已删除用户。 */
    record AdminUserStats(long totalUsers, long todayNewUsers) {}
}
