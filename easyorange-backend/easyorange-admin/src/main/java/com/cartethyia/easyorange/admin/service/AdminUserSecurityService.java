package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
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

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(String id) {
        UserDO entity = findUserByIdOrThrow(id);
        if (entity.getStatus() != UserStatus.LOCKED && entity.getStatus() != UserStatus.DISABLED) {
            throw BusinessException.of("该用户未被锁定或禁用");
        }
        entity.setStatus(UserStatus.NORMAL);
        userMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResponse resetPassword(String id) {
        UserDO entity = findUserByIdOrThrow(id);
        String newPassword = generateRandomPassword();
        entity.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(entity);
        return ResetPasswordResponse.builder()
                .newPassword(newPassword)
                .message("密码已重置，请将新密码安全地传递给用户")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceLogout(String id) {
        findUserByIdOrThrow(id);
        tokenService.revokeAllUserSessions(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeUserRole(String id, UserRoleRequest request) {
        UserDO entity = findUserByIdOrThrow(id);
        UserType newRole = UserType.fromCode(request.getRole());
        if (newRole == entity.getUserType()) {
            throw BusinessException.of("用户已是该角色");
        }
        if (newRole == UserType.ADMIN) {
            long adminCount = ChainWrappers.lambdaQueryChain(userMapper)
                    .eq(UserDO::getUserType, UserType.ADMIN)
                    .eq(UserDO::getDelFlag, 0)
                    .count();
            if (adminCount <= 1 && entity.getUserType() == UserType.ADMIN) {
                throw BusinessException.of("不能修改最后一个管理员的角色");
            }
        }
        entity.setUserType(newRole);
        userMapper.updateById(entity);
    }

    private UserDO findUserByIdOrThrow(String id) {
        UserDO entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }
        return entity;
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
