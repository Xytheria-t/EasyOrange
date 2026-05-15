package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.domain.service.AuthenticationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetAppService {

    private final AuthenticationDomainService authenticationDomainService;
    private final UserEventPort userEventPort;

    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = authenticationDomainService.resetPassword(
            request.phone(),
            request.verifyCode(),
            request.newPassword()
        );

        if (user == null) {
            log.info("Password reset processed for phone: {}", maskPhone(request.phone()));
            return;
        }

        userEventPort.publish(new ForgotPasswordEvent(user.getId(), request.phone()));

    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
