package com.cartethyia.easyorange.user.application.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import org.springframework.stereotype.Component;

@Component
public class UserAssembler {

    public UserVO toVo(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(MaskUtils.maskEmail(user.getEmail()))
            .phone(MaskUtils.maskPhone(user.getPhone()))
            .studentId(user.getStudentId())
            .realName(MaskUtils.maskName(user.getRealName()))
            .avatar(user.getAvatar())
            .status(user.getStatus() != null
                ? Integer.parseInt(user.getStatus().getCode()) : 0)
            .createTime(user.getCreateTime())
            .updateTime(user.getUpdateTime())
            .build();
    }

    public UserProfileVO toProfileVo(User user, java.util.Set<String> roles, java.util.Set<String> permissions, Long loginTime) {
        if (user == null) {
            return null;
        }
        return UserProfileVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(MaskUtils.maskEmail(user.getEmail()))
            .phone(MaskUtils.maskPhone(user.getPhone()))
            .studentId(user.getStudentId())
            .realName(MaskUtils.maskName(user.getRealName()))
            .status(user.getStatus() != null
                ? Integer.parseInt(user.getStatus().getCode()) : 0)
            .statusDesc(user.getStatus() != null
                ? user.getStatus().getDescription() : null)
            .gender(user.getSex() != null ? Integer.parseInt(user.getSex().getCode()) : null)
            .userType(user.getUserType() != null
                ? user.getUserType().getDescription() : null)
            .avatar(user.getAvatar())
            .createTime(user.getCreateTime())
            .updateTime(user.getUpdateTime())
            .roles(roles)
            .permissions(permissions)
            .loginTime(loginTime)
            .build();
    }

    public LoginResponse toLoginResponse(User user, String accessToken, String refreshToken) {
        return LoginResponse.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .user(toVo(user))
            .build();
    }
}
