package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.user.domain.service.SmsCodeDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsCodeAppService {

    private final SmsCodeDomainService smsCodeDomainService;

    public void sendSmsCode(String phone) {
        smsCodeDomainService.sendCode(phone);
    }
}
