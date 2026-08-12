package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Admin 用户查询/操作适配器
 * 实现 AdminUserQueryPort，通过 User Mapper 访问用户数据并转换为 Admin 模块需要的格式
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminUserQueryAdapter implements AdminUserQueryPort {

    private final UserMapper userMapper;

    @Override
    public UserInfo getUserInfo(String userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return toUserInfo(user);
    }

    @Override
    public Map<String, UserInfo> getUserInfos(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserDO> users = userMapper.selectByIds(userIds);
        return users.stream().collect(Collectors.toMap(UserDO::getId, this::toUserInfo, (a, b) -> a));
    }

    @Override
    public UserDetail getUserDetail(String userId) {
        UserDO user = findUser(userId);
        return user != null ? toUserDetail(user) : null;
    }

    @Override
    public UserQueryResult queryUsers(UserQueryCondition condition) {
        int pageNum = condition.pageNum() != null ? condition.pageNum() : 1;
        int pageSize = condition.pageSize() != null ? condition.pageSize() : 20;

        var wrapper = ChainWrappers.lambdaQueryChain(userMapper).eq(UserDO::getDelFlag, 0);

        if (StringUtils.hasText(condition.keyword())) {
            wrapper.and(w -> w.like(UserDO::getUsername, condition.keyword())
                    .or()
                    .like(UserDO::getNickName, condition.keyword())
                    .or()
                    .like(UserDO::getEmail, condition.keyword())
                    .or()
                    .like(UserDO::getPhone, condition.keyword()));
        }

        if (StringUtils.hasText(condition.userType())) {
            wrapper.eq(UserDO::getUserType, UserType.fromCode(condition.userType()));
        }

        if (StringUtils.hasText(condition.status())) {
            wrapper.eq(UserDO::getStatus, parseStatus(condition.status()));
        }

        if (condition.startTime() != null) {
            wrapper.ge(UserDO::getCreateTime, condition.startTime());
        }

        if (condition.endTime() != null) {
            wrapper.le(UserDO::getCreateTime, condition.endTime());
        }

        wrapper.orderByDesc(UserDO::getCreateTime);

        Page<UserDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<UserDetail> records =
                page.getRecords().stream().map(this::toUserDetail).toList();
        return new UserQueryResult(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public UserAuth getUserAuth(String userId) {
        UserDO user = findUser(userId);
        if (user == null) {
            return null;
        }
        return new UserAuth(
                user.getUserType() != null ? user.getUserType().getCode() : null,
                user.getStatus() != null ? user.getStatus().getCode() : null);
    }

    @Override
    public void updateUserStatus(String userId, String statusCode) {
        UserDO user = findUserOrThrow(userId);
        user.setStatus(parseStatus(statusCode));
        userMapper.updateById(user);
    }

    @Override
    public void unlockUser(String userId) {
        UserDO user = findUserOrThrow(userId);
        if (user.getStatus() != UserStatus.LOCKED && user.getStatus() != UserStatus.DISABLED) {
            throw BusinessException.of("该用户未被锁定或禁用");
        }
        user.setStatus(UserStatus.NORMAL);
        userMapper.updateById(user);
    }

    @Override
    public void setUserType(String userId, String typeCode) {
        UserDO user = findUserOrThrow(userId);
        UserType target;
        try {
            target = UserType.fromCode(typeCode);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的用户角色");
        }
        if (target == user.getUserType()) {
            throw BusinessException.of("用户已是该角色");
        }
        if (target == UserType.ADMIN) {
            long adminCount = ChainWrappers.lambdaQueryChain(userMapper)
                    .eq(UserDO::getUserType, UserType.ADMIN)
                    .eq(UserDO::getDelFlag, 0)
                    .count();
            if (adminCount <= 1 && user.getUserType() == UserType.ADMIN) {
                throw BusinessException.of("不能修改最后一个管理员的角色");
            }
        }
        user.setUserType(target);
        userMapper.updateById(user);
    }

    @Override
    public void setPassword(String userId, String encodedPassword) {
        UserDO user = findUserOrThrow(userId);
        user.setPassword(encodedPassword);
        userMapper.updateById(user);
    }

    @Override
    public UserStats getUserStats() {
        long totalUsers = ChainWrappers.lambdaQueryChain(userMapper)
                .eq(UserDO::getDelFlag, 0)
                .count();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNewUsers = ChainWrappers.lambdaQueryChain(userMapper)
                .eq(UserDO::getDelFlag, 0)
                .ge(UserDO::getCreateTime, todayStart)
                .count();

        return new UserStats(totalUsers, todayNewUsers);
    }

    @Override
    public List<RecentUser> getRecentUsers(int limit) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        return ChainWrappers.lambdaQueryChain(userMapper)
                .eq(UserDO::getDelFlag, 0)
                .ge(UserDO::getCreateTime, todayStart)
                .orderByDesc(UserDO::getCreateTime)
                .last("LIMIT " + limit)
                .list()
                .stream()
                .map(this::toRecentUser)
                .toList();
    }

    private UserInfo toUserInfo(UserDO user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getNickName(), user.getAvatar(), user.getPhone());
    }

    private UserDetail toUserDetail(UserDO user) {
        return new UserDetail(
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

    private RecentUser toRecentUser(UserDO user) {
        return new RecentUser(
                user.getId(),
                user.getUsername(),
                user.getNickName(),
                user.getAvatar(),
                user.getEmail(),
                user.getPhone(),
                user.getUserType() != null ? user.getUserType().getCode() : null,
                user.getUserType() != null ? user.getUserType().getDescription() : null,
                user.getStatus() != null ? user.getStatus().getCode() : null,
                user.getStatus() != null ? user.getStatus().getDescription() : null,
                user.getCreateTime());
    }

    private UserDO findUser(String userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null || user.getDelFlag() != 0) {
            return null;
        }
        return user;
    }

    private UserDO findUserOrThrow(String userId) {
        UserDO user = findUser(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
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
}
