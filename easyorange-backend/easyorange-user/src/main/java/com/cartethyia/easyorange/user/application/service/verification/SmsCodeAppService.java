package com.cartethyia.easyorange.user.application.service.verification;

import com.cartethyia.easyorange.user.domain.service.SmsCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsCodeAppService {

    private final SmsCodeService smsCodeService;

    public void sendSmsCode(String phone) {
        smsCodeService.sendCode(phone);
    }
}