package com.cartethyia.easyorange.user.domain.port;

/**
 * 短信验证码端口 - 封装验证码的生成、存储、限流、发送和验证。
 */
public interface SmsCodePort {

    /** 发送验证码，限流时返回 false */
    boolean send(String phone);

    /** 验证验证码 */
    VerifyResult verify(String phone, String code);

    enum VerifyResult { OK, NOT_FOUND, TOO_MANY_ATTEMPTS }
}
