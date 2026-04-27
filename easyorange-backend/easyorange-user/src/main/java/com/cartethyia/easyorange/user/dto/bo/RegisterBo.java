package com.cartethyia.easyorange.user.dto.bo;

import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.enums.UserType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * 用户注册业务对象
 * 职责：封装注册业务逻辑，包括密码加密、用户实体构建
 */
public record RegisterBo(
        String username,
        String password
) {

    /**
     * 构建用户实体
     * 业务规则：
     * 1. 密码加密存储
     * 2. 默认用户类型为 NORMAL
     * 3. 默认状态为 NORMAL
     * 4. 自动填充创建时间
     */
    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .createTime(LocalDateTime.now())
                .build();
    }
}
