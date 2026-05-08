package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserRegistrationDomainService {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;

    public User register(String username, String password, String phone, String email, String nickname) {
        validateUsernameNotExists(username);
        validateUniqueContactInfo(phone, email);

        String encodedPassword = passwordDomainService.encode(password);
        User user = User.register(username, encodedPassword, nickname);

        if (StringUtils.hasText(phone) || StringUtils.hasText(email)) {
            user = user.updateProfile(email, phone, null, null, null, null, null);
        }

        return user;
    }

    private void validateUsernameNotExists(String username) {
        userRepository.findByUsername(username)
            .ifPresent(_ -> { throw BusinessException.of(UserResultCode.USERNAME_EXISTS); });
    }

    private void validateUniqueContactInfo(String phone, String email) {
        if (StringUtils.hasText(phone)) {
            userRepository.findByPhone(phone)
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.PHONE_EXISTS); });
        }
        if (StringUtils.hasText(email)) {
            userRepository.findByEmail(email)
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.EMAIL_EXISTS); });
        }
    }
}
