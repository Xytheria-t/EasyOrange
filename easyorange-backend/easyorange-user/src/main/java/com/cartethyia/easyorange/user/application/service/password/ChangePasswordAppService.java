package com.cartethyia.easyorange.user.application.service.password;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ChangePasswordRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordAppService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final UserEventPort userEventPort;

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserOrThrow();

        BizRequire.ne(request.oldPassword(), request.newPassword(), "新密码不能与旧密码相同");

        BizRequire.requireTrue(
            passwordEncoder.matches(request.oldPassword(), user.getPassword()),
            UserResultCode.PASSWORD_ERROR
        );

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        User updatedUser = user.changePassword(encodedNewPassword, user.getId());
        boolean updated = userRepository.update(updatedUser);

        BizRequire.requireTrue(updated, "修改密码失败，请稍后重试");

        userEventPort.publishPasswordChanged(new PasswordChangedEvent(user.getId()));
    }

    private User getCurrentUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }
}