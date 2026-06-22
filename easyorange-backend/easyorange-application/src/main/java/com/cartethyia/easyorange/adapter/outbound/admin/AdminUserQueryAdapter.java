package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin 用户查询适配器
 * 实现 AdminUserQueryPort，通过 User Mapper 查询数据并转换为 Admin 模块需要的格式
 */
@Component
@RequiredArgsConstructor
public class AdminUserQueryAdapter implements AdminUserQueryPort {

    private final UserMapper userMapper;

    @Override
    public UserInfo getUserInfo(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return toUserInfo(user);
    }

    @Override
    public Map<Long, UserInfo> getUserInfos(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream()
            .collect(Collectors.toMap(
                UserEntity::getId,
                this::toUserInfo,
                (a, b) -> a
            ));
    }

    private UserInfo toUserInfo(UserEntity user) {
        return new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getNickName(),
            user.getAvatar(),
            user.getPhone()
        );
    }
}