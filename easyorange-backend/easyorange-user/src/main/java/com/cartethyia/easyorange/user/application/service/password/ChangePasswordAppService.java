package com.cartethyia.easyorange.user.application.service.password;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.command.ChangePasswordCommand;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordAppService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordCommand command) {
        User user = getCurrentUserOrThrow();

        BizRequire.ne(command.getOldPassword(), command.getNewPassword(), "新密码不能与旧密码相同");

        BizRequire.requireTrue(
            passwordEncoder.matches(command.getOldPassword(), user.getPassword()),
            UserResultCode.PASSWORD_ERROR
        );

        String encodedNewPassword = passwordEncoder.encode(command.getNewPassword());
        User updatedUser = user.changePassword(encodedNewPassword, user.getId());
        boolean updated = userRepository.update(updatedUser);

        BizRequire.requireTrue(updated, "修改密码失败，请稍后重试");
    }

    private User getCurrentUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }
}
