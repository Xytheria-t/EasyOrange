package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.admin.dto.request.ResetPasswordRequest;
import com.cartethyia.easyorange.admin.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.dto.request.UserUnlockRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminUserVO;
import com.cartethyia.easyorange.admin.dto.response.ResetPasswordVO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceExtension {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public void unlockUser(Long id, UserUnlockRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }

        if (entity.getStatus() != UserStatus.LOCKED && entity.getStatus() != UserStatus.DISABLED) {
            throw BusinessException.of("该用户未被锁定或禁用");
        }

        entity.setStatus(UserStatus.NORMAL);
        userMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordVO resetPassword(Long id, ResetPasswordRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }

        String newPassword = generateRandomPassword(12);
        entity.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(entity);

        return ResetPasswordVO.builder()
            .newPassword(newPassword)
            .message("密码已重置，请将新密码安全地传递给用户")
            .build();
    }

    public void forceLogout(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeUserRole(Long id, UserRoleRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }

        UserType newRole = UserType.fromCode(request.getRole());
        if (newRole == null) {
            throw BusinessException.of("无效的角色值");
        }

        if (newRole == entity.getUserType()) {
            throw BusinessException.of("用户已是该角色");
        }

        if (newRole == UserType.ADMIN) {
            long adminCount = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                    .eq(UserEntity::getUserType, UserType.ADMIN)
                    .eq(UserEntity::getDelFlag, 0)
            );
            if (adminCount <= 1 && entity.getUserType() == UserType.ADMIN) {
                throw BusinessException.of("不能修改最后一个管理员的角色");
            }
        }

        entity.setUserType(newRole);
        userMapper.updateById(entity);
    }

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CHAR_DIGIT = "0123456789";
    private static final String CHAR_SPECIAL = "!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALL_CHARS = CHAR_LOWER + CHAR_UPPER + CHAR_DIGIT + CHAR_SPECIAL;

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        sb.append(CHAR_LOWER.charAt(RANDOM.nextInt(CHAR_LOWER.length())));
        sb.append(CHAR_UPPER.charAt(RANDOM.nextInt(CHAR_UPPER.length())));
        sb.append(CHAR_DIGIT.charAt(RANDOM.nextInt(CHAR_DIGIT.length())));
        sb.append(CHAR_SPECIAL.charAt(RANDOM.nextInt(CHAR_SPECIAL.length())));

        for (int i = 4; i < length; i++) {
            sb.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        List<Character> chars = sb.chars().mapToObj(c -> (char) c).toList();
        java.util.Collections.shuffle(chars, new java.security.SecureRandom());

        StringBuilder result = new StringBuilder();
        for (Character c : chars) {
            result.append(c);
        }
        return result.toString();
    }
}
