package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
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
    private final UserEventPort userEventPort;

    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = authenticationService.resetPassword(
            request.phone(),
            request.verifyCode(),
            request.newPassword()
        );

        if (user == null) {
            log.info("Password reset processed for phone: {}", maskPhone(request.phone()));
            return;
        }

        userEventPort.publishForgotPassword(new ForgotPasswordEvent(user.getId()));

    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}