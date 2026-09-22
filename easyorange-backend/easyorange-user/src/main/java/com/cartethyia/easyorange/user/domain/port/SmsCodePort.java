package com.cartethyia.easyorange.user.domain.port;

import java.security.SecureRandom;

/**
 * 短信验证码端口 — 封装验证码的发送和验证行为。
 * <p>
 * 限流策略等实现参数由各适配器自行决定，不在接口中定义。
 */
public interface SmsCodePort {

    /** 隐式 public static final；SecureRandom 线程安全，两适配器共用。 */
    SecureRandom RANDOM = new SecureRandom();

    /** 发送验证码，限流时返回 false */
    boolean send(String phone);

    /** 验证验证码，成功即消费（删除），此后 verify/check 均不再匹配 */
    VerifyResult verify(String phone, String code);

    /**
     * 校验验证码但不消费 — 供「下一步」预检；计入验证次数防爆破，
     * 成功后验证码保留，最终由 {@link #verify} 消费。
     */
    VerifyResult check(String phone, String code);

    /**
     * 生成 6 位数字验证码。
     * <p>
     * 用 {@link SecureRandom} 而非 ThreadLocalRandom：验证码是安全令牌，
     * ThreadLocalRandom 种子可由输出反推、批量预测后续码。
     */
    static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    enum VerifyResult {
        OK,
        NOT_FOUND,
        TOO_MANY_ATTEMPTS
    }
}
