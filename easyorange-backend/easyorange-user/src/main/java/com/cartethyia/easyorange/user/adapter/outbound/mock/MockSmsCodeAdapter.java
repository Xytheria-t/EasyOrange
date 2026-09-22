package com.cartethyia.easyorange.user.adapter.outbound.mock;

import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 内存实现的短信验证码适配器（dev/test 环境）。
 * <p>
 * 与 {@code redisSmsCodeAdapter} 按 {@code @Profile} 互斥（dev/test ↔ it/prod，
 * 装配互斥由 {@code SmsCodeAdapterWiringTest} 守护）。
 * 只负责验证码的生成、存储与有效期校验，不复制 {@code redisSmsCodeAdapter} 的
 * 发送间隔/每日配额/验证次数等限流策略（策略归属生产适配器，双份实现会漂移）。
 * 不依赖 Redis，不发送真实短信，应用重启后数据丢失。
 */
@Component
@Profile({"dev", "test"})
public class MockSmsCodeAdapter implements SmsCodePort {

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final SmsSenderPort smsSenderPort;
    private final Clock clock;

    @Autowired
    public MockSmsCodeAdapter(SmsSenderPort smsSenderPort) {
        this(smsSenderPort, Clock.systemUTC());
    }

    /** 测试注入 Clock，免等 5 分钟真实 TTL 才能验证过期路径。 */
    MockSmsCodeAdapter(SmsSenderPort smsSenderPort, Clock clock) {
        this.smsSenderPort = smsSenderPort;
        this.clock = clock;
    }

    @Override
    public boolean send(String phone) {
        String code = SmsCodePort.generateCode();
        // 先投递后落码：发送失败（SmsSenderPort 契约抛异常）时不残留脏验证码
        smsSenderPort.send(phone, code);
        // 顺带清掉过期条目，map 规模以一个 TTL 窗口内的手机号数为上界
        codes.values().removeIf(this::expired);
        codes.put(phone, new CodeEntry(code, clock.instant()));
        return true;
    }

    @Override
    public VerifyResult verify(String phone, String code) {
        return evaluate(phone, code, true);
    }

    @Override
    public VerifyResult check(String phone, String code) {
        return evaluate(phone, code, false);
    }

    private VerifyResult evaluate(String phone, String code, boolean consume) {
        if (phone == null || phone.isBlank() || code == null || code.isBlank()) {
            return VerifyResult.NOT_FOUND;
        }
        CodeEntry entry = codes.get(phone);
        if (entry == null || expired(entry) || !entry.code.equals(code)) {
            return VerifyResult.NOT_FOUND;
        }
        if (consume) {
            // 只消费本次校验命中的那份码：校验期间若已重发新码，不误删新码
            codes.remove(phone, entry);
        }
        return VerifyResult.OK;
    }

    private boolean expired(CodeEntry entry) {
        return clock.instant().isAfter(entry.createdAt.plus(UserSecurityConstant.SMS_CODE_TTL));
    }

    private record CodeEntry(String code, Instant createdAt) {}
}
