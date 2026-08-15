package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.user.domain.port.UserQueryPort;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class MessageUserInfoAdapter implements UserInfoPort {

    private final UserQueryPort userQueryPort;

    @Override
    public Map<String, UserInfo> getUserInfoMap(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userQueryPort.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(UserQueryPort.UserInfo::id, this::toUserInfo, (a, _) -> a));
    }

    private UserInfo toUserInfo(UserQueryPort.UserInfo user) {
        return UserInfo.of(user.id(), user.username(), user.avatar());
    }
}
