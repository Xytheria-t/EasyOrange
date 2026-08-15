package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserAuth;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenService;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserSecurityService {

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CHAR_DIGIT = "0123456789";
    private static final String CHAR_SPECIAL = "!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALL_CHARS = CHAR_LOWER + CHAR_UPPER + CHAR_DIGIT + CHAR_SPECIAL;

    private final AdminUserPort adminUserPort;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(String id) {
        adminUserPort.unlockUser(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResponse resetPassword(String id) {
        UserAuth auth = requireUser(id);
        String newPassword = generateRandomPassword();
        adminUserPort.setPassword(id, passwordEncoder.encode(newPassword));
        return ResetPasswordResponse.builder()
                .newPassword(newPassword)
                .message("密码已重置，请将新密码安全地传递给用户")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceLogout(String id) {
        requireUser(id);
        tokenService.revokeAllUserSessions(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeUserRole(String id, UserRoleRequest request) {
        adminUserPort.setUserType(id, request.getRole());
        // 角色即时生效：吊销该用户全部会话，下次登录/刷新按新角色签发
        tokenService.revokeAllUserSessions(id);
    }

    private UserAuth requireUser(String id) {
        UserAuth auth = adminUserPort.getUserAuth(id);
        if (auth == null) {
            throw BusinessException.of("用户不存在");
        }
        return auth;
    }

    private static char pickRandom(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }

    private static final int PASSWORD_LENGTH = 12;

    private String generateRandomPassword() {
        var sb = new StringBuilder(PASSWORD_LENGTH);
        sb.append(pickRandom(CHAR_LOWER));
        sb.append(pickRandom(CHAR_UPPER));
        sb.append(pickRandom(CHAR_DIGIT));
        sb.append(pickRandom(CHAR_SPECIAL));
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            sb.append(pickRandom(ALL_CHARS));
        }
        // Fisher-Yates shuffle to avoid predictable prefix pattern
        for (int i = PASSWORD_LENGTH - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = sb.charAt(i);
            sb.setCharAt(i, sb.charAt(j));
            sb.setCharAt(j, temp);
        }
        return sb.toString();
    }
}
