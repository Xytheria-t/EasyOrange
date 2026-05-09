package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordDomainService {

    private static final String PASSWORD_SAME_ERROR = "新密码不能与旧密码相同";

    private final PasswordEncoderPort passwordEncoderPort;

    public String encode(String rawPassword) {
        return passwordEncoderPort.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoderPort.matches(rawPassword, encodedPassword);
    }

    public void validateDifferentPassword(String oldPassword, String newPassword) {
        BizRequire.ne(oldPassword, newPassword, PASSWORD_SAME_ERROR);
    }
}
