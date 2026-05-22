package com.cartethyia.easyorange.user.application.service.verification;

import com.cartethyia.easyorange.user.domain.service.SmsCodeService;
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
    private SmsCodeService smsCodeService;

    private SmsCodeAppService service;

    @BeforeEach
    void setUp() {
        service = new SmsCodeAppService(smsCodeService);
    }

    @Test
    @DisplayName("发送短信验证码成功")
    void sendSmsCode_success() {
        String phone = "13812345678";

        service.sendSmsCode(phone);

        verify(smsCodeService).sendCode(phone);
    }
}