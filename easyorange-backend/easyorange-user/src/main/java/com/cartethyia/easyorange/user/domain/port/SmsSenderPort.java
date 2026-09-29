package com.cartethyia.easyorange.user.domain.port;

/** 短信发送端口 — 验证码的实际投递，由 {@link SmsCodePort} 的适配器实现调用。 */
public interface SmsSenderPort {

    /** 失败须抛异常而非静默吞掉；仓内仅 {@code MockSmsSenderAdapter}（全 profile 装配，验证码只落日志）。 */
    void send(String phone, String code);
}
