package com.cartethyia.easyorange.user.adapter.outbound.mock;

import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现的短信验证码适配器（开发/测试环境）。
 * <p>
 * 不依赖 Redis，不发送真实短信。应用重启后数据丢失。
 * 当存在 {@code redisSmsCodeAdapter} Bean 时自动跳过。
 */
@Component
@ConditionalOnMissingBean(name = "redisSmsCodeAdapter")
@RequiredArgsConstructor
public class MockSmsCodeAdapter implements SmsCodePort {

    private static final Logger log = LoggerFactory.getLogger(MockSmsCodeAdapter.class);

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> sendLimits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> dailyCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> verifyCounts = new ConcurrentHashMap<>();

    private final SmsSenderPort smsSenderPort;

    @Override
    public boolean send(String phone) {
        // 发送间隔检查
        Instant blockedUntil = sendLimits.get(phone);
        if (blockedUntil != null && Instant.now().isBefore(blockedUntil)) {
            return false;
        }

        // 每日配额检查
        long daily = dailyCounts.merge(phone, 1L, Long::sum);
        if (daily > MAX_DAILY) {
            return false;
        }

        // 生成并存储验证码
        String code = SmsCodePort.generateCode();
        codes.put(phone, new CodeEntry(code, Instant.now()));
        sendLimits.put(phone, Instant.now().plus(SEND_INTERVAL));

        smsSenderPort.send(phone, code);
        return true;
    }

    @Override
    public VerifyResult verify(String phone, String code) {
        if (code == null || code.isBlank()) {
            return VerifyResult.NOT_FOUND;
        }

        long attempts = verifyCounts.merge(phone, 1L, Long::sum);
        if (attempts > MAX_VERIFY_ATTEMPTS) {
            codes.remove(phone);
            verifyCounts.remove(phone);
            return VerifyResult.TOO_MANY_ATTEMPTS;
        }

        CodeEntry entry = codes.get(phone);
        if (entry == null || Instant.now().isAfter(entry.createdAt.plus(CODE_TTL))) {
            return VerifyResult.NOT_FOUND;
        }
        if (!entry.code.equals(code)) {
            return VerifyResult.NOT_FOUND;
        }

        codes.remove(phone);
        verifyCounts.remove(phone);
        return VerifyResult.OK;
    }

    private record CodeEntry(String code, Instant createdAt) {}
}
