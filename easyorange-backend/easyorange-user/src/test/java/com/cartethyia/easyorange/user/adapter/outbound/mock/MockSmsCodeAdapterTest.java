package com.cartethyia.easyorange.user.adapter.outbound.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MockSmsCodeAdapterTest {

    private static final String PHONE = "13800000000";
    private static final Instant BASE = Instant.parse("2026-09-23T00:00:00Z");

    private final AtomicReference<Instant> now = new AtomicReference<>(BASE);
    private final List<String> sentCodes = new ArrayList<>();
    private final SmsSenderPort sender = (phone, code) -> sentCodes.add(code);
    private final MockSmsCodeAdapter adapter = new MockSmsCodeAdapter(sender, mutableClock());

    @Test
    @DisplayName("send：投递 6 位数字验证码并落库，check 命中")
    void sendStoresVerifiableCode() {
        assertThat(adapter.send(PHONE)).isTrue();
        assertThat(sentCodes).hasSize(1);
        assertThat(sentCodes.get(0)).matches("\\d{6}");
        assertThat(adapter.check(PHONE, sentCodes.get(0))).isEqualTo(SmsCodePort.VerifyResult.OK);
    }

    @Test
    @DisplayName("verify：成功即消费，同码二次校验失败")
    void verifyConsumesCode() {
        adapter.send(PHONE);
        String code = sentCodes.get(0);

        assertThat(adapter.verify(PHONE, code)).isEqualTo(SmsCodePort.VerifyResult.OK);
        assertThat(adapter.check(PHONE, code)).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
        assertThat(adapter.verify(PHONE, code)).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
    }

    @Test
    @DisplayName("重发覆盖旧码：旧码失效、新码可用")
    void resendOverwritesOldCode() {
        adapter.send(PHONE);
        String oldCode = sentCodes.get(0);
        adapter.send(PHONE);
        String newCode = sentCodes.get(1);

        assertThat(adapter.check(PHONE, oldCode)).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
        assertThat(adapter.check(PHONE, newCode)).isEqualTo(SmsCodePort.VerifyResult.OK);
    }

    @Test
    @DisplayName("TTL 边界：5 分钟整仍有效，过 1 秒失效")
    void codeExpiresAfterTtl() {
        adapter.send(PHONE);
        String code = sentCodes.get(0);

        now.set(BASE.plus(UserSecurityConstant.SMS_CODE_TTL));
        assertThat(adapter.check(PHONE, code)).isEqualTo(SmsCodePort.VerifyResult.OK);

        now.set(BASE.plus(UserSecurityConstant.SMS_CODE_TTL).plusSeconds(1));
        assertThat(adapter.check(PHONE, code)).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
    }

    @Test
    @DisplayName("过期后重发：清理分支执行，新码可用旧码失效")
    void resendAfterExpiryWorks() {
        adapter.send(PHONE);
        String expiredCode = sentCodes.get(0);
        now.set(BASE.plus(UserSecurityConstant.SMS_CODE_TTL).plusSeconds(1));

        adapter.send(PHONE);

        assertThat(adapter.check(PHONE, expiredCode)).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
        assertThat(adapter.check(PHONE, sentCodes.get(1))).isEqualTo(SmsCodePort.VerifyResult.OK);
    }

    @Test
    @DisplayName("错码不消费：错误尝试后原码仍可校验")
    void wrongCodeDoesNotConsume() {
        adapter.send(PHONE);
        String code = sentCodes.get(0);
        String wrong = "000000".equals(code) ? "999999" : "000000";

        assertThat(adapter.verify(PHONE, wrong)).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
        assertThat(adapter.check(PHONE, code)).isEqualTo(SmsCodePort.VerifyResult.OK);
    }

    @Test
    @DisplayName("null 手机号 / 空白验证码 → NOT_FOUND 不抛异常")
    void nullAndBlankInputsReturnNotFound() {
        adapter.send(PHONE);

        assertThat(adapter.check(null, "123456")).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
        assertThat(adapter.check(PHONE, null)).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
        assertThat(adapter.check(PHONE, "  ")).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
        assertThat(adapter.verify(null, "123456")).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
    }

    @Test
    @DisplayName("发送失败：异常上抛且不残留验证码（先发后存）")
    void failedSendLeavesNoCode() {
        List<String> attempted = new ArrayList<>();
        SmsSenderPort boom = (phone, code) -> {
            attempted.add(code);
            throw new IllegalStateException("provider down");
        };
        MockSmsCodeAdapter failing = new MockSmsCodeAdapter(boom, mutableClock());

        assertThatThrownBy(() -> failing.send(PHONE)).isInstanceOf(IllegalStateException.class);
        assertThat(attempted).hasSize(1);
        assertThat(failing.check(PHONE, attempted.get(0))).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
    }

    @Test
    @DisplayName("demo-code 配置后固定发该码，未配置仍随机（演示免查日志）")
    void configuredDemoCodeIsUsed() {
        MockSmsCodeAdapter fixed = new MockSmsCodeAdapter(sender, mutableClock(), "307519");

        fixed.send(PHONE);

        assertThat(sentCodes.get(sentCodes.size() - 1)).isEqualTo("307519");
        assertThat(fixed.check(PHONE, "307519")).isEqualTo(SmsCodePort.VerifyResult.OK);
        assertThat(fixed.check(PHONE, "000000")).isEqualTo(SmsCodePort.VerifyResult.NOT_FOUND);
    }

    private Clock mutableClock() {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
    }
}
