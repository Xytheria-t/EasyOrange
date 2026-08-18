package com.cartethyia.easyorange.user.adapter.outbound.mock;

import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 内存实现的短信验证码适配器（开发/测试环境）。
 * <p>
 * 只负责验证码的生成、存储与有效期校验，不复制 {@code redisSmsCodeAdapter} 的
 * 发送间隔/每日配额/验证次数等限流策略（策略归属生产适配器，双份实现会漂移）。
 * 不依赖 Redis，不发送真实短信，应用重启后数据丢失。
 */
@Component
@ConditionalOnMissingBean(name = "redisSmsCodeAdapter")
@RequiredArgsConstructor
public class MockSmsCodeAdapter implements SmsCodePort {

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();

    private final SmsSenderPort smsSenderPort;

    @Override
    public boolean send(String phone) {
        String code = SmsCodePort.generateCode();
        codes.put(phone, new CodeEntry(code, Instant.now()));
        smsSenderPort.send(phone, code);
        return true;
    }

    @Override
    public VerifyResult verify(String phone, String code) {
        if (code == null || code.isBlank()) {
            return VerifyResult.NOT_FOUND;
        }

        CodeEntry entry = codes.get(phone);
        if (entry == null || Instant.now().isAfter(entry.createdAt.plus(UserSecurityConstant.SMS_CODE_TTL))) {
            return VerifyResult.NOT_FOUND;
        }
        if (!entry.code.equals(code)) {
            return VerifyResult.NOT_FOUND;
        }

        codes.remove(phone);
        return VerifyResult.OK;
    }

    private record CodeEntry(String code, Instant createdAt) {}
}
