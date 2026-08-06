package com.cartethyia.easyorange.user.domain.constant;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserSecurityConstant {

    // ========== 登录锁定 ==========

    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    // ========== 短信验证码 ==========

    /** 验证码有效期 */
    public static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);
    /** 发送间隔 */
    public static final Duration SMS_SEND_INTERVAL = Duration.ofSeconds(60);
    /** 每日最大发送次数 */
    public static final long SMS_MAX_DAILY = 10;
    /** 最大验证尝试次数 */
    public static final long SMS_MAX_VERIFY_ATTEMPTS = 5;
}
