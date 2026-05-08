package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;

public class UserRegistrationDomainService {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;

    public UserRegistrationDomainService(
            UserRepository userRepository,
            PasswordDomainService passwordDomainService) {
        this.userRepository = userRepository;
        this.passwordDomainService = passwordDomainService;
    }

    public User register(String username, String password, String phone, String email, String nickname) {
        validateUsernameNotExists(username);
        validateUniqueContactInfo(phone, email);

        String encodedPassword = passwordDomainService.encode(password);
        User user = User.register(username, encodedPassword, nickname);

        if (phone != null && !phone.isBlank() || email != null && !email.isBlank()) {
            user = user.updateProfile(email, phone, null, null, null, null, null);
        }

        return userRepository.save(user);
    }

    private void validateUsernameNotExists(String username) {
        userRepository.findByUsername(username)
            .ifPresent(_ -> { throw BusinessException.of(UserResultCode.USERNAME_EXISTS); });
    }

    private void validateUniqueContactInfo(String phone, String email) {
        if (phone != null && !phone.isBlank()) {
            userRepository.findByPhone(phone)
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.PHONE_EXISTS); });
        }
        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email)
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.EMAIL_EXISTS); });
        }
    }
}
