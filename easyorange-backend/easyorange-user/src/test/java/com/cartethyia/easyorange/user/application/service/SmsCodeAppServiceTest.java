package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.user.domain.service.SmsCodeDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsCodeAppService 测试")
class SmsCodeAppServiceTest {

    @Mock
    private SmsCodeDomainService smsCodeDomainService;

    private SmsCodeAppService service;

    @BeforeEach
    void setUp() {
        service = new SmsCodeAppService(smsCodeDomainService);
    }

    @Test
    @DisplayName("发送短信验证码成功")
    void sendSmsCode_success() {
        String phone = "13812345678";

        service.sendSmsCode(phone);

        verify(smsCodeDomainService).sendCode(phone);
    }
}
