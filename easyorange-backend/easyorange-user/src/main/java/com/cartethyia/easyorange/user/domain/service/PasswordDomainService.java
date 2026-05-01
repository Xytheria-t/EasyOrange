package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.util.BizRequire;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordDomainService {

    private static final String PASSWORD_SAME_ERROR = "新密码不能与旧密码相同";

    private final PasswordEncoder passwordEncoder;

    public PasswordDomainService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void validateDifferentPassword(String oldPassword, String newPassword) {
        BizRequire.ne(oldPassword, newPassword, PASSWORD_SAME_ERROR);
    }
}
