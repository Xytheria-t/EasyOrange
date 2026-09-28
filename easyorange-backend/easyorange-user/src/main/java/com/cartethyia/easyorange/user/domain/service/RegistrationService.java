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
     * 注册新用户 — 内部先查重再编码，明文密码不离开本方法。
     * <p>
     * 与 {@link #validateRegisterable} 的分工：编排时通常先调后者（不消费验证码的预检），
     * 此处的自查重是兜底，防止绕过预检的调用方跳过唯一性校验。
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
