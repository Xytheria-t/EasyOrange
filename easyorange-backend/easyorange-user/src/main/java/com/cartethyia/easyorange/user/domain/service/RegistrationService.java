package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    /**
     * Registers a new user with the given username and password.
     *
     * @param username the desired username (must be unique)
     * @param password the raw plain-text password (will be encoded before storage)
     * @return the newly created user aggregate
     * @throws BusinessException if the username already exists
     */
    public User registerNewUser(String username, String password) {
        validateUsernameNotExists(username);

        String encodedPassword = passwordEncoder.encode(password);

        return User.create(username, encodedPassword);
    }

    private void validateUsernameNotExists(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw BusinessException.of(UserResultCode.USERNAME_EXISTS);
        }
    }
}
