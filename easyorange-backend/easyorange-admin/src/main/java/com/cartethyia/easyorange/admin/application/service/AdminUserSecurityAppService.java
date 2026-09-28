package com.cartethyia.easyorange.admin.application.service;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserAuth;
import com.cartethyia.easyorange.framework.auth.TokenService;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端用户安全操作 — 解锁 / 重置密码 / 强制登出 / 改角色。
 * <p>
 * <b>取舍</b>：三条写路径都收 reason + operatorId 并落库（{@code eo_user.remark} + {@code update_by}），
 * 不靠日志留痕 —— 「谁在什么时候把谁停了」是管理端最常被追问的审计问题。
 * <p>
 * <b>边界</b>：重置密码与改角色都会吊销该用户全部会话，让变更即时生效；
 * 吊销失败不回滚密码变更（凭证已改这一事实先落地，会话作废可由用户重新登录兜底）。
 */
@Service
@RequiredArgsConstructor
public class AdminUserSecurityAppService {

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
    public void unlockUser(String id, String operatorId) {
        adminUserPort.unlockUser(id, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    /** 返回明文新密码 —— 明文只在这一刻存在，响应文案与包装归 web 层。 */
    public String resetPassword(String id, String reason, String operatorId) {
        requireUser(id);
        String newPassword = generateRandomPassword();
        adminUserPort.setPassword(id, passwordEncoder.encode(newPassword), reason, operatorId);
        return newPassword;
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceLogout(String id) {
        requireUser(id);
        tokenService.revokeAllUserSessions(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeUserRole(String id, String role, String reason, String operatorId) {
        adminUserPort.setUserType(id, role, reason, operatorId);
        // 角色即时生效：吊销该用户全部会话，下次登录/刷新按新角色签发
        tokenService.revokeAllUserSessions(id);
    }

    private UserAuth requireUser(String id) {
        UserAuth auth = adminUserPort.getUserAuth(id);
        if (auth == null) {
            throw AdminDomainException.userNotFound(id);
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
