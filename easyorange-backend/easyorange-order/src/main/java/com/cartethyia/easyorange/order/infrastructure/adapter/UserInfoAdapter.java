package com.cartethyia.easyorange.order.infrastructure.adapter;

import com.cartethyia.easyorange.order.domain.port.outbound.UserInfoPort;
import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserInfoAdapter implements UserInfoPort {

    private final UserRepository userRepository;

    @Override
    public Optional<UserInfo> getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserInfo(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail()
                ));
    }
}
