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

    public User registerNewUser(String username, String password) {
        validateUsernameNotExists(username);

        String encodedPassword = passwordEncoder.encode(password);

        return userRepository.save(User.create(username, encodedPassword));
    }

    private void validateUsernameNotExists(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw BusinessException.of(UserResultCode.USERNAME_EXISTS);
        }
    }
}
