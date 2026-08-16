package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.order.domain.port.UserInfoPort;
import com.cartethyia.easyorange.user.domain.port.UserQueryPort;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * {@link UserInfoPort} 适配器 — 委托 user 模块公开信息投影端口批量查询用户名。
 */
@Primary
@Component
@RequiredArgsConstructor
public class OrderUserInfoAdapter implements UserInfoPort {

    private final UserQueryPort userQueryPort;

    @Override
    public Map<String, String> findUsernames(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userQueryPort.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(UserQueryPort.UserInfo::id, UserQueryPort.UserInfo::username, (a, _) -> a));
    }
}
