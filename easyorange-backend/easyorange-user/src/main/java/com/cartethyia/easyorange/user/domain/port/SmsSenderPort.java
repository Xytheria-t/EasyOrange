package com.cartethyia.easyorange.user.domain.port;

/**
 * 短信发送端口 - 验证码的实际投递。
 * <p>
 * {@link com.cartethyia.easyorange.user.domain.service.SmsCodeService} 生成验证码后，
 * 通过此端口将验证码投递到用户手机。
 * <p>
 * 职责：
 * <ul>
 *   <li>将验证码发送到指定手机号</li>
 *   <li>不关注验证码的存储和验证（由 {@link SmsCodePort} 负责）</li>
 * </ul>
 * <p>
 * 实现类：
 * <ul>
 *   <li>开发/测试：{@code MockSmsSenderAdapter}（日志输出，不真实发送）</li>
 *   <li>生产：对接第三方 SMS 服务商（阿里云、腾讯云等）</li>
 * </ul>
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
