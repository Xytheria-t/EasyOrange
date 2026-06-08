package com.cartethyia.easyorange.user.domain.port;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码端口 - 封装验证码的生成、存储、限流、发送和验证。
 */
public interface SmsCodePort {

    /** 验证码有效期 */
    Duration CODE_TTL = Duration.ofMinutes(5);
    /** 发送间隔 */
    Duration SEND_INTERVAL = Duration.ofSeconds(60);
    /** 每日最大发送次数 */
    long MAX_DAILY = 10;
    /** 最大验证尝试次数 */
    long MAX_VERIFY_ATTEMPTS = 5;

    /** 发送验证码，限流时返回 false */
    boolean send(String phone);

    /** 验证验证码 */
    VerifyResult verify(String phone, String code);

    /**
     * 生成 6 位数字验证码。
     */
    static String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    enum VerifyResult { OK, NOT_FOUND, TOO_MANY_ATTEMPTS }
}
