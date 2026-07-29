package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin 用户查询适配器
 * 实现 AdminUserQueryPort，通过 User Mapper 查询数据并转换为 Admin 模块需要的格式
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
        return users.stream()
            .collect(Collectors.toMap(
                UserDO::getId,
                this::toUserInfo,
                (a, b) -> a
            ));
    }

    private UserInfo toUserInfo(UserDO user) {
        return new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getNickName(),
            user.getAvatar(),
            user.getPhone()
        );
    }
}