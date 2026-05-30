package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.service.TokenRefreshResult;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String password) {
        return registrationService.registerNewUser(username, password).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public User login(LoginCredential credential) {
        return authenticationService.authenticate(credential, RequestUtil.getClientIp());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.invalidateToken(refreshToken);
        }
        SecurityContextUtil.clearContext();
    }

    public TokenRefreshResult refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(String phone, String verifyCode, String newPassword) {
        authenticationService.resetPassword(phone, verifyCode, newPassword);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String oldPassword, String newPassword) {
        User user = getCurrentUserOrThrow();

        BizRequire.ne(oldPassword, newPassword, "新密码不能与旧密码相同");

        BizRequire.requireTrue(
            passwordEncoder.matches(oldPassword, user.getPassword()),
            UserResultCode.PASSWORD_ERROR
        );

        String encodedNewPassword = passwordEncoder.encode(newPassword);
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
