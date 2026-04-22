package com.cartethyia.easyorange.user.application.handler;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.application.command.ForgotPasswordCommand;
import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.Password;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordHandler {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public void handle(ForgotPasswordCommand command) {
        UserAggregate user = userRepository.findByUsername(command.getPhone())
                .orElseThrow(() -> new RuntimeException("该手机号未注册"));

        Password newPassword = Password.fromRaw(command.getNewPassword());
        Password encodedPassword = newPassword.encode(passwordEncoder::encode);

        user.changePassword(encodedPassword);
        userRepository.update(user);

        log.info("action=forgotPassword success phone={}", command.getPhone());
    }
}