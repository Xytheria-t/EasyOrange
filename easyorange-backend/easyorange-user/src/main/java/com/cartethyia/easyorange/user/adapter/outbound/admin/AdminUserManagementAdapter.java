package com.cartethyia.easyorange.user.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AdminUserManagementService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * {@link AdminUserManagementPort} 实现 — 管理端用户读写。
 * <p>
 * 读路径经 {@link UserMapper} 投影（分页查询/统计/批量信息）；写路径负责枚举 code 契约解析后委托
 * {@link AdminUserManagementService}（业务规则 + 聚合根迁移），再经 {@link UserRepository} 持久化。
 */
@Component
@RequiredArgsConstructor
public class AdminUserManagementAdapter implements AdminUserManagementPort {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final AdminUserManagementService adminUserManagementService;

    @Override
    public AdminUserInfo getInfo(String userId) {
        UserDO user = userMapper.selectById(userId);
        return user != null ? toInfo(user) : null;
    }

    @Override
    public Map<String, AdminUserInfo> getInfos(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(UserDO::getId, this::toInfo, (a, b) -> a));
    }

    @Override
    public AdminUserDetail getDetail(String userId) {
        UserDO user = findActive(userId);
        return user != null ? toDetail(user) : null;
    }

    @Override
    public AdminUserPage query(AdminUserQuery query) {
        int pageNum = query.pageNum() != null ? query.pageNum() : 1;
        int pageSize = query.pageSize() != null ? query.pageSize() : 20;

        var wrapper = ChainWrappers.lambdaQueryChain(userMapper).eq(UserDO::getDelFlag, 0);

        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(UserDO::getUsername, query.keyword())
                    .or()
                    .like(UserDO::getNickName, query.keyword())
                    .or()
                    .like(UserDO::getEmail, query.keyword())
                    .or()
                    .like(UserDO::getPhone, query.keyword()));
        }

        if (StringUtils.hasText(query.userType())) {
            wrapper.eq(UserDO::getUserType, parseUserType(query.userType()));
        }

        if (StringUtils.hasText(query.status())) {
            wrapper.eq(UserDO::getStatus, parseStatus(query.status()));
        }

        if (query.startTime() != null) {
            wrapper.ge(UserDO::getCreateTime, query.startTime());
        }

        if (query.endTime() != null) {
            wrapper.le(UserDO::getCreateTime, query.endTime());
        }

        wrapper.orderByDesc(UserDO::getCreateTime);

        Page<UserDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<AdminUserDetail> records =
                page.getRecords().stream().map(this::toDetail).toList();
        return new AdminUserPage(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public AdminUserAuth getAuth(String userId) {
        UserDO user = findActive(userId);
        if (user == null) {
            return null;
        }
        return new AdminUserAuth(
                user.getUserType() != null ? user.getUserType().getCode() : null,
                user.getStatus() != null ? user.getStatus().getCode() : null);
    }

    @Override
    public void updateStatus(String userId, String statusCode) {
        userRepository.update(adminUserManagementService.updateStatus(userId, parseStatus(statusCode)));
    }

    @Override
    public void unlock(String userId) {
        userRepository.update(adminUserManagementService.unlock(userId));
    }

    @Override
    public void setUserType(String userId, String typeCode) {
        userRepository.update(adminUserManagementService.changeUserType(userId, parseUserType(typeCode)));
    }

    @Override
    public void setPassword(String userId, String encodedPassword) {
        userRepository.update(adminUserManagementService.resetPassword(userId, encodedPassword));
    }

    @Override
    public AdminUserStats getStats() {
        long totalUsers = ChainWrappers.lambdaQueryChain(userMapper)
                .eq(UserDO::getDelFlag, 0)
                .count();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNewUsers = ChainWrappers.lambdaQueryChain(userMapper)
                .eq(UserDO::getDelFlag, 0)
                .ge(UserDO::getCreateTime, todayStart)
                .count();

        return new AdminUserStats(totalUsers, todayNewUsers);
    }

    @Override
    public List<AdminRecentUser> getRecentUsers(int limit) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        return ChainWrappers.lambdaQueryChain(userMapper)
                .eq(UserDO::getDelFlag, 0)
                .ge(UserDO::getCreateTime, todayStart)
                .orderByDesc(UserDO::getCreateTime)
                .page(new Page<>(1, limit))
                .getRecords()
                .stream()
                .map(this::toRecentUser)
                .toList();
    }

    private AdminUserInfo toInfo(UserDO user) {
        return new AdminUserInfo(
                user.getId(), user.getUsername(), user.getNickName(), user.getAvatar(), user.getPhone());
    }

    private AdminUserDetail toDetail(UserDO user) {
        return new AdminUserDetail(
                user.getId(),
                user.getUsername(),
                user.getNickName(),
                user.getAvatar(),
                user.getEmail(),
                user.getPhone(),
                user.getStudentId(),
                user.getRealName(),
                user.getUserType() != null ? user.getUserType().getCode() : null,
                user.getUserType() != null ? user.getUserType().getDescription() : null,
                user.getStatus() != null ? user.getStatus().getCode() : null,
                user.getStatus() != null ? user.getStatus().getDescription() : null,
                user.getLoginIp(),
                user.getLoginDate(),
                user.getCreateTime(),
                user.getUpdateTime());
    }

    private AdminRecentUser toRecentUser(UserDO user) {
        AdminUserDetail detail = toDetail(user);
        return new AdminRecentUser(
                detail.id(),
                detail.username(),
                detail.nickName(),
                detail.avatar(),
                detail.email(),
                detail.phone(),
                detail.userType(),
                detail.userTypeDesc(),
                detail.status(),
                detail.statusDesc(),
                detail.createTime());
    }

    private UserDO findActive(String userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null || user.getDelFlag() != 0) {
            return null;
        }
        return user;
    }

    /** 前端契约：状态以枚举 code（'NORMAL'/'DISABLED'/'LOCKED'）传输，非法值抛出业务异常。 */
    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.fromCode(status);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的用户状态");
        }
    }

    /** 前端契约：角色以枚举 code（'00'/'01'/'02'）传输，非法值抛出业务异常。 */
    private UserType parseUserType(String typeCode) {
        try {
            return UserType.fromCode(typeCode);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的用户角色");
        }
    }
}
