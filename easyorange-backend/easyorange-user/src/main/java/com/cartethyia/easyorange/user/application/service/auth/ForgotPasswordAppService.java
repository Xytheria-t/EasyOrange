package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.user.application.command.ForgotPasswordCommand;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordAppService {

    private final AuthenticationService authenticationService;

    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordCommand command) {
        authenticationService.resetPassword(
            command.getPhone(),
            command.getVerifyCode(),
            command.getNewPassword()
        );
        log.info("Password reset processed for phone: {}", maskPhone(command.getPhone()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
