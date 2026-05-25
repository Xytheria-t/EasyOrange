package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.dto.request.ResetPasswordRequest;
import com.cartethyia.easyorange.admin.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.dto.request.UserUnlockRequest;
import com.cartethyia.easyorange.admin.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceExtension {

    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Long id, UserUnlockRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }
        entity.setStatus(UserStatus.NORMAL);
        userMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResponse resetPassword(Long id, ResetPasswordRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }
        String newPassword = "123456";
        entity.setPassword(newPassword);
        userMapper.updateById(entity);
        return new ResetPasswordResponse(newPassword, "密码已重置为默认密码");
    }

    @Transactional(rollbackFor = Exception.class)
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
    }
}
