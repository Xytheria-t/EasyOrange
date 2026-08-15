package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.RecentUser;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserAuth;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserStats;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminRecentUser;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserDetail;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserInfo;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserPage;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 用户查询/操作适配器 — 纯翻译层。
 * <p>
 * 实现 {@link AdminUserPort}，全部查询逻辑与业务规则（状态解析、解锁前置、最后一个管理员保护等）由
 * user 模块的 {@link AdminUserManagementPort} 实现，本类仅做两侧记录的等价映射。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminUserAdapter implements AdminUserPort {

    private final AdminUserManagementPort adminUserManagementPort;

    @Override
    public UserInfo getUserInfo(String userId) {
        AdminUserInfo info = adminUserManagementPort.getInfo(userId);
        return info != null ? toUserInfo(info) : null;
    }

    @Override
    public Map<String, UserInfo> getUserInfos(List<String> userIds) {
        return adminUserManagementPort.getInfos(userIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toUserInfo(e.getValue())));
    }

    @Override
    public UserDetail getUserDetail(String userId) {
        AdminUserDetail detail = adminUserManagementPort.getDetail(userId);
        return detail != null ? toUserDetail(detail) : null;
    }

    @Override
    public UserQueryResult queryUsers(UserQueryCondition condition) {
        AdminUserQuery query = new AdminUserQuery(
                condition.keyword(),
                condition.userType(),
                condition.status(),
                condition.startTime(),
                condition.endTime(),
                condition.pageNum(),
                condition.pageSize());
        AdminUserPage page = adminUserManagementPort.query(query);
        return new UserQueryResult(
                page.records().stream().map(this::toUserDetail).toList(),
                page.total(),
                page.pageNum(),
                page.pageSize());
    }

    @Override
    public UserAuth getUserAuth(String userId) {
        var auth = adminUserManagementPort.getAuth(userId);
        return auth != null ? new UserAuth(auth.userType(), auth.status()) : null;
    }

    @Override
    public void updateUserStatus(String userId, String statusCode) {
        adminUserManagementPort.updateStatus(userId, statusCode);
    }

    @Override
    public void unlockUser(String userId) {
        adminUserManagementPort.unlock(userId);
    }

    @Override
    public void setUserType(String userId, String typeCode) {
        adminUserManagementPort.setUserType(userId, typeCode);
    }

    @Override
    public void setPassword(String userId, String encodedPassword) {
        adminUserManagementPort.setPassword(userId, encodedPassword);
    }

    @Override
    public UserStats getUserStats() {
        var stats = adminUserManagementPort.getStats();
        return new UserStats(stats.totalUsers(), stats.todayNewUsers());
    }

    @Override
    public List<RecentUser> getRecentUsers(int limit) {
        return adminUserManagementPort.getRecentUsers(limit).stream()
                .map(this::toRecentUser)
                .toList();
    }

    private UserInfo toUserInfo(AdminUserInfo info) {
        return new UserInfo(info.id(), info.username(), info.nickName(), info.avatar(), info.phone());
    }

    private UserDetail toUserDetail(AdminUserDetail detail) {
        return new UserDetail(
                detail.id(),
                detail.username(),
                detail.nickName(),
                detail.avatar(),
                detail.email(),
                detail.phone(),
                detail.studentId(),
                detail.realName(),
                detail.userType(),
                detail.userTypeDesc(),
                detail.status(),
                detail.statusDesc(),
                detail.loginIp(),
                detail.loginDate(),
                detail.createTime(),
                detail.updateTime());
    }

    private RecentUser toRecentUser(AdminRecentUser user) {
        return new RecentUser(
                user.id(),
                user.username(),
                user.nickName(),
                user.avatar(),
                user.email(),
                user.phone(),
                user.userType(),
                user.userTypeDesc(),
                user.status(),
                user.statusDesc(),
                user.createTime());
    }
}
