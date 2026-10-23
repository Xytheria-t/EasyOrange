package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.message.domain.port.MessageUserProfilePort;
import com.cartethyia.easyorange.message.domain.valueobject.MessageUserProfile;
import com.cartethyia.easyorange.user.domain.port.UserQueryPort;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 消息侧用户档案适配器 — 把 user 模块的四字段投影裁成消息展示需要的三字段（id / username / avatar）。
 * 与同包 {@link OrderUserNamesAdapter} 不是重复实现：两者实现的是不同模块的不同端口，前者要头像、后者只取用户名。
 */
@Primary
@Component
@RequiredArgsConstructor
public class MessageUserProfileAdapter implements MessageUserProfilePort {

    private final UserQueryPort userQueryPort;

    @Override
    public Map<String, MessageUserProfile> getProfiles(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userQueryPort.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(UserQueryPort.UserProfile::id, this::toProfile, (a, _) -> a));
    }

    private MessageUserProfile toProfile(UserQueryPort.UserProfile user) {
        return MessageUserProfile.of(user.id(), user.username(), user.avatar());
    }
}
