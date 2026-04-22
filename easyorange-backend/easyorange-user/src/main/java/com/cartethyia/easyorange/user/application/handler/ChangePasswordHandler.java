package com.cartethyia.easyorange.user.application.handler;

import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.command.ChangePasswordCommand;
import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
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
public class ChangePasswordHandler {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public void handle(ChangePasswordCommand command) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();

        UserAggregate user = userRepository.findById(new UserId(currentUserId))
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Password oldPassword = user.getPassword();
        Password newPassword = Password.fromRaw(command.getNewPassword());

        if (!passwordEncoder.matches(command.getOldPassword(), oldPassword.getEncodedValue())) {
            throw new RuntimeException("旧密码错误");
        }

        Password encodedNewPassword = newPassword.encode(passwordEncoder::encode);
        PasswordChangedEvent event = user.changePassword(encodedNewPassword);

        userRepository.update(user);
        log.info("action=changePassword success userId={}", currentUserId);
    }
}