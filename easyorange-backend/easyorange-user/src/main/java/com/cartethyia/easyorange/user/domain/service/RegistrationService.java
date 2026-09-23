package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    /**
     * Registers a new user with the given username, password and phone.
     *
     * @param username the desired username (must be unique)
     * @param password the raw plain-text password (will be encoded before storage)
     * @param phone    the phone number bound at signup (must be unique, used by SMS login / 找回密码)
     * @return the newly created user aggregate
     * @throws BusinessException if the username or phone already exists
     */
    public User registerNewUser(String username, String password, String phone) {
        validateRegisterable(username, phone);

        String encodedPassword = passwordEncoder.encode(password);

        return User.create(username, encodedPassword, phone);
    }

    /** 注册前置查重 — 与消费验证码解耦，重复注册失败不浪费取码。 */
    public void validateRegisterable(String username, String phone) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw BusinessException.of(UserResultCode.USERNAME_EXISTS);
        }
        if (phone != null
                && !phone.isBlank()
                && userRepository.findByPhone(phone).isPresent()) {
            throw BusinessException.of(UserResultCode.PHONE_EXISTS);
        }
    }
}
