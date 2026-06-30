package com.cartethyia.easyorange.adapter.outbound.user;

import com.cartethyia.easyorange.order.domain.port.UserInfoPort;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderUserInfoAdapter implements UserInfoPort {

    private final UserRepository userRepository;

    @Override
    public Optional<UserInfo> getUserInfo(String userId) {
        return userRepository.findById(userId)
                .map(user -> new UserInfo(
                        user.getId(),
                        user.getUsername(),
                        user.getContactInfo() != null ? user.getContactInfo().email() : null
                ));
    }
}
