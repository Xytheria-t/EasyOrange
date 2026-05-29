package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.user.application.command.ForgotPasswordCommand;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForgotPasswordAppService 测试")
class ForgotPasswordAppServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    private ForgotPasswordAppService service;

    @BeforeEach
    void setUp() {
        service = new ForgotPasswordAppService(authenticationService);
    }

    @Test
    @DisplayName("忘记密码成功 — 用户存在时重置密码")
    void forgotPassword_userExists() {
        String phone = "13812345678";
        String verifyCode = "123456";
        String newPassword = "NewPass123";
        ForgotPasswordCommand command = new ForgotPasswordCommand(phone, verifyCode, newPassword);

        User user = User.builder()
            .id(1L)
            .credentials(new Credentials("testuser", "encodedPassword"))
            .build();
        when(authenticationService.resetPassword(phone, verifyCode, newPassword))
            .thenReturn(Optional.of(user));

        service.forgotPassword(command);

        verify(authenticationService).resetPassword(phone, verifyCode, newPassword);
    }

    @Test
    @DisplayName("忘记密码 — 用户不存在时不抛异常")
    void forgotPassword_userNotFound() {
        String phone = "13812345678";
        String verifyCode = "123456";
        String newPassword = "NewPass123";
        ForgotPasswordCommand command = new ForgotPasswordCommand(phone, verifyCode, newPassword);

        when(authenticationService.resetPassword(phone, verifyCode, newPassword))
            .thenReturn(Optional.empty());

        service.forgotPassword(command);

        verify(authenticationService).resetPassword(phone, verifyCode, newPassword);
    }
}