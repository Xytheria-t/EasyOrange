package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageUserInfoAdapter implements UserInfoPort {

    private final UserRepository userRepository;

    @Override
    public UserInfo getUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(this::toUserInfo)
                .orElse(null);
    }

    @Override
    public Map<Long, UserInfo> getUserInfoMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::toUserInfo,
                        (a, _) -> a
                ));
    }

    private UserInfo toUserInfo(User user) {
        String avatar = null;
        if (user.getPersonalInfo() != null) {
            avatar = user.getPersonalInfo().avatar();
        }
        return UserInfo.of(user.getId(), user.getUsername(), avatar);
    }
}
