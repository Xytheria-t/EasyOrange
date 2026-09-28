package com.cartethyia.easyorange.user.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.event.UserPasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.RecentUser;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AdminUserManagementService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * {@link AdminUserManagementPort} 实现 — 管理端用户读写。
 * <p>
 * 读路径经 {@link UserMapper} 投影（分页查询/统计/批量信息）；写路径负责枚举 code 契约解析后委托
 * {@link AdminUserManagementService}（业务规则 + 聚合根迁移），再经 {@link UserRepository} 持久化。
 * <p>
 * 边界：本模块是管理端唯一的用户写出口，故「改凭证的连带处置」收在此处——重置密码后统一吊销
 * 该用户全部会话并发 {@link UserPasswordChangedEvent}（source=admin），与改角色即时生效同口径，
 * 避免账号被盗场景下旧会话活到 access token 自然过期。事务按读/写分挂，与其他仓储实现一致。
 */
@Component
@RequiredArgsConstructor
public class AdminUserManagementAdapter implements AdminUserManagementPort {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final AdminUserManagementService adminUserManagementService;
    private final TokenService tokenService;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public AdminUserInfo getInfo(String userId) {
        UserDO user = userMapper.selectById(userId);
        return user != null ? toInfo(user) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, AdminUserInfo> getInfos(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(UserDO::getId, this::toInfo, (a, b) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetail getDetail(String userId) {
        UserDO user = findActive(userId);
        return user != null ? toDetail(user) : null;
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String userId, String statusCode, String reason, String operatorId) {
        userRepository.update(
                adminUserManagementService.updateStatus(userId, parseStatus(statusCode), reason, operatorId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlock(String userId, String operatorId) {
        userRepository.update(adminUserManagementService.unlock(userId, operatorId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserType(String userId, String typeCode, String reason, String operatorId) {
        userRepository.update(
                adminUserManagementService.changeUserType(userId, parseUserType(typeCode), reason, operatorId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPassword(String userId, String encodedPassword, String reason, String operatorId) {
        userRepository.update(adminUserManagementService.resetPassword(userId, encodedPassword, reason, operatorId));
        // 改凭证即刻作废既有会话，与改角色同口径：管理端重置的典型触发场景是账号被盗，
        // 不吊销则被顶替的会话仍有效到 access token 自然过期
        tokenService.revokeAllUserSessions(userId);
        domainEventPublisher.publish(new UserPasswordChangedEvent(UuidV7.generateId(), userId, "admin"));
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Map<String, Long> getCreateTrend(LocalDate since) {
        // 按「年-月」分组在 Java 侧做：用户量级下拉一张 create_time 列表在内存里分组，
        // 比 DATE_FORMAT(create_time, '%Y-%m') 走不了索引更省；也免了 SQL 方言绑定
        var rows = ChainWrappers.lambdaQueryChain(userMapper)
                .select(UserDO::getCreateTime)
                .eq(UserDO::getDelFlag, 0)
                .ge(UserDO::getCreateTime, since.atStartOfDay())
                .list();

        Map<String, Long> counts = new TreeMap<>();
        for (UserDO row : rows) {
            counts.merge(row.getCreateTime().format(MONTH_FORMAT), 1L, Long::sum);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentUser> findRecentRegistrations(int limit) {
        return ChainWrappers.lambdaQueryChain(userMapper)
                .eq(UserDO::getDelFlag, 0)
                .orderByDesc(UserDO::getCreateTime)
                .last("LIMIT " + limit)
                .list()
                .stream()
                .map(user -> new RecentUser(user.getId(), user.getNickName(), user.getCreateTime()))
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
