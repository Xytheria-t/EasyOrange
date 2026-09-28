package com.cartethyia.easyorange.user.domain.port;

/**
 * 短信发送端口 - 验证码的实际投递。
 * <p>
 * {@link SmsCodePort} 的适配器实现内部调用此端口将验证码投递到用户手机。
 * <p>
 * 职责：
 * <ul>
 *   <li>将验证码发送到指定手机号</li>
 *   <li>不关注验证码的存储和验证（由 {@link SmsCodePort} 负责）</li>
 * </ul>
 * <p>
 * 实现类：仓内仅 {@code MockSmsSenderAdapter} 一个（全 profile 装配，验证码只落日志），
 * 未接真实供应商；接入时新增一个实现并改其 {@code @Profile} 即可。
 */
public interface SmsSenderPort {

    /**
     * 发送验证码。
     * <p>
     * 实现类应记录发送日志，并在失败时抛出异常。
     *
     * @param phone 目标手机号
     * @param code  验证码内容
     */
    void send(String phone, String code);
}
