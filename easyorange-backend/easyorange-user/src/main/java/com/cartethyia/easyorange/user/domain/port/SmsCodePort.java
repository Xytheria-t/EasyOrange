package com.cartethyia.easyorange.user.domain.port;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码端口 — 封装验证码的发送和验证行为。
 * <p>
 * 限流策略等实现参数由各适配器自行决定，不在接口中定义。
 */
public interface SmsCodePort {

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

    enum VerifyResult {
        OK,
        NOT_FOUND,
        TOO_MANY_ATTEMPTS
    }
}
