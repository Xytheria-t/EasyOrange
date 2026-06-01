package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final SmsCodeService smsCodeService;

    /**
     * 验证手机短信码 → 设置新密码。
     * <p>
     * 统一处理所有密码修改场景：
     * <ul>
     *   <li>忘记密码（匿名，phone 由用户输入）</li>
     *   <li>修改密码（已登录，phone 从登录态获取）</li>
     * </ul>
     *
     * @param phone      手机号
     * @param verifyCode 短信验证码
     * @param newPassword 新密码（明文）
     * @return 更新后的用户聚合根
     */
    public User resetPassword(String phone, String verifyCode, String newPassword) {
        smsCodeService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw BusinessException.of(UserResultCode.PASSWORD_SAME_AS_OLD);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        User updated = user.changePassword(encodedPassword, null);
        userRepository.update(updated);
        return updated;
    }
}
