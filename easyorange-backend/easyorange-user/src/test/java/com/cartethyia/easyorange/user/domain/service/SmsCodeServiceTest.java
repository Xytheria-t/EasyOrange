package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsRateLimitPort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsCodeService 测试")
class SmsCodeServiceTest {

    @Mock
    private SmsCodePort smsCodePort;

    @Mock
    private SmsRateLimitPort rateLimitPort;

    @Mock
    private SmsSenderPort smsSenderPort;

    private SmsCodeService service;

    private static final String PHONE = "13812345678";
    private static final String CODE = "123456";

    @BeforeEach
    void setUp() {
        service = new SmsCodeService(smsCodePort, rateLimitPort, smsSenderPort);
    }

    @Nested
    @DisplayName("sendCode")
    class SendCodeTests {

        @Test
        @DisplayName("发送验证码成功")
        void success() {
            when(rateLimitPort.isSendLimited(PHONE)).thenReturn(false);
            when(rateLimitPort.incrementDailyCount(PHONE)).thenReturn(1L);

            service.sendCode(PHONE);

            verify(rateLimitPort).isSendLimited(PHONE);
            verify(rateLimitPort).incrementDailyCount(PHONE);
            verify(smsCodePort).save(eq(PHONE), anyString(), eq(Duration.ofMinutes(5)));
            verify(rateLimitPort).setSendInterval(PHONE, Duration.ofSeconds(60));
            verify(smsSenderPort).send(eq(PHONE), anyString());
        }

        @Test
        @DisplayName("发送受限时抛出异常")
        void sendLimited() {
            when(rateLimitPort.isSendLimited(PHONE)).thenReturn(true);

            assertThatThrownBy(() -> service.sendCode(PHONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码发送过于频繁");

            verify(rateLimitPort, never()).incrementDailyCount(any());
            verify(smsCodePort, never()).save(any(), any(), any());
            verify(smsSenderPort, never()).send(any(), any());
        }

        @Test
        @DisplayName("超过每日最大发送次数时抛出异常")
        void exceededDailyLimit() {
            when(rateLimitPort.isSendLimited(PHONE)).thenReturn(false);
            when(rateLimitPort.incrementDailyCount(PHONE)).thenReturn(11L);

            assertThatThrownBy(() -> service.sendCode(PHONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码发送过于频繁");

            verify(smsCodePort, never()).save(any(), any(), any());
            verify(smsSenderPort, never()).send(any(), any());
        }

        @Test
        @DisplayName("生成的验证码长度为6位数字")
        void generatedCodeIsSixDigits() {
            when(rateLimitPort.isSendLimited(PHONE)).thenReturn(false);
            when(rateLimitPort.incrementDailyCount(PHONE)).thenReturn(1L);

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            service.sendCode(PHONE);

            verify(smsCodePort).save(eq(PHONE), codeCaptor.capture(), eq(Duration.ofMinutes(5)));
            verify(smsSenderPort).send(eq(PHONE), codeCaptor.capture());
            String generatedCode = codeCaptor.getValue();
            assertThat(generatedCode).hasSize(6);
            assertThat(generatedCode).containsOnlyDigits();
        }
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCodeTests {

        @Test
        @DisplayName("验证码正确时验证成功")
        void correctCode() {
            when(rateLimitPort.incrementVerifyCount(PHONE)).thenReturn(1L);
            when(smsCodePort.get(PHONE)).thenReturn(CODE);

            service.verifyCode(PHONE, CODE);

            verify(rateLimitPort).incrementVerifyCount(PHONE);
            verify(smsCodePort).get(PHONE);
            verify(smsCodePort).delete(PHONE);
        }

        @Test
        @DisplayName("验证码为空时抛出异常")
        void blankCode() {
            assertThatThrownBy(() -> service.verifyCode(PHONE, ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码无效或已过期");

            verify(rateLimitPort, never()).incrementVerifyCount(any());
            verify(smsCodePort, never()).get(any());
        }

        @Test
        @DisplayName("验证码为null时抛出异常")
        void nullCode() {
            assertThatThrownBy(() -> service.verifyCode(PHONE, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码无效或已过期");

            verify(rateLimitPort, never()).incrementVerifyCount(any());
            verify(smsCodePort, never()).get(any());
        }

        @Test
        @DisplayName("验证码不匹配时抛出异常")
        void wrongCode() {
            when(rateLimitPort.incrementVerifyCount(PHONE)).thenReturn(1L);
            when(smsCodePort.get(PHONE)).thenReturn(CODE);

            assertThatThrownBy(() -> service.verifyCode(PHONE, "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码无效或已过期");

            verify(smsCodePort, never()).delete(any());
        }

        @Test
        @DisplayName("验证码已过期（null）时抛出异常")
        void expiredCode() {
            when(rateLimitPort.incrementVerifyCount(PHONE)).thenReturn(1L);
            when(smsCodePort.get(PHONE)).thenReturn(null);

            assertThatThrownBy(() -> service.verifyCode(PHONE, CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码无效或已过期");

            verify(smsCodePort, never()).delete(any());
        }

        @Test
        @DisplayName("超过最大验证次数时删除验证码并抛出异常")
        void exceededMaxVerifyAttempts() {
            when(rateLimitPort.incrementVerifyCount(PHONE)).thenReturn(6L);

            assertThatThrownBy(() -> service.verifyCode(PHONE, CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码验证次数过多");

            verify(smsCodePort).delete(PHONE);
            verify(smsCodePort, never()).get(any());
        }
    }
}
