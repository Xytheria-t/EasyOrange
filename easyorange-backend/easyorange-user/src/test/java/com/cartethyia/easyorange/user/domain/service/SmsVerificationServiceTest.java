package com.cartethyia.easyorange.user.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsVerificationService 测试")
class SmsVerificationServiceTest {

    @Mock
    private SmsCodePort smsCodePort;

    private SmsVerificationService service;

    private static final String PHONE = "13812345678";
    private static final String CODE = "123456";

    @BeforeEach
    void setUp() {
        service = new SmsVerificationService(smsCodePort);
    }

    @Test
    @DisplayName("验证码正确 — 通过")
    void ok() {
        when(smsCodePort.verify(PHONE, CODE)).thenReturn(SmsCodePort.VerifyResult.OK);

        assertThatCode(() -> service.verifyCodeOrThrow(PHONE, CODE)).doesNotThrowAnyException();
        verify(smsCodePort).verify(PHONE, CODE);
    }

    @Test
    @DisplayName("验证码无效 — 抛 B1008")
    void notFound() {
        when(smsCodePort.verify(PHONE, CODE)).thenReturn(SmsCodePort.VerifyResult.NOT_FOUND);

        assertThatThrownBy(() -> service.verifyCodeOrThrow(PHONE, CODE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(UserResultCode.SMS_CODE_INVALID.getCode());
    }

    @Test
    @DisplayName("验证次数过多 — 抛 B1010")
    void tooManyAttempts() {
        when(smsCodePort.verify(PHONE, CODE)).thenReturn(SmsCodePort.VerifyResult.TOO_MANY_ATTEMPTS);

        assertThatThrownBy(() -> service.verifyCodeOrThrow(PHONE, CODE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(UserResultCode.SMS_CODE_VERIFY_TOO_FREQUENT.getCode());
    }
}
